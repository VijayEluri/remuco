package remuco.ui.screens;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.List;

import remuco.player.PlobList;
import remuco.ui.Theme;

public final class PloblistScreen extends List {

	public class PloblistSelection {

		/** ID or path. */
		public final String elem;

		public final boolean isPlob;

		private PloblistSelection(String elem, boolean isPlob) {

			this.isPlob = isPlob;

			this.elem = elem;
		}

	}

	private final String fixedTitle;

	private final Vector itemCommands;

	private PlobList list = null;

	public PloblistScreen() {

		this(null);
	}

	public PloblistScreen(String title) {

		super("", List.IMPLICIT);

		fixedTitle = title;

		itemCommands = new Vector();

	}

	/**
	 * Add a command which shall only be visible if there are any items in this
	 * list.
	 */
	public void addItemCommand(Command c) {
		if (!itemCommands.contains(c)) {
			itemCommands.addElement(c);
		}
		if (size() > 0) {
			addCommand(c);
		}
	}

	public PloblistSelection getSelection() {

		if (list == null) {
			return null;
		}

		final int index = getSelectedIndex();
		final int numNested = list.getNumNested();

		if (index < 0) {
			return null;
		} else if (index < numNested) {
			return new PloblistSelection(list.getPathForNested(index),
					false);
		} else if (index < numNested + list.getNumPlobs()) {
			return new PloblistSelection(list
					.getPlobID(index - numNested), true);
		} else {
			return null;
		}
	}

	public void setPloblist(PlobList pl) {

		list = pl;

		deleteAll();

		if (fixedTitle != null) {
			setTitle(fixedTitle);
		} else {
			setTitle(pl.getName());
		}

		final int plobs = list != null ? list.getNumPlobs() : 0;
		final int nested = list != null ? list.getNumNested() : 0;

		// show or hide item dependent commands

		final Enumeration e = itemCommands.elements();

		if (plobs + nested == 0) {

			while (e.hasMoreElements()) {
				super.removeCommand((Command) e.nextElement());
			}

		} else {

			while (e.hasMoreElements()) {
				addCommand((Command) e.nextElement());
			}
		}

		for (int i = 0; i < nested; i++) {
			append(list.getNested(i), Theme.LIST_ICON_PLOBLIST);
		}
		for (int i = 0; i < plobs; i++) {
			append(list.getPlobName(i), Theme.LIST_ICON_PLOB);
		}
	}

	public void setSelectedNested(String name) {

		final int len = list != null ? list.getNumNested() : 0;

		for (int i = 0; i < len; i++) {
			if (list.getNested(i).equals(name)) {
				if (i < size()) {
					setSelectedIndex(i, true);
				}
				break;
			}
		}

	}

	public void setSelectedPlob(int nr) {

		if (list == null) {
			return;
		}
		if (nr >= 0 && nr < list.getNumPlobs()) {
			final int index = list.getNumNested() + nr;
			if (index < size()) {
				setSelectedIndex(index, true);
			}
		}
	}

	public void setSelectedPlob(String id) {

		final int len = list != null ? list.getNumPlobs() : 0;

		for (int i = 0; i < len; i++) {
			if (list.getPlobID(i).equals(id)) {
				final int index = list.getNumNested() + i;
				if (index < size()) {
					setSelectedIndex(i, true);
				}
				break;
			}
		}
	}

}
