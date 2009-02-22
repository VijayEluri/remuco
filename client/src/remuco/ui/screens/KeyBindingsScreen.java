package remuco.ui.screens;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.ui.CMD;
import remuco.ui.IKeyListener;
import remuco.ui.KeyBindings;

/** Screen to configure key bindings. */
public final class KeyBindingsScreen extends List implements CommandListener,
		IKeyListener {

	private static final Command CMD_RESET = new Command("Reset",
			Command.SCREEN, 30);

	/** The current action to bind a key to. */
	private int actionToBind;

	private final Alert alertKeyConflict, alertReset;

	private final Display display;

	private final StringBuffer msgKeyConflict = new StringBuffer(60);

	private final CommandListener parent;

	private final KeyBinderScreen screenKeyBinder;

	private final KeyBindings keyBindings;

	/**
	 * The key selected to set for {@link #actionToBind}. This field has only a
	 * valid value if the selected key is already in use for another action --
	 * it is used for handling this situation by interacting with the user.
	 */
	private int selectedKey;

	/**
	 * @param display
	 * @param parent
	 * @param player
	 */
	public KeyBindingsScreen(final CommandListener parent, final Display display) {

		super("Key Bindings", IMPLICIT);

		this.display = display;
		this.parent = parent;

		keyBindings = KeyBindings.getInstance();

		screenKeyBinder = new KeyBinderScreen(this);

		addCommand(CMD_RESET);
		setCommandListener(this);

		for (int i = 0; i < KeyBindings.actionNames.length; i++)
			append("", null);
		updateList();

		alertKeyConflict = new Alert("Key already in use!");
		alertKeyConflict.setType(AlertType.WARNING);
		alertKeyConflict.setTimeout(Alert.FOREVER);
		alertKeyConflict.addCommand(CMD.NO);
		alertKeyConflict.addCommand(CMD.YES);
		alertKeyConflict.setCommandListener(this);

		alertReset = new Alert("Please confirm:");
		alertReset.setString("Reset to default key bindings?");
		alertReset.setType(AlertType.WARNING);
		alertReset.addCommand(CMD.NO);
		alertReset.addCommand(CMD.YES);
		alertReset.setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		int actionOld;

		if (c == CMD_RESET) {

			display.setCurrent(alertReset);

		} else if (c == CMD.YES && d == alertReset) {

			keyBindings.resetToDefaults();

			updateList();

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertReset) {

			display.setCurrent(this);

		} else if (c == List.SELECT_COMMAND) { // an action to set a key for
			// has been chosen

			actionToBind = getSelectedIndex();

			screenKeyBinder.configure(actionToBind);

			display.setCurrent(screenKeyBinder);

		} else if (c == CMD.YES && d == alertKeyConflict) {

			actionOld = keyBindings.release(selectedKey);
			keyBindings.bindKeyToAction(actionToBind, selectedKey);

			updateList(actionToBind);
			if (actionOld >= 0)
				updateList(actionOld);

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertKeyConflict) {

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);
		}

	}

	public void keyPressed(int key) {

		int actionOld;
		String keyName;

		if (key == 0 || key == keyBindings.getKeyForAction(actionToBind)) {

			display.setCurrent(this);

		} else if (keyBindings.isBound(key)) {

			selectedKey = key;

			actionOld = keyBindings.getActionForKey(key);
			keyName = screenKeyBinder.getKeyName(key);

			msgKeyConflict.delete(0, msgKeyConflict.length());
			msgKeyConflict.append("Key ").append(keyName);
			msgKeyConflict.append(" is already in use for '");
			msgKeyConflict.append(KeyBindings.actionNames[actionOld]).append(
					"'.");
			msgKeyConflict.append("\nDo you want to unset it from '");
			msgKeyConflict.append(KeyBindings.actionNames[actionOld]);
			msgKeyConflict.append("' and use it for '");
			msgKeyConflict.append(KeyBindings.actionNames[actionToBind]).append(
					"' ?");
			alertKeyConflict.setString(msgKeyConflict.toString());

			display.setCurrent(alertKeyConflict);

		} else { // key is valid and free

			keyBindings.bindKeyToAction(actionToBind, key);

			updateList(actionToBind);

			display.setCurrent(this);
		}
	}

	public void keyReleased(int key) {
		// ignore

	}

	/**
	 * Update the key name for all actions in the displayed key bindings list.
	 * 
	 */
	private void updateList() {

		for (int action = 0; action < KeyBindings.actionNames.length; action++) {

			updateList(action);

		}

	}

	/**
	 * Update the key name mapped to the given action in the displayed key
	 * bindings list.
	 * 
	 * @param action
	 */
	private void updateList(int action) {

		int key;
		String keyName;

		key = keyBindings.getKeyForAction(action);
		keyName = (key != 0) ? screenKeyBinder.getKeyName(key) : "";
		set(action, KeyBindings.actionNames[action] + ": " + keyName, null);

	}
}
