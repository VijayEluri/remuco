"""Rhythmbox player adapter for Remuco, implemented as a Rhythmbox plugin.

__author__ = "Oben Sonne <obensonne@googlemail.com>"
__copyright__ = "Copyright 2009, Oben Sonne"
__license__ = "GPL"
__version__ = "0.8.0"

"""
import os.path
import traceback
import time

import gobject

import rb, rhythmdb

import remuco
from remuco import log

# =============================================================================
# plugin
# =============================================================================

class RemucoPlugin(rb.Plugin):
    
    def __init__(self):
        
        rb.Plugin.__init__(self)
        
        self.__rba = None
        
    def activate(self, shell):
        
        if self.__rba is not None:
            return
        
        print("create RhythmboxAdapter")
        self.__rba = RhythmboxAdapter()
        print("RhythmboxAdapter created")

        print("start RhythmboxAdapter")
        self.__rba.start(shell)
        print("RhythmboxAdapter started")
        
    def deactivate(self, shell):
    
        if self.__rba is None:
            return
        
        print("stop RhythmboxAdapter")
        self.__rba.stop()
        print("RhythmboxAdapter stopped")
        
        self.__rba = None
        
# =============================================================================
# constants
# =============================================================================

PLAYORDER_SHUFFLE = "shuffle"
PLAYORDER_SHUFFLE_ALT = "random" # starts with ..
PLAYORDER_REPEAT = "linear-loop"
PLAYORDER_NORMAL = "linear"

SECTION_LIBRARY = "Library"

COVER_FILE_NAMES = ("folder", "front", "album", "cover")
COVER_FILE_TYPES = ("png", "jpeg", "jpg")

# =============================================================================
# actions
# =============================================================================

IA_JUMP = remuco.ItemAction("Jump to")
IA_REMOVE = remuco.ItemAction("Remove", multiple=True)
LA_PLAY = remuco.ListAction("Play")
IA_ENQUEUE = remuco.ItemAction("Enqueue", multiple=True)

PLAYLIST_ACTIONS = (IA_JUMP, IA_ENQUEUE)
QUEUE_ACTIONS = (IA_JUMP, IA_REMOVE)
MLIB_LIST_ACTIONS = (LA_PLAY,)
MLIB_ITEM_ACTIONS = (IA_ENQUEUE,)

#TODO implement added actions

# =============================================================================
# player adapter
# =============================================================================

