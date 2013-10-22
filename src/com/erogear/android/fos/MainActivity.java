package com.erogear.android.fos;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	public static final int PREFERENCE_INTENT_RESULT = 1;
	public static final String PREVIEWS_DATA_TAG = "previews";
	PreviewFragment list;
	
	private BluetoothVideoService videoSvc;
	private ServiceConnection svcConn;
	
	/*
	private class LoadingTask extends AsyncTask<ArrayList<Preview>> {
		@Override
		protected void onPostExecute(ArrayList<Preview> result) {
			Bundle previews = new Bundle();
			previews.putParcelableArrayList(MainActivity.PREVIEWS_DATA_TAG, result);
			list.setArguments(previews);
			getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
		}
	}
	*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = new PreviewFragment();
		
		// Initialize previews
		//new LoadingTask.execute();
		
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//Find or create the video service
        startService(new Intent(this, BluetoothVideoService.class));
        svcConn = getBluetoothServiceConnection(); // Async method that sets up Bluetooth service
        // Events depending on service availability will be set in svcConn.onServiceConnected
        
        bindService(new Intent(this, BluetoothVideoService.class), svcConn, Service.START_STICKY);
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
	
	private ServiceConnection getBluetoothServiceConnection() {
		return new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				
			}
		};
	}
}