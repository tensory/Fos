package com.erogear.android.fos;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;

public class ControllerPreferenceManager {
	public static final String FILE_KEY = "CONTROLLER";
	public static final String DEVICES_KEY = "PAIRED_DEVICES";
	protected SharedPreferences preferences;

	public ControllerPreferenceManager(Context context) {
		preferences = context.getSharedPreferences(ControllerPreferenceManager.FILE_KEY, Context.MODE_PRIVATE);
	}
	
	public MultiheadController getSavedHeadController() {
		MultiheadController ctrl = new 
		return ctrl;
	}
	
	
	public ArrayList<String> getLastPairedAddresses() {
		Object[] stored = preferences.getStringSet(ControllerPreferenceManager.DEVICES_KEY, Collections.<String>emptySet()).toArray();
		ArrayList<String> deviceAddresses = new ArrayList<String>(stored.length);
		
		for (int i = 0; i < stored.length; i++) {
			deviceAddresses.add((String) stored[i]);
		}
		
		return deviceAddresses;
	}
}
