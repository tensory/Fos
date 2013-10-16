package com.erogear.android.fos;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	PreviewFragment list;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = new PreviewFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
	}
	
	public void onClickPreview(View v) {
		list.onClickPreview(v);
	}
}