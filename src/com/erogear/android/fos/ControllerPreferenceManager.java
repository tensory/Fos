package com.erogear.android.fos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.FrameConsumer;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.MultiheadController.Head;

public class ControllerPreferenceManager {
	public static final String FILE_KEY = "CONTROLLER";
	protected static final String DEVICES_KEY = "PAIRED_DEVICES";
	protected static final String PREFS_WIDTH = "PANEL_WIDTH";
	protected static final String PREFS_HEIGHT = "PANEL_HEIGHT";
	protected String deviceIntentKey;
	protected String[] knownDeviceAddresses;
	protected int panelWidthDefault = 32;
	protected int panelHeightDefault = 24;
	protected SharedPreferences preferences;

	/**
	 * Create a ControllerPreferenceManager containing a key 
	 * to tag Bluetooth device addresses at binding time.
	 * See documentation on BluetoothVideoService.connectDevice
	 * 
	 * @param context
	 * @param key String, originating from DeviceListActivity.EXTRA_DEVICE_ADDRESS
	 */
	public ControllerPreferenceManager(Context context, final String key) {
		preferences = context.getSharedPreferences(ControllerPreferenceManager.FILE_KEY, Context.MODE_PRIVATE);
		deviceIntentKey = key;
	}
	
	/**
	 * Reconstruct a MultiheadController using device addresses and dimensions 
	 * from the last instance of a MultiheadController.
	 * 
	 * @param svc 
	 * @return
	 */
	public MultiheadController getSavedHeadController(BluetoothVideoService svc) {
	
		int width = preferences.getInt(ControllerPreferenceManager.PREFS_WIDTH, panelWidthDefault);
		int height = preferences.getInt(ControllerPreferenceManager.PREFS_HEIGHT, panelHeightDefault);
		MultiheadController headController = new MultiheadController(width, height);
        
		// Associate heads with this controller
		String[] addresses = getLastPairedAddresses();
		Intent intent = new Intent();
		for (int i = 0; i < addresses.length; i++) {
			String address = addresses[i];
			intent.putExtra(deviceIntentKey, address);
			
			svc.connectDevice(intent, true);
		}
		
		// At the time that this controller is returned, the connections have NOT finished.
		// These events are handled in the BluetoothVideoService's Handler.
		return headController;
	}
	
	
	protected String[] getLastPairedAddresses() {
		Object[] stored = preferences.getStringSet(ControllerPreferenceManager.DEVICES_KEY, Collections.<String>emptySet()).toArray();
		String[] deviceAddresses = new String[stored.length];
		
		for (int i = 0; i < stored.length; i++) {
			deviceAddresses[i] = (String) stored[i];
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
					 deviceAddresses.add(d.getAddress());
				 }
			 }
		 }
		 
		 SharedPreferences.Editor editor = preferences.edit();
		 editor.putStringSet(ControllerPreferenceManager.DEVICES_KEY, new HashSet<String>(deviceAddresses));
		 editor.commit();
	}
	 
	public void setKnownDevices(String[] deviceAddresses) {
		knownDeviceAddresses = deviceAddresses;
	}
}
