/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.State;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;
import remuco.util.Log;

/**
 * A simple screeny represents integer values with a specific immutable image
 * for each defined value. It is used to display certain elements of a
 * {@link State}.
 */
public final class SimpleScreeny extends Screeny {

	public static final int TYPE_PLAYBACK = 0;

	public static final int TYPE_REPEAT = 1;

	public static final int TYPE_SHUFFLE = 2;

	private static final int[] IMGIDS_PLAYBACK = new int[] {
			Theme.RTE_STATE_PLAYBACK_STOP, Theme.RTE_STATE_PLAYBACK_PAUSE,
			Theme.RTE_STATE_PLAYBACK_PLAY };
	
	private static final int[] IMGIDS_REPEAT = new int[] {
			Theme.RTE_STATE_REPEAT_OFF, Theme.RTE_STATE_REPEAT_ON };
	
	private static final int[] IMGIDS_SHUFFLE = new int[] {
			Theme.RTE_STATE_SHUFFLE_OFF, Theme.RTE_STATE_SHUFFLE_ON };
	
	private static final int VALUE_REPEAT_OFF = 0;

	private static final int VALUE_REPEAT_ON = 1;

	private static final int VALUE_SHUFFLE_OFF = 0;

	private static final int VALUE_SHUFFLE_ON = 1;

	private static final int[] VALUES_PLAYBACK = new int[] {
			State.PLAYBACK_STOP, State.PLAYBACK_PAUSE, State.PLAYBACK_PLAY };

	private static final int[] VALUES_REPEAT = new int[] { VALUE_REPEAT_OFF,
			VALUE_REPEAT_ON };

	private static final int[] VALUES_SHUFFLE = new int[] { VALUE_SHUFFLE_OFF,
			VALUE_SHUFFLE_ON };

	/**
	 * The images to use to represent a value. The image to use to represent
	 * value {@link #values}<code>[i]</code> is located in
	 * <code>images[i]</code>. Content may change when calling
	 * {@link #initRepresentation()}.
	 */
	private final Image[] images;

	/**
	 * The IDs of the images (as set in {@link Theme}) to use to represent the
	 * several values of this screeny. Gets set once and is dependent of the
	 * screeny's type.
	 */
	private final int[] imgIDs;

	private final int type;

	private int val;

	/**
	 * The values this screeny may represent. Gets set once and is dependent of
	 * the screeny's type.
	 */
	private final int[] values;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            on of {@link #TYPE_REPEAT}, {@link #TYPE_SHUFFLE} or
	 *            {@link #TYPE_PLAYBACK}
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>type</code> has an invalid value
	 */
	public SimpleScreeny(PlayerInfo player, int type) {

		super(player);

		this.type = type;

		switch (type) {

		case TYPE_REPEAT:
			imgIDs = IMGIDS_REPEAT;
			values = VALUES_REPEAT;
			break;

		case TYPE_SHUFFLE:
			imgIDs = IMGIDS_SHUFFLE;
			values = VALUES_SHUFFLE;
			break;

		case TYPE_PLAYBACK:
			imgIDs = IMGIDS_PLAYBACK;
			values = VALUES_PLAYBACK;
			break;

		default:
			throw new IllegalArgumentException();
		}

		images = new Image[imgIDs.length];

	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {

		if (!isInScreeny(px, py)) {
			return;
		}
		
		switch (type) {

		case TYPE_REPEAT:
			actionListener.handleActionPressed(KeyBindings.ACTION_REPEAT);
			break;

		case TYPE_SHUFFLE:
			actionListener.handleActionPressed(KeyBindings.ACTION_SHUFFLE);
			break;

		case TYPE_PLAYBACK:
			actionListener.handleActionPressed(KeyBindings.ACTION_PLAYPAUSE);
			break;

		default:
			Log.bug("Feb 22, 2009.6:27:06 PM");
			break;
		}
	}

	protected void dataUpdated() {

		State s = (State) data;

		if (s == null) {
			val = 0;
			return;
		}

		switch (type) {

		case TYPE_REPEAT:
			val = s.isRepeat() ? VALUE_REPEAT_ON : VALUE_REPEAT_OFF;
			break;

		case TYPE_SHUFFLE:
			val = s.isShuffle() ? VALUE_SHUFFLE_ON : VALUE_SHUFFLE_OFF;
			break;

		case TYPE_PLAYBACK:
			val = s.getPlayback();
			break;

		default:
			Log.bug("Feb 22, 2009.6:27:06 PM");
			break;
		}

	}

	protected void initRepresentation() throws ScreenyException {

		if (type == TYPE_PLAYBACK && !player.supports(Feature.KNOWN_PLAYBACK)) {
			setImage(INVISIBLE);
			return;
		}

		if (type == TYPE_REPEAT && !player.supports(Feature.KNOWN_REPEAT)) {
			setImage(INVISIBLE);
			return;
		}

		if (type == TYPE_SHUFFLE && !player.supports(Feature.KNOWN_SHUFFLE)) {
			setImage(INVISIBLE);
			return;
		}

		for (int i = 0; i < images.length; i++) {
			images[i] = theme.getImg(imgIDs[i]);
		}

		setImage(images[0]);

	}

	protected void updateRepresentation() {

		for (int i = 0; i < values.length; i++) {

			if (val == values[i]) {
				try {
					setImage(images[i]);
				} catch (ScreenyException e) {
					// if the Theme is well formed (i.e. all state images of one
					// type have the same size) this should not happen (already
					// catched in initRepresentation)
				}
				return;
			}
		}
	}

}
