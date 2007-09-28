package remuco;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import remuco.util.Keys;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Config is a static class. So it is accessable from every other class.
 * Implementing it as an instance class makes no sense since I have no idea how
 * a Remuco instance could determine which saved configuration belongs to it
 * (which must be decided if more than one instance of Remuco is in use).
 * 
 * @author Christian Buennig
 * 
 */
public final class Config {

	public static final String APP_PROP_THEMES = "Remuco-themes";

	private static final String[] APP_PROP_ALLKEYS = new String[] { APP_PROP_THEMES };

	private static final Hashtable applicationProperties = new Hashtable();

	private static final String DEVICE_SPLITTER = ",";

	private static final Vector devices = new Vector();

	private static final int FIRST_RECORD_ID = 1;

	private static final String KEY_DEVICES = "devs";

	private static boolean loaded = false;

	private static final Hashtable options = new Hashtable();

	private static final String RECORD = "options";

	/**
	 * Get the value of a configuration option.
	 * 
	 * @param name
	 *            the option to get the value of
	 * @return the option's value or <code>null</code> if the option is not
	 *         set
	 */
	public static synchronized String get(String name) {

		return (String) options.get(name);

	}

	/**
	 * Get a property defined in the application's manifest or jad file.
	 * 
	 * @param key
	 *            property name, one of <code>Config.APP_PROP_..</code>
	 * @return the property's value or <code>null</code> if the property is
	 *         not set
	 */
	public static String getApplicationProperty(String key) {

		return (String) applicationProperties.get(key);

	}

	/**
	 * Add adevice to the list of known devices. The new device will be placed
	 * on top of the list.
	 * 
	 * @param addr
	 *            the device address
	 * @param name
	 *            the device name (may be <code>null</code> if not known)
	 */
	public static void knownDevicesAdd(String addr, String name) {

		int pos;

		pos = devices.indexOf(addr);
		while (pos != -1 && pos % 2 != 0) {
			pos = devices.indexOf(addr, pos);
		}

		if (pos == -1) {
			devices.insertElementAt(addr, 0);
			devices.insertElementAt(name, 1);
		} else {
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
			devices.insertElementAt(addr, 0);
			devices.insertElementAt(name, 1);
		}

	}

	/**
	 * Delete a device from the list of known devices.
	 * 
	 * @param addr
	 *            the address of the device to delete
	 */
	public static void knownDevicesDelete(String addr) {

		int pos;

		pos = devices.indexOf(addr);
		while (pos != -1 && pos % 2 != 0) {
			pos = devices.indexOf(addr, pos);
		}

		if (pos != -1) {
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
		}
	}

	/**
	 * Forget all known devices.
	 * 
	 */
	public static void knownDevicesDeleteAll() {

		devices.removeAllElements();

	}

	/**
	 * Get all known devices.
	 * 
	 * @return the devices as a vector containing 2 strings for each device -
	 *         its address (element <code>2*i</code> for device <code>i</code>)
	 *         and its name (element <code>2*i+1</code> for device
	 *         <code>i</code>), the latter one may be <code>null</code>
	 */
	public static Vector knownDevicesGet() {
		return devices;
	}

	/**
	 * Load the configuration. This automatically configures the {@link Keys}
	 * (using {@link Keys#configure(int[])}).
	 * 
	 * @return <code>true</code> if loading was successful, <code>false</code>
	 *         if errors orccured (in this case defaults are used for the
	 *         configurations which could not get set, so the application can
	 *         continue its work as normal)
	 */
	public static synchronized boolean load() {

		RecordStore rs = null;
		ByteArrayInputStream bais;
		DataInputStream dis;
		byte[] ba;
		int nextId;
		String key, val;
		int[] keyConfig = new int[Keys.getConfiguration().length];
		boolean ret = true;

		// open record

		if (loaded)
			return true; // may happen if Remuco gets started more than once

		loaded = true;

		rs = openRecord(RECORD);

		if (rs == null)
			return false;

		try {
			nextId = rs.getNextRecordID();
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CONF] load: error, not open ???");
			return false;
		} catch (RecordStoreException e) {
			Log.ln("[CONF] load: unknown error (" + e.getMessage() + ")");
			closeRecord(rs);
			return false;
		}

		// load keys

