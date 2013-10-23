package com.erogear.android.fos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.erogear.android.bluetooth.comm.BluetoothChatService;
import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.comm.FrameConsumer;
import com.erogear.android.bluetooth.video.FFMPEGVideoProvider;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	public class PreviewList {
		private ArrayList<Preview> items;
		private boolean videoProvidersReady;
		private boolean imagesReady;
		private boolean ready;
		
		public PreviewList() {
			items = new ArrayList<Preview>();
			videoProvidersReady = false;
			imagesReady = false;
			ready = false;
		}
		
		public void setItems(ArrayList<Preview> list) {
			items = list;
		}
		
		public ArrayList<Preview> getItems() {
			return items;
		}
		
	}
	
	// Intent request codes
	public static final int PREFERENCE_INTENT_RESULT = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	
	public static final String TAG = "MAIN";
	public static final String PREVIEWS_DATA_TAG = "previews";
	
	PreviewFragment list;
	PreviewList previews = new PreviewList();
	boolean previewVideoProvidersSet;
	
	private BluetoothVideoService videoSvc;
	private ServiceConnection svcConn;
    private MultiheadController headController;
    
    // false = see data as sent to panel. true = see color data from video file.
    private static final boolean COLOR_PREVIEW = false;
	
	// TODO: Log instead of Toasting.
	private long lastNoDeviceToast = 0;
	/*
	// Load previews on a different thread.
	private class LoadingTask extends AsyncTask<Void, Void, ArrayList<Preview>> {
		
		@Override
		protected ArrayList<Preview> doInBackground(Void... params) {
			return previewList;
		}
		// Start bluetooth service
		// When bluetooth service is started, make VideoSvc available to Previews
		// so that Previews can have their own VideoProviders
		// This MUST wait for VideoSvc
		
		
		@Override
		protected void onPostExecute(ArrayList<Preview> result) {
			Context app = getApplicationContext();
			
			// Start populating previews with frame data.
			// Eventually probably move this stuff to AsyncTask or Runnable 
			for (Preview preview : result) {
				
			}
			
			/*
			Bundle previews = new Bundle();
			previews.putParcelableArrayList(MainActivity.PREVIEWS_DATA_TAG, result);
			list.setArguments(previews);
			getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
			
			Log.d(MainActivity.TAG, String.valueOf(result.size()));
		}
	}*/
	
	/**
	 * Permit construction of a Handler for messages
	 * from BluetoothVideoService
	 */
	private class IncomingMessageCallback implements Handler.Callback {
		@Override
		public boolean handleMessage(Message msg) {
			DeviceConnection conn;

			switch (msg.what) {
			case BluetoothVideoService.MESSAGE_STATE_CHANGE:
				switch(msg.arg1) {
				case BluetoothChatService.STATE_CONNECTION_LOST:
					conn = (DeviceConnection) msg.obj;
					Toast.makeText(MainActivity.this, "Lost connection to " + conn.getDeviceName(), Toast.LENGTH_SHORT).show();
					break;
				case BluetoothChatService.STATE_CONNECTION_FAIL:
					conn = (DeviceConnection) msg.obj;
					Toast.makeText(MainActivity.this, "Could not connect to device " + conn.getDeviceName(), Toast.LENGTH_SHORT).show();
					break;   	
				}
				break;
			case BluetoothVideoService.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				//addConversationLine("Me: " + byteArrayToHex(writeBuf));
				break;
			case BluetoothVideoService.MESSAGE_READ:
				//byte[] readBuf = (byte[]) msg.obj;
				//String readMessage = byteArrayToHex(readBuf);
				addConversationLine(((FrameConsumer)msg.obj).getName() + ":  " + msg.arg1 + " " + msg.arg2);
				break;
			case BluetoothVideoService.MESSAGE_NEW_FRAME:
				/*
	                previewFrame((ByteBufferFrame)msg.obj);

				 */
				break;
			case BluetoothVideoService.MESSAGE_VIDEO_LOADED:
				Toast.makeText(getApplicationContext(), "Video loaded!", Toast.LENGTH_SHORT).show();
				addConversationLine((String) msg.obj);
				break;
			case BluetoothVideoService.MESSAGE_VIDEO_LOAD_FAIL:
				Toast.makeText(getApplicationContext(), "Error while loading video; you may have some valid frames.", Toast.LENGTH_SHORT).show();
				addConversationLine((String) msg.obj);
				break;
			case BluetoothVideoService.MESSAGE_FATAL_ERROR:
				Toast.makeText(getApplicationContext(), "Video service had fatal error.", Toast.LENGTH_SHORT).show();
				finish();
				break;
			case BluetoothVideoService.MESSAGE_NO_DEVICE:
				conn = (DeviceConnection) msg.obj;
				if(lastNoDeviceToast < System.currentTimeMillis() - 2000)
					Toast.makeText(getApplicationContext(), conn.getDeviceName() + " is not connected.", Toast.LENGTH_SHORT).show();

				lastNoDeviceToast = System.currentTimeMillis();
				break;
			}
			return true;
		};
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = new PreviewFragment();
		
		// Initialize previews
		// Previews are not yet ready: each must be assigned a VideoProvider
		previews.setItems(Preview.getAll(MainActivity.this, getResources().getXml(R.xml.previews)));
		
		//new LoadingTask().execute();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
        Log.i(TAG, "--- ONRESUME ---");
        
        startService(new Intent(this, BluetoothVideoService.class));
        svcConn = new ServiceConnection() {
        	@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// Set up notification, the old way.
				// TODO add API check for using the new way
				String appName = getString(R.string.app_name);
				Notification note = new Notification(R.drawable.ic_launcher, appName, System.currentTimeMillis());
				Intent i=new Intent(MainActivity.this, MainActivity.class);

                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, i, 0);

                note.setLatestEventInfo(MainActivity.this, appName, getString(R.string.eg_service_subtitle), pi);
                note.flags|=Notification.FLAG_NO_CLEAR;
                
                // End notification setup
                
                BluetoothVideoService.MyBinder myBinder = (BluetoothVideoService.MyBinder) service;
                videoSvc = myBinder.getService();
                
                Intent btOn = videoSvc.start(note); //If bluetooth is off, this returns an intent that will get it turned on
                videoSvc.addHandler(mHandler); 
                
                if (btOn != null) {
                    startActivityForResult(btOn, REQUEST_ENABLE_BT);
                }
                
                headController = (MultiheadController) videoSvc.getConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY);
                if (headController == null) {
                    headController = new MultiheadController(32, 24);
                    videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, headController);
                    videoSvc.addHandler(headController.getHandler());
                }
                
                try {
                	initializePreviews();
                } catch (Exception e) {
                	Log.e(MainActivity.TAG, "Previews could not be initialized: " + e.getMessage());
                }
			}
        	
        	@Override
			public void onServiceDisconnected(ComponentName name) {
				 videoSvc.removeHandler(mHandler);
			}
        };
			
        bindService(new Intent(MainActivity.this, BluetoothVideoService.class), svcConn, Service.START_STICKY);
        Log.d(MainActivity.TAG, "Bluetooth started");
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
	
    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "--- ONSTOP ---");
        if (svcConn != null) {
        	unbindService(svcConn);
        }
    }
	
    // The Handler that gets information back from the BluetoothVideoService
    private final Handler mHandler = new Handler(new IncomingMessageCallback());
    
    private void addConversationLine(String str) {
    	Log.d("BluetoothServiceConnection", str);
    }
    
    /**
     * Get the right video provider for a given video file type. 
     * @param extension
     * @return
     */
    private VideoProvider getVideoProvider(String extension) {
    	boolean colorPreview = false;
    	int width = 32, height = 8;
    	// the above vars must be made dynamic
    	// see doAfterServiceBound in VideoPlayer.java
    	return new FFMPEGVideoProvider(MainActivity.this, videoSvc, width, height, colorPreview);	
    }
    
    private void initializePreviews() throws Exception {
    	if (previews.getItems().size() == 0) {
    		throw new Exception("No previews were read from XML");
    	}
    	
    	for (Preview p : previews.getItems()) {
    		// Copy the video asset to a file if it doesn't already exist.
			File videoFile = p.saveVideoFileAsset(getApplicationContext());
			
			// Bind a VideoProvider to this preview.
			
			// Preview has a video file, now load it.
			if (videoFile.exists()) {
				Log.d(MainActivity.TAG, "Trying to load video " + p.getFilename());
				FFMPEGVideoProvider ffmpeg = new FFMPEGVideoProvider(MainActivity.this, videoSvc, headController.getVirtualWidth(), headController.getVirtualHeight(), COLOR_PREVIEW);
		        ffmpeg.loadVideo(videoFile);
		        
		        p.setVideoProvider(ffmpeg);
			}
			
			Log.d(MainActivity.TAG, videoFile.getAbsolutePath());
			
			
			// Verify existence of a preview image file 
			// and create it if not present.
			try {
				p.confirmPreviewBitmapReady(getApplicationContext());
			} catch (Exception e) {
				Log.e(MainActivity.TAG, e.getMessage());
			}
    		
    		
    	}

    	Log.d(MainActivity.TAG, "REady to start?");
    }
}