package com.erogear.android.fos.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.erogear.android.fos.NumberPickerDialogPreference;
import com.erogear.android.fos.R;

public class PrefsFragment extends PreferenceFragment {
	SharedPreferences sp;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate preferences
        addPreferencesFromResource(R.xml.preferences);

        sp = getPreferenceScreen().getSharedPreferences();
        String prefkeyFrameRate = getResources().getString(R.string.prefkeyFrameRate);
        NumberPickerDialogPreference frameRate = (NumberPickerDialogPreference) findPreference(prefkeyFrameRate);
        
        // Set summary text
        frameRate.setSummary(constructSummaryText(prefkeyFrameRate));
	}
	
    
    private String constructSummaryText(String key) {
    	String summary = "";
    	if (key.equals(getResources().getString(R.string.prefkeyFrameRate))) {
    		// Preference is stored as a list index
    		// Retrieve the list to show the user-readable value
    		String[] values = getActivity().getResources().getStringArray(R.array.valuesFrameRate);
    		int valueIndex = sp.getInt(key, 0);
    		summary = String.valueOf(values[valueIndex]) + " " + getResources().getString(R.string.txtFramesPerSecond);
    	}
    	return summary;    	
    }
}