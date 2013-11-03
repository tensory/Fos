package com.erogear.android.fos;

import android.content.Intent;
import android.util.Log;

import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.TranslateVirtualFrame;

public class HeadControllerManager {
	public String DEVICE_INTENT_KEY;
	private int connectionsExpected, connectionsCompleted;
	private MultiheadController headController;
	
	// Create a HeadControllerManager that can wait for multiple heads to be attached
	public HeadControllerManager(final String key) {
		DEVICE_INTENT_KEY = key;
		connectionsExpected = 0;
		connectionsCompleted = 0;
	}
	
	public MultiheadController getNewHeadController(int width, int height) {
		headController = new MultiheadController(width, height);
		return headController;
	}
	
	public MultiheadController getHeadController() {
		return headController;
	}
	
	public void connectDevices(String[] addresses, BluetoothVideoService svc) throws Exception {
		Intent intent = new Intent();
		connectionsExpected = addresses.length;
		connectionsCompleted = 0;
			
		for (int i = 0; i < addresses.length; i++) {
			String address = addresses[i];
			intent.putExtra(DEVICE_INTENT_KEY, address);
			try {
				// This may fail if Bluetooth connection has timed out
				svc.connectDevice(intent, true);
			} catch (Exception e) {
				throw new Exception("Bluetooth connection error");
			}
		}
	}
	
	public boolean waiting() {
		return (connectionsCompleted < connectionsExpected);
	}
	
	public boolean ready() {
		return (connectionsCompleted == connectionsExpected);
	}
	
	public void addHead(DeviceConnection connection) {
		headController.mapHead(connection, new TranslateVirtualFrame(connection.getInputWidth(), connection.getInputHeight(), 0, 0), true);
		connectionsCompleted += 1;
	}
	
	public void finishConnection() {
		connectionsCompleted += 1;
	}
	
	/**
	 * Push a frame to the attached device, to show that it's alive
	 * @param head
	 * @param index number to display on the device
	 */
	public void pushStatusFrame(MultiheadController.Head head, int index) {
		if (!head.enabled) {
            head.consumer.sendFrame(DeviceConnection.BLACK_FRAME);
        } else {
            head.consumer.sendFrame(new NumberFrame(index));
        }
	}
}