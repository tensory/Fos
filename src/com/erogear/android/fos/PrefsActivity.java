package com.erogear.android.fos;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.erogear.android.fos.fragments.PrefsFragment;

public class PrefsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
	}
}
