IGNORE = 0

# =============================================================================
# connection related messages
# =============================================================================

_CONN = 100
#CONN_PLIST = _CONN
CONN_PINFO = _CONN + 10
CONN_CINFO = _CONN + 20
CONN_BYE = _CONN + 90

# =============================================================================
# sync messages
# =============================================================================

_SYNC = 200
SYNC_STATE = _SYNC
SYNC_PROGRESS = _SYNC  + 1
SYNC_ITEM = _SYNC  + 2

# =============================================================================
# control messages
# =============================================================================

_CTRL = 300

CTRL_PLAYPAUSE = _CTRL
CTRL_NEXT = _CTRL + 1
CTRL_PREV = _CTRL + 2
CTRL_SEEK = _CTRL + 3
CTRL_VOLUME = _CTRL + 4
CTRL_REPEAT = _CTRL + 5
CTRL_SHUFFLE = _CTRL + 6
CTRL_FULLSCREEN = _CTRL + 7
CTRL_RATE = _CTRL + 8
CTRL_TAG = _CTRL + 30
CTRL_SHUTDOWN = _CTRL + 90

# =============================================================================
# action messages
# =============================================================================

_ACT = 400
ACT_PLAYLIST = _ACT
ACT_QUEUE = _ACT + 1
ACT_MLIB = _ACT + 2
ACT_FILES = _ACT + 3

# =============================================================================
# request messages
# =============================================================================

_REQ = 500

REQ_ITEM = _REQ
REQ_PLAYLIST = _REQ + 1
REQ_QUEUE = _REQ + 2
REQ_MLIB = _REQ + 3
REQ_FILES = _REQ + 4

# =============================================================================
# internal messages
# =============================================================================

_PRIV = 0x10000000

PRIV_INITIAL_SYNC = _PRIV # used internally in server

# =============================================================================

def _is_in_range(range_start, id):
    return id >= range_start and id < range_start + 100

def is_control(id):
    return _is_in_range(_CTRL, id)

def is_action(id):
    return _is_in_range(_ACT, id)

def is_request(id):
    return _is_in_range(_REQ, id)

def is_private(id):
    return _is_in_range(_PRIV, id)
    