package com.erogear.android.fos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.FrameConsumer;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.MultiheadController.Head;

public class ControllerPreferenceManager {
	public static final String FILE_KEY = "CONTROLLER";
	public static final String DEVICES_KEY = "PAIRED_DEVICES";
	public static final String PREFS_WIDTH = "PANEL_WIDTH";
	public static final String PREFS_HEIGHT = "PANEL_HEIGHT";
	protected String deviceIntentKey;
	protected int panelWidthDefault = 32;
	protected int panelHeightDefault = 24;
	protected SharedPreferences preferences;
	protected boolean panelDimensionsChanged = false;

	/**
	 * Create a ControllerPreferenceManager containing a key 
	 * to tag Bluetooth device addresses at binding time.
	 * See documentation on BluetoothVideoService.connectDevice
	 * 
	 * @param context
	 */
	public ControllerPreferenceManager(Context context) {
		preferences = context.getSharedPreferences(ControllerPreferenceManager.FILE_KEY, Context.MODE_PRIVATE);
	}
	
	protected String[] getLastPairedAddresses() {
		Object[] stored = preferences.getStringSet(ControllerPreferenceManager.DEVICES_KEY, Collections.<String>emptySet()).toArray();
		// Devices are stored in key-value form: DEVICE_NAME,DEVICE_ADDRESS
		// Get only the addresses
		
		String[] deviceAddresses = new String[stored.length];
		
		for (int i = 0; i < stored.length; i++) {
			String[] parts = ((String) stored[i]).split(",");
			deviceAddresses[i] = parts[1];
		}
		
		return deviceAddresses;
	}
	
	protected String[] getLastPairedDeviceNames() {
		Object[] stored = preferences.getStringSet(ControllerPreferenceManager.DEVICES_KEY, Collections.<String>emptySet()).toArray();
		// Devices are stored in key-value form: DEVICE_NAME,DEVICE_ADDRESS
		// Get the device names
		String[] deviceAddresses = new String[stored.length];
		
		for (int i = 0; i < stored.length; i++) {
			String[] parts = ((String) stored[i]).split(",");
			deviceAddresses[i] = parts[0];
		}
		
		return deviceAddresses;
	}
	
	/**
	 * Store Bluetooth device addresses of the heads of a current headController
	 */
	 public void storeDeviceAddresses(Map<FrameConsumer, Head> map, Set<BluetoothDevice> bluetoothDevices) {
		 ArrayList<String> deviceAddresses = new ArrayList<String>();
		 List<BluetoothDevice> bondedDevices = new ArrayList<BluetoothDevice>(bluetoothDevices);
		 
		 for (Map.Entry<FrameConsumer, Head> entry : map.entrySet()) {
			 FrameConsumer device = (FrameConsumer) entry.getKey();
			 String deviceName = device.getName();
			 for (BluetoothDevice d : bondedDevices) {
				 if (deviceName.equals(d.getName())) {
					 String pair = String.format("%1$s,%2$s", d.getName(), d.getAddress());
					 deviceAddresses.add(pair);
				 }
			 }
		 }
		 
		 SharedPreferences.Editor editor = preferences.edit();
		 editor.putStringSet(ControllerPreferenceManager.DEVICES_KEY, new HashSet<String>(deviceAddresses));
		 editor.commit();
	}
	 
	public MultiheadController getHeadController(HeadControllerManager manager, BluetoothVideoService videoService) {
		int width = preferences.getInt(ControllerPreferenceManager.PREFS_WIDTH, panelWidthDefault);
		int height = preferences.getInt(ControllerPreferenceManager.PREFS_HEIGHT, panelHeightDefault);
		
		String[] lastPairedAddresses = getLastPairedAddresses();
		if (lastPairedAddresses.length == 0) {
			return null; // The caller should prompt the user for action if no addresses were stored
		}
		
		MultiheadController headController = manager.getNewHeadController(width, height);
		try {
			manager.connectDevices(lastPairedAddresses, videoService);
		} catch (Exception e) {
			Log.e("CONTROLLER_PREFERENCES", e.getStackTrace().toString());
		}
				
		/* At the time that this controller is returned, 
		 * connections to physical devices have NOT been completed.
		 * headController.getHeads().size == 0
		 * These events are handled in the BluetoothVideoService's Handler.
		 */
		return headController;	
	}
	
	/**
	 * Get the index where a saved device was stored
	 */
	public int getDeviceIndex(String deviceName) {
		int idx = -1;
		String[] knownDevices = getLastPairedDeviceNames();
		for (int i = 0; i < knownDevices.length; i++) {
			if (((String) knownDevices[i]).equals(deviceName)) idx = i;
		}
		return idx;
	}
	
	public void setPanelDimensionsChangedFlag(boolean state) {
		panelDimensionsChanged = state;
	}
	
	public boolean getPanelDimensionsChanged() {
		return panelDimensionsChanged;
	}
	
	public SharedPreferences getPreferences() {
		return preferences;
	}
}