		try {
			ba = new byte[rs.getRecordSize(FIRST_RECORD_ID)];
			rs.getRecord(FIRST_RECORD_ID, ba, 0);

			bais = new ByteArrayInputStream(ba);
			dis = new DataInputStream(bais);

		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CONF] load: error, not open ???");
			return false;
		} catch (InvalidRecordIDException e) {
			Log.ln("[CONF] load: record seems to be empty");
			closeRecord(rs);
			return true;
		} catch (RecordStoreException e) {
			Log.ln("[CONF] load: unknown error (" + e.getMessage() + ")");
			closeRecord(rs);
			return false;
		}

		try {
			for (int i = 0; i < keyConfig.length; i++) {
				keyConfig[i] = dis.readInt();
			}
			ret = Keys.configure(keyConfig);
			if (!ret)
				Log.ln("[CONF] load: keys malformed");
		} catch (EOFException e) {
			Log.ln("[CONF] load: keys malformed");
			ret = false;
		} catch (IOException e) {
			Log.ln("[CONF] load: unknown IO error (" + e.getMessage() + ")");
			ret = false;
		}

		// load options

		for (int i = FIRST_RECORD_ID + 1; i < nextId; i++) {

			try {

				ba = new byte[rs.getRecordSize(i)];
				rs.getRecord(i, ba, 0);

				bais = new ByteArrayInputStream(ba);
				dis = new DataInputStream(bais);

			} catch (RecordStoreNotOpenException e) {
				Log.ln("[CONF] load: error, not open ???");
				return false;
			} catch (InvalidRecordIDException e) {
				continue;
			} catch (RecordStoreException e) {
				Log.ln("[CONF] load: unknown error (" + e.getMessage() + ")");
				closeRecord(rs);
				return false;
			}

			try {
				key = dis.readUTF();
				val = dis.readUTF();
				Log.debug("[CONF] load: option " + key + " = '" + val + "'");
			} catch (IOException e) {
				Log.ln("[CONF] load: error, bad strings in record " + i + "("
						+ e.getMessage() + ")");
				ret = false;
				continue;
			}

			options.put(key, val);

		}

		// ok, done

		closeRecord(rs);

		// update device list

		devicesFromOptions();

		Log.ln("[CONF] load: " + (ret ? "success" : "erros"));

		return ret;

	}

	/**
	 * Saves the current configuration. This automatically saves the current
	 * {@link Keys} configuration using {@link Keys#getConfiguration()}.
	 * 
	 * @return <code>true</code> on success, <code>false</code> if errors
	 *         occured
	 */
	public static synchronized boolean save() {

		RecordStore rs;
		ByteArrayOutputStream baos;
		DataOutputStream dos;
		byte[] ba;
		String key, val;
		int[] keyConfig;
		Enumeration keys;
		boolean ret = true;
		int rid;

		// delete old config

		try {
			RecordStore.deleteRecordStore(RECORD);
			Log.ln("[CONF] save: deleted old config");
		} catch (RecordStoreNotFoundException e) {
			Log.ln("[CONF] save: no config yet");
		} catch (RecordStoreException e) {
			Log.ln("[CONF] save: unknown error (" + e.getMessage() + ")");
			return false;
		}

		// open record

		rs = openRecord(RECORD);

		if (rs == null)
			return false;

		devicesIntoOptions();

		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);

		// get current key config

		keyConfig = Keys.getConfiguration();

		// save key config

		try {
			for (int i = 0; i < keyConfig.length; i++) {
				dos.writeInt(keyConfig[i]);
			}
		} catch (IOException e) {
			Log.ln("[CONF] save: unknown IO error (" + e.getMessage() + ")");
		}

		ba = baos.toByteArray();
		baos.reset();

		try {
			rid = rs.addRecord(ba, 0, ba.length);
			if (rid != FIRST_RECORD_ID) {
				Log.ln("[CONF] save: WARNING, keys not in record 1 !!!");
				closeRecord(rs);
				return false;
			}
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CONF] save: error, not open ???");
			return false;
		} catch (RecordStoreFullException e) {
			Log.ln("[CONF] save: error, full");
			closeRecord(rs);
			return false;
		} catch (RecordStoreException e) {
			Log.ln("[CONF] save: unknown error (" + e.getMessage() + ")");
			closeRecord(rs);
			return false;
		}

		// save options

		keys = options.keys();

		while (keys.hasMoreElements()) {

			key = (String) keys.nextElement();
			val = (String) options.get(key);

			try {
				dos.writeUTF(key);
				dos.writeUTF(val);
			} catch (IOException e) {
				Log.ln("[CONF] save: bad string (" + key + " or " + val + ")");
				ret = false;
				continue;
			}

			ba = baos.toByteArray();
			baos.reset();

			try {
				rs.addRecord(ba, 0, ba.length);
			} catch (RecordStoreNotOpenException e) {
				Log.ln("[CONF] save: error, not open ???");
				return false;
			} catch (RecordStoreFullException e) {
				Log.ln("[CONF] save: error, full");
				closeRecord(rs);
				return false;
			} catch (RecordStoreException e) {
				Log.ln("[CONF] save: unknown error (" + e.getMessage() + ")");
				closeRecord(rs);
				return false;
			}

		}

		// ok, done

		closeRecord(rs);

		Log.ln("[CONF] save: " + (ret ? "success" : "erros"));

		return ret;

	}

	/**
	 * Set an configuration option which will be saved later when
	 * {@link #save()} gets called.
	 * 
	 * @param name
	 *            option name (name {@value #KEY_DEVICES} is reserved !)
	 * @param value
	 *            the option's value (use <code>null</code> to unset an
	 *            option)
	 */
	public static void set(String name, String value) {

		if (value == null)
			options.remove(name);

		options.put(name, value);

	}

	/**
	 * Makes all known application properties accessable to other classes via
	 * the method {@link #getApplicationProperty(String)}. Known properties are
	 * <code>Config.APP_PROP_..</code>.
	 * 
	 * @param midlet
	 *            the midlet which has access to the application properties
	 */
	protected static void setApplicationProperties(MIDlet midlet) {

		String val;

		for (int i = 0; i < APP_PROP_ALLKEYS.length; i++) {
			val = midlet.getAppProperty(APP_PROP_ALLKEYS[i]);
			if (val != null)
				applicationProperties.put(APP_PROP_ALLKEYS[i], val);

		}

	}

	private static void closeRecord(RecordStore rs) {

		if (rs == null)
			return;

		try {
			rs.closeRecordStore();
			Log.ln("[CONF] close: ok");
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CONF] close: not open!");
		} catch (RecordStoreException e) {
			Log.ln("[CONF] close: unknown error (" + e.getMessage() + ")");
		}

	}

	private static void devicesFromOptions() {

		String val;
		String[] devs;

		devices.removeAllElements();

		val = (String) options.get(KEY_DEVICES);

		if (val == null)
			return;

		devs = Tools.splitString(val, DEVICE_SPLITTER);

		if (devs.length == 0)
			return;

		if (devs.length % 2 != 0) {
			Log.ln("[CONF] option devs malformed");
			return;
		}

		for (int i = 0; i < devs.length; i++) {
			devices.addElement(devs[i]);
		}

	}

	private static void devicesIntoOptions() {

		StringBuffer val = new StringBuffer(100);

		int len;

		len = devices.size();

		if (len == 0) {
			options.remove(KEY_DEVICES);
			return;
		}

		for (int i = 0; i < len; i++) {
			val.append((String) devices.elementAt(i)).append(DEVICE_SPLITTER);
		}

		if (len > 0)
			val.deleteCharAt(val.length() - 1);

		options.put(KEY_DEVICES, val.toString());

	}

	private static RecordStore openRecord(String name) {

		RecordStore rs = null;

		try {
			rs = RecordStore.openRecordStore(RECORD, true);
			Log.debug("[CONF] open: ok (~" + ((rs.getSize() / 1024) + 1)
					+ "K used, " + rs.getSizeAvailable() / 1024 + "K free)");
			return rs;
		} catch (RecordStoreFullException e) {
			Log.ln("[CONF] open: error, full");
		} catch (RecordStoreNotFoundException e) {
			Log.ln("[CONF] open: error, not found ???");
		} catch (RecordStoreException e) {
			Log.ln("[CONF] open: unknown error (" + e.getMessage() + ")");
		}

		return null;
	}

}
