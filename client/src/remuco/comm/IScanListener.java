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
package remuco.comm;

/**
 * Interface for classes interested in device scan results.
 * 
 * @see IScanner#startScan(IScanListener)
 * 
 * @author Oben Sonne
 * 
 */
public interface IScanListener {

	/**
	 * Hands out the found devices.
	 * 
	 * @param devices
	 *            the found devices, where element <code>3 * i</code> is the
	 *            address of device <code>i</code>, element
	 *            <code>3 * i + 1</code> its name and element
	 *            <code>3 * i + 2</code> its type
	 */
	public void notifyScannedDevices(String[] devices);

}
