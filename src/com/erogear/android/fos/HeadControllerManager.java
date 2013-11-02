package com.erogear.android.fos;

import java.util.Observable;

import android.content.Intent;

import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.TranslateVirtualFrame;

public class HeadControllerManager {
	public String DEVICE_INTENT_KEY;
	private int connectionsExpected, connectionsCompleted;
	private MultiheadController headController;
	
	public HeadControllerManager(final String key) {
		DEVICE_INTENT_KEY = key;
	}
	
	public MultiheadController getNewHeadController(int width, int height) {
		headController = new MultiheadController(width, height);
		return headController;
	}
	
	public MultiheadController getHeadController() {
		return headController;
	}
	
	public void connectDevices(String[] addresses, BluetoothVideoService svc) {
		Intent intent = new Intent();
		connectionsExpected = addresses.length;
		connectionsCompleted = 0;
			
		for (int i = 0; i < addresses.length; i++) {
			String address = addresses[i];
			intent.putExtra(DEVICE_INTENT_KEY, address);
			
			svc.connectDevice(intent, true);
		}
	}
	
	public boolean waiting() {
		return (connectionsExpected < connectionsCompleted);
	}
	
	public boolean ready() {
		return waiting();
	}
	
	public void addHead(DeviceConnection connection) {
		headController.mapHead(connection, new TranslateVirtualFrame(connection.getInputWidth(), connection.getInputHeight(), 0, 0), true);
		connectionsCompleted += 1;
	}
	
	public void finishConnection() {
		connectionsCompleted += 1;
	}
}