class RhythmboxAdapter(remuco.PlayerAdapter):

    def __init__(self):
        
        self.__shell = None
        
        remuco.PlayerAdapter.__init__(self, "Rhythmbox",
                                      max_rating=5,
                                      playback_known=True,
                                      volume_known=True,
                                      repeat_known=True,
                                      shuffle_known=True,
                                      progress_known=True)
        
        self.__item_id = None
        self.__item_entry = None
        self.__playlist_sc = None
        self.__queue_sc = None
        
        self.__signal_ids = ()
        
        log.debug("init done")

    def start(self, shell):
        
        if self.__shell is not None:
            log.warning("already started")
            return
        
        remuco.PlayerAdapter.start(self)
        
        self.__shell = shell
        
        sp = self.__shell.get_player()
        
        # shortcuts to RB data 
        
        self.__item_id = None
        self.__item_entry = None
        self.__playlist_sc = sp.get_playing_source()
        self.__queue_sc = self.__shell.props.queue_source
        
        # connect to shell player signals

        self.__signal_ids = (
            sp.connect("playing_changed", self.__notify_playing_changed),
            sp.connect("playing_uri_changed", self.__notify_playing_uri_changed),
            sp.connect("playing-source-changed", self.__notify_source_changed)
        )

        # state sync will happen by timeout
        # trigger item sync:
        self.__notify_playing_uri_changed(sp, sp.get_playing_path()) # item sync
        
        log.debug("start done")

    def stop(self):
        
        remuco.PlayerAdapter.stop(self)

        if self.__shell is None:
            return

        # disconnect from shell player signals

        sp = self.__shell.get_player()

        for sid in self.__signal_ids:
            sp.disconnect(sid)
            
        self.__signal_ids = ()

        # release shell
        
        self.__shell = None
        
        log.debug("stop done")
        
    def poll(self):
        
        sp = self.__shell.get_player()
        
        # check repeat and shuffle
        
        order = sp.props.play_order
        
        repeat = order == PLAYORDER_REPEAT or \
                 order.startswith(PLAYORDER_SHUFFLE_ALT)
        self.update_repeat(repeat)
        
        shuffle = order == PLAYORDER_SHUFFLE or \
                  order.startswith(PLAYORDER_SHUFFLE_ALT)
        self.update_shuffle(shuffle)
        
        # check volume

        volume = int(sp.get_volume() * 100)
        self.update_volume(volume)
        
        # check progress
        
        try:
            progress = sp.get_playing_time()
            length = sp.get_playing_song_duration()
        except gobject.GError:
            progress = 0
            length = 0 
        else:
            self.update_progress(progress, length)
        
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_next(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.do_next()
        except gobject.GError, e:
            log.debug("do next failed: %s" % str(e))
    
    def ctrl_previous(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.set_playing_time(0)
            time.sleep(0.1)
            sp.do_previous()
        except gobject.GError, e:
            log.debug("do previous failed: %s" % str(e))
    
    def ctrl_rate(self, rating):
        
        if self.__item_entry is not None:
            db = self.__shell.props.db
            try:
                db.set(self.__item_entry, rhythmdb.PROP_RATING, rating)
            except gobject.GError, e:
                log.debug("rating failed: %s" % str(e))
    
    def ctrl_toggle_playing(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.playpause()
        except gobject.GError, e:
            log.debug("toggle play pause failed: %s" % str(e))
                
    # shuffle and repeat cannot be set:
    # http://mail.gnome.org/archives/rhythmbox-devel/2008-April/msg00078.html
    
    def ctrl_seek(self, direction):
        
        sp = self.__shell.get_player()

        try:
            sp.seek(direction * 5)
        except gobject.GError, e:
            log.debug("seek failed: %s" % str(e))
        else:
            # update volume within a short time (don't wait for scheduled poll)
            gobject.idle_add(self.poll)    
    
    def ctrl_volume(self, direction):
        
        sp = self.__shell.get_player()
        
        if direction == 0:
            sp.set_volume(0)
        else:
            try:
                sp.set_volume_relative(direction * 0.05)
            except gobject.GError, e:
                log.debug("set volume failed: %s" % str(e))
        
        # update volume within a short time (don't wait for scheduled poll)
        gobject.idle_add(self.poll)
        
    # =========================================================================
    # action interface
    # =========================================================================
    
    def action_playlist_item(self, action_id, positions, ids):

        if action_id == IA_JUMP.id:
            
            try:
                self.__jump_in_plq(self.__playlist_sc, positions[0])
            except gobject.GError, e:
                log.debug("playlist jump failed: %s" % e)
        
        elif action_id == IA_ENQUEUE.id:
            
            self.__enqueue_items(ids)
        
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    def action_queue_item(self, action_id, positions, ids):

        if action_id == IA_JUMP.id:
            
            try:
                self.__jump_in_plq(self.__queue_sc, positions[0])
            except gobject.GError, e:
                log.debug("queue jump failed: %s" % e)
    
        elif action_id == IA_REMOVE.id:
            
            for id in ids:
                self.__shell.remove_from_queue(id)
    
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    def action_mlib_item(self, action_id, path, positions, ids):
        
        if action_id == IA_ENQUEUE.id:
            
            self.__enqueue_items(ids)
        
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    def action_mlib_list(self, action_id, path):
        
        if action_id == LA_PLAY.id:
            
            sc = self.__mlib_path_to_source(path)
            if sc is None:
                log.warning("no source for path %s" % path)
                return
            
            qm = sc.get_entry_view().props.model
            
            sp = self.__shell.get_player()
    
            if sc != self.__playlist_sc:
                try:
                    sp.set_selected_source(sc)
                    sp.set_playing_source(sc)
                    self.__jump_in_plq(sc, 0)
                except gobject.GError, e:
                    log.debug("switching source failed: %s" % str(e))
            
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    
    
    # =========================================================================
    # request interface
    # =========================================================================
    
    def request_playlist(self, client):
        
        try:
            qm = self.__playlist_sc.get_entry_view().props.model 
            ids, names = self.__get_items_from_qmodel(qm)
        except gobject.GError, e:
            log.warning("failed to get playlist items: %s" % e)
            ids, names = [], []
        
        self.reply_playlist_request(client, ids, names,
                                    item_actions=PLAYLIST_ACTIONS)

    def request_queue(self, client):
        
        sc = self.__queue_sc
        qm = sc.props.query_model

        try:
            ids, names = self.__get_items_from_qmodel(qm)
        except gobject.GError, e:
            log.warning("failed to get queue items: %s" % e)
            ids, names = [], []
        
        self.reply_queue_request(client, ids, names,
                                 item_actions=QUEUE_ACTIONS)

    def request_mlib(self, client, path):

        nested = []
        ids = []
        names = []
        
        slm = self.__shell.props.sourcelist_model
        
        ### root ? ###
        
        if not path:
            for group in slm:
                group_name = group[2]
                nested.append(group_name)
            self.reply_mlib_request(client, path, nested, ids, names)
            return
        
        ### group ? ### Library, Playlists

        if len(path) == 1:
            for group in slm:
                group_name = group[2]
                if path[0] == group_name:
                    for sc in group.iterchildren():
                        source_name = sc[2]
                        log.debug("append %s" % source_name)
                        nested.append(source_name)
                    break
            self.reply_mlib_request(client, path, nested, ids, names,
                                        list_actions=MLIB_LIST_ACTIONS)
            return
            
        ### regular playlist (source) ! ### Library/???, Playlists/???
        
        sc = self.__mlib_path_to_source(path)

        if sc is None:
            
            self.reply_mlib_request(client, path, nested, ids, names)
            return
        
        qm = sc.get_entry_view().props.model
            
        try:
            ids, names = self.__get_items_from_qmodel(qm)
        except gobject.GError, e:
            log.warning("failed to list items: %s" % e)
            ids, names = [], []
        
        self.reply_mlib_request(client, path, nested, ids, names,
                                item_actions=MLIB_ITEM_ACTIONS)
        
    # ==========================================================================
    # callbacks
    # ==========================================================================
    
    def __notify_playing_uri_changed(self, sp, uri):
        """Shell player signal callback to handle an item change."""
        
        log.debug("playing uri changed: %s" % uri)
        
        db = self.__shell.props.db

        entry = sp.get_playing_entry()
        if entry is None:
            id = None
        else:
            id = db.entry_get(entry, rhythmdb.PROP_LOCATION)
        
        self.__item_id = id
        self.__item_entry = entry
        
        if entry is not None and id is not None:

            info = self.__get_item_info(entry)
    
            img_data = db.entry_request_extra_metadata(entry, "rb:coverArt")
            if img_data is None:
                img_file = self.find_image(id)
            else:
                try:
                    img_file = "%s/art.png" % self.config.cache_dir
                    img_data.save(img_file, "png")
                except IOError, e:
                    log.warning("failed to save cover art (%s)" % e)
                    img_file = None
    
        else:
            id = None
            img_file = None
            info = None

        self.update_item(id, info, img_file)
        
        # a new item may result in a new position:
        pfq = self.__shell.get_player().props.playing_from_queue
        self.update_position(self.__get_position(), queue=pfq)

    def __notify_playing_changed(self, sp, b):
        """Shell player signal callback to handle a change in playback."""
        
        log.debug("playing changed: %s" % str(b))
        
        if b:
            self.update_playback(remuco.PLAYBACK_PLAY)
        else:
            self.update_playback(remuco.PLAYBACK_PAUSE)

    def __notify_source_changed(self, sp, source_new):
        """Shell player signal callback to handle a playlist switch."""
        
        log.debug("source changed: %s" % str(source_new))
        
        self.__playlist_sc = source_new
        
    # =========================================================================
    # helper methods
    # =========================================================================

    def __jump_in_plq(self, sc, position):
        """Do a jump within the playlist or queue.
        
        @param sc:
            either current playlist or queue source
        @param position:
            position to jump to
            
        """

        if sc is None:
            return
        
        qm = sc.get_entry_view().props.model
        
        id_to_remove_from_queue = None
        
        sp = self.__shell.get_player()

        if sp.props.playing_from_queue:
            id_to_remove_from_queue = self.__item_id

        found = False
        i = 0
        for row in qm:
            if i == position:
                sp.set_selected_source(sc)
                sp.set_playing_source(sc)
                sp.play_entry(row[0])
                found = True
                break
            i += 1
        
        if not found:
            sp.do_next()
        
        if id_to_remove_from_queue != None:
            log.debug("remove %s from queue" % id_to_remove_from_queue)
            self.__shell.remove_from_queue(id_to_remove_from_queue)

    def __get_items_from_qmodel(self, qmodel):
        """Get all items in a query model.
        
        @return: 2 lists - IDs and names of the items
        """
        
        db = self.__shell.props.db

        ids = []
        names = []

        if qmodel is None:
            return (ids, names)

        for row in qmodel:
            uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
            ids.append(uri)
            artist = db.entry_get(row[0], rhythmdb.PROP_ARTIST)
            title = db.entry_get(row[0], rhythmdb.PROP_TITLE)
            if artist and title:
                names.append("%s - %s" % (artist, title))
            elif title:
                names.append(title)
            elif artist:
                names.append(artist)
            else:
                names.append("Unknown")

        return (ids, names)

    def __get_item_info(self, entry):
        """Get meta information for an item.
        
        @return: meta information (dictionary) - also if entry is None (in this
                 case dummy information is returned)
        """
        
        if entry is None:
            return { remuco.INFO_TITLE : "No information" }
        
        db = self.__shell.props.db
        
        meta = {
            remuco.INFO_TITLE : str(db.entry_get(entry, rhythmdb.PROP_TITLE)),
            remuco.INFO_ARTIST: str(db.entry_get(entry, rhythmdb.PROP_ARTIST)),
            remuco.INFO_ALBUM : str(db.entry_get(entry, rhythmdb.PROP_ALBUM)),
            remuco.INFO_GENRE : str(db.entry_get(entry, rhythmdb.PROP_GENRE)),
            remuco.INFO_BITRATE : str(db.entry_get(entry, rhythmdb.PROP_BITRATE)),
            remuco.INFO_LENGTH : str(db.entry_get(entry, rhythmdb.PROP_DURATION)),
            remuco.INFO_RATING : str(int(db.entry_get(entry, rhythmdb.PROP_RATING))),
            remuco.INFO_TRACK : str(db.entry_get(entry, rhythmdb.PROP_TRACK_NUMBER)),
            remuco.INFO_YEAR : str(db.entry_get(entry, rhythmdb.PROP_YEAR))
        }

        return meta 
    
    def __mlib_path_to_source(self, path):
        """Get the source object related to a library path.
        
        @param path: must contain the source' group and name (2 element list)
        """
        
        if len(path) != 2:
            log.error("** BUG ** invalid path length: %s" % path)
            return None
        
        group_name, source_name = path
        
        if group_name is None or source_name is None:
            return None
        
        slm = self.__shell.props.sourcelist_model
        
        for group in slm:
            if group_name == group[2]:
                for source in group.iterchildren():
                    if source_name == source[2]:
                        return source[3]

    def __enqueue_items(self, ids):
        
        for id in ids:
            self.__shell.add_to_queue(id)
            
    def __get_position(self):

        sp = self.__shell.get_player()

        db = self.__shell.props.db

        position = 0
        queue = False
        
        id_now = self.__item_id
        
        if id_now is not None:
            
            if sp.props.playing_from_queue:
                queue = True
                qmodel = self.__queue_sc.props.query_model
            else:
                qmodel = self.__playlist_sc.get_entry_view().props.model
                
            if qmodel is not None:
                for row in qmodel:
                    id = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                    if id_now == id:
                        break
                    position += 1
                    
        log.debug ("position: %i" % position)
        
        return position

