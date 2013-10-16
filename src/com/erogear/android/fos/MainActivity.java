package com.erogear.android.fos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	public static int PREFERENCE_INTENT_RESULT = 1;
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.main, menu);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_settings:
				Intent i = new Intent(getBaseContext(), PrefsActivity.class);
				startActivityForResult(i, MainActivity.PREFERENCE_INTENT_RESULT);
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}