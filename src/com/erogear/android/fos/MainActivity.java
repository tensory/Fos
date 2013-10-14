package com.erogear.android.fos;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreviewFragment list = new PreviewFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
	}
}