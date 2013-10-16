package com.erogear.android.fos.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.erogear.android.fos.R;

public class PrefsFragment extends PreferenceFragment {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
