package com.erogear.android.fos.fragments;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.erogear.android.fos.NumberPickerDialogPreference;
import com.erogear.android.fos.R;

public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	SharedPreferences sp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Inflate preferences
		addPreferencesFromResource(R.xml.preferences);
		sp = getPreferenceManager().getSharedPreferences();

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

	@Override
	public void onResume() {
		super.onResume();
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		sp.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
		Log.e("PREFERENCE_CHANGED", key);
		Preference pref = findPreference(key);
		pref.setSummary(constructSummaryText(key));
	}
}