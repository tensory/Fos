package com.erogear.android.fos;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.erogear.android.bluetooth.comm.BluetoothChatService;
import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.comm.FrameConsumer;
import com.erogear.android.bluetooth.comm.MultiheadSetupActivity;
import com.erogear.android.bluetooth.video.FFMPEGVideoProvider;
import com.erogear.android.bluetooth.video.FrameController;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	// Intent request codes
	public static final int PREFERENCE_INTENT_RESULT = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int MULTIHEAD_SETUP_RESULT = 3;
	
	// Preferences
	private SharedPreferences controllerPrefs;
	private int frameRate;
	
	// Tags
	public static final String TAG = "MAIN";
	public static final String PREVIEWS_DATA_TAG = "previews";
	
	// Video dimensions
	// These are persisted as preferences, but not set through common app settings.
	private static final String PREFS_WIDTH = "panelWidth";
	private static final String PREFS_HEIGHT = "panelHeight";
	private int panelWidth = 32;
	private int panelHeight = 24;
	private boolean panelDimensionsChanged = false;
	
	
	// Preview video queue
	Queue<Preview> q = new LinkedList<Preview>();
	PreviewLoadStateManager qManager = PreviewLoadStateManager.getInstance();
	PreviewLoader activePreview = new PreviewLoader();
	
	// Video controller pool
	private SparseArray<VideoProvider> previewVideoProviderCache;
	
	// Previews sent to the display fragment
	PreviewFragment list;
	ArrayList<Preview> previews = new ArrayList<Preview>();
	// Boolean flag permitting the list fragment to be loaded.
	boolean mainActivityRunning;
	
	private BluetoothVideoService videoSvc;
	private ServiceConnection svcConn;
	private MultiheadController headController;
	private FrameController<VideoProvider, MultiheadController> controller;

    // false = see data as sent to panel. true = see color data from video file.
    private static final boolean COLOR_PREVIEW = false;
	
	// TODO: Log instead of Toasting.
	private long lastNoDeviceToast = 0;
	
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
				// Get the current frame and video provider
				if (activePreview == null) {
					return false;
				}
				
				if (activePreview.hasListIndex() && controller != null) {
					try {
						int previewKey = activePreview.getPreview().hashCode();
						list.drawFrameInCurrentPreview(
								previewVideoProviderCache.get(previewKey).getFrame(controller.getCurrentFrame()));
					} catch (Exception e) {
						Log.e(MainActivity.TAG, "No frame data available for index " + String.valueOf(controller.getCurrentFrame()));
					}
				}

				break;
			case BluetoothVideoService.MESSAGE_READ:
				//byte[] readBuf = (byte[]) msg.obj;
				//String readMessage = byteArrayToHex(readBuf);
				addConversationLine(((FrameConsumer)msg.obj).getName() + ":  " + msg.arg1 + " " + msg.arg2);
				break;
			case BluetoothVideoService.MESSAGE_NEW_FRAME:
				// New frame loaded
				break;
			case BluetoothVideoService.MESSAGE_VIDEO_LOADED:
				Toast.makeText(getApplicationContext(), "Video loaded!", Toast.LENGTH_SHORT).show();
				addConversationLine((String) msg.obj);
				
				// Use the just-loaded video to extract a thumbnail
				previewVideoProviderCache.put(activePreview.getPreview().hashCode(), activePreview.getVideoProvider());
				activePreview.getPreview().confirmPreviewBitmapReady(MainActivity.this, getApplicationContext().getFilesDir());
				// Advance the queue
				q.remove();
				if (q.size() > 0) {
					activePreview.attachPreview(q.peek());
					loadNextPreviewVideo();
				} else {
					// Finished loading preview videos
					activePreview = null;
					panelDimensionsChanged = false;
					qManager.setFinished();
				}

				if (qManager.hasFinished()) {
					// All videos have loaded!
					// Launch preview display
					displayPreviews();
				}
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
	
	/**
	 * Manager for state variables about video preview loading.
	 * This prevents the load attempt from being restarted.
	 * Only one manager should be created for a queue of videos to load.
	 * 
	 * This class may not be necessary when onConfigChange is overridden. Try it...
	 */
	private static class PreviewLoadStateManager {
		boolean started = false;
		boolean finished = false;
		private static PreviewLoadStateManager instance = null;
		
		protected PreviewLoadStateManager() {}
		
		public static PreviewLoadStateManager getInstance() {
			if (instance == null) {
				instance = new PreviewLoadStateManager();
				instance.started = false;
				instance.finished = false;
			}
			return instance;
		}
		
		public void setStarted() {
			instance.started = true;
		}
		
		/**
		 * Use this method only when you want to force 
		 * a restart of the video loading process.
		 */
		public void reset() {
			instance.started = false;
			instance.finished = false;
		}
		
		public void setFinished() {
			instance.finished = true;
		}
		
		public boolean hasStarted() {
			return instance.finished;
		}
		
		public boolean hasFinished() {
			return instance.finished;
		}
	}
	    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initControllerPreferences();
		
		frameRate = -1;
		String frameRateTag = getResources().getString(R.string.prefkeyFrameRate);
		// Set frame rate from savedInstanceState first
		if (savedInstanceState != null) {
			frameRate = savedInstanceState.getInt(frameRateTag, frameRate);
		}
		// That didn't work, so try retrieving from prefs
		if (frameRate == -1) {
			frameRate = getFrameRateFromPreferences(frameRateTag);
		}
		
		/*
		 * Initialize previews
		 * 
		 * Previews are not yet ready when Preview.getAll returns: 
		 * each must be assigned a VideoProvider
		 * 
		 * After videos finish loading (through queue processed by VideoService message handler)
		 * it is safe to hand previews off to PreviewFragment
		 */
		previews = Preview.getAll(MainActivity.this, getResources().getXml(R.xml.previews));
		
		// Initialize video providers cache
		previewVideoProviderCache = new SparseArray<VideoProvider>(previews.size());
	}
	
	/**
	 * Inhibit BluetoothVideoService from being started every time orientation changes.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		// Use different aspect ratio depending on orientation
		double aspectRatio = getPreviewAspectRatio(newConfig.orientation);
		
		// Redraw previews items to fill view at proportional height.
        scalePreviewItemHeights(aspectRatio);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

        Log.i(TAG, "--- ONRESUME ---");
        
		// Resume state indicating that MainActivity is running.
		mainActivityRunning = true;
		
		// Retrieve frameRate from preferences
		frameRate = getFrameRateFromPreferences(getResources().getString(R.string.prefkeyFrameRate));

		if (list != null && activePreview != null) {
			int previewIndex = getPreviewIndexFromPreferences(PreviewLoader.PREVIEW_SELECTED_TAG);
			setSelectedPreview(previewIndex);
			Log.e("ON_RESUME", "Resuming with preview index " + previewIndex + " and frame rate " + frameRate);
			
			if (controller != null) {
				toggleVideoPlaying(controller.isAutoAdvancing(), frameRate);
			}
		}
		
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
                    headController = new MultiheadController(panelWidth, panelHeight);
                    videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, headController);
                    videoSvc.addHandler(headController.getHandler());
                }
                
                /* Bluetooth Service init finished */
                // Prompt user to set up device controllers if none found
                if (headController.getHeads().size() == 0) {
                	AlertDialog alertDialog = getConfigurationAlertBuilder().create();
                	alertDialog.show();
                } else {
                	Log.i(MainActivity.TAG, headController.getHeads().size() + " heads attached");
                }

                /*
                 * Restart video loading with new dimensions 
                 * if panel dimensions have changed on this resume.
                 */
                if (panelDimensionsChanged == true) {
                	// Prepare to reload videos
                	qManager.reset();
                	previewVideoProviderCache = new SparseArray<VideoProvider>(previews.size());
                	
                }
                
                /* Load previews or redraw them if loaded */
                if (!qManager.hasStarted()) {
        			try {
        				activePreview = new PreviewLoader();
        				initializePreviews();
        				qManager.setStarted();
        			} catch (Exception e) {
        				Log.e(MainActivity.TAG, "Previews could not be initialized");
        				e.printStackTrace();
        			}
        		} else if (qManager.hasFinished()) {
                	displayPreviews();
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
			case R.id.action_configure_panels:
				Intent j = new Intent(getBaseContext(), MultiheadSetupActivity.class);
				startActivityForResult(j, MainActivity.MULTIHEAD_SETUP_RESULT);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mainActivityRunning = false;
		
		// Record activePreview index
		if (activePreview != null) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this); //.putInt(PreviewLoader.PREVIEW_SELECTED_TAG, 2);
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt(PreviewLoader.PREVIEW_SELECTED_TAG, activePreview.getListIndex());
			editor.commit();
			//TODO
			Log.d("ON_PAUSE", "stored " + sp.getInt(PreviewLoader.PREVIEW_SELECTED_TAG, -9000));
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
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case MULTIHEAD_SETUP_RESULT:
    		int newWidth, newHeight;
    		newWidth = headController.getVirtualWidth();
    		newHeight = headController.getVirtualHeight();
    	
    		loadPanelDimensionsFromPreferences();
    		if (newWidth != panelWidth || newHeight != panelHeight) {
    			setPanelDimensionsPreferences(newWidth, newHeight);
    		}
    		
    		Log.d(MainActivity.TAG, "setup activity completed");
    		break;
    	default:
    		break;
    	}
    }
    
    // The Handler that gets information back from the BluetoothVideoService
    private final Handler mHandler = new Handler(new IncomingMessageCallback());
    
    private void addConversationLine(String str) {
    	Log.d("BluetoothServiceConnection", str);
    }
	
    private void initializePreviews() throws Exception {
    	if (previews.size() == 0) {
    		throw new Exception("No previews were read from XML");
    	}
    	
    	for (Preview p : previews) {
    		// Copy the video asset to a file if it doesn't already exist.
			p.saveVideoFileAsset(getApplicationContext());

			// Enqueue the preview for loading video assets
			q.add(p);    		
    	}
    	
    	// Set activePreview as first element
    	activePreview.attachPreview(q.peek());

    	loadNextPreviewVideo();
    }
    
    public void loadNextPreviewVideo() {
    	FFMPEGVideoProvider ffmpeg = new FFMPEGVideoProvider(MainActivity.this, videoSvc, headController.getVirtualWidth(), headController.getVirtualHeight(), COLOR_PREVIEW);
        File f = new File(getApplicationContext().getFilesDir(), activePreview.getPreview().getFilename());
    	// File should exist at this point.
        // Fatal error if it does not.
        
    	ffmpeg.loadVideo(f);
    	activePreview.setVideoProvider(ffmpeg);
    }
    
    public void displayPreviews() {
    	Log.e(MainActivity.TAG, "Running displayPreviews");
    	
    	// TODO prevent double flash
    	list = new PreviewFragment();
    	Bundle fragmentData = new Bundle();
    	fragmentData.putParcelableArrayList(MainActivity.PREVIEWS_DATA_TAG, previews);
		list.setArguments(fragmentData);
		
		if (mainActivityRunning == true) {
			getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
		}
    }
    
    /**
     * Scale the height of preview items
     * based on the container width.
     */
    public void scalePreviewItemHeights(double aspectRatio) {
    	list.redrawPreviewItems(findViewById(R.id.frameLayout).getWidth(), aspectRatio);    		
    }
    
    public int getPreviewItemHeight() {
    	int width = findViewById(R.id.frameLayout).getWidth();
    	return (int) (width * getPreviewAspectRatio());
    }
    
    private double getPreviewAspectRatio() {
    	return getPreviewAspectRatio(getResources().getConfiguration().orientation);
    }

    private double getPreviewAspectRatio(int orientation) {
    	double aspectRatio = 1.0;
    	if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		return aspectRatio / 4;
    	}
    	return aspectRatio / 6;
    }
    
    /**
     * Get the mapping of preview IDs to loaded video providers
     * @return video provider cache
     */
    public SparseArray<VideoProvider> getVideoProviderCache() {
    	return previewVideoProviderCache;
    }
    
    public BluetoothVideoService getBluetoothVideoService() {
    	return videoSvc;
    }
    
    public void setSelectedPreview(int index) {
    	if (index == PreviewFragment.PREVIEW_NOT_SET_INDEX) {
    		
    		// Log.e("MAIN", "Deactivating any active preview");
    		int oldListIndex = activePreview.getListIndex();
    		list.deactivateItem(oldListIndex);
    		activePreview.setListIndex(PreviewFragment.PREVIEW_NOT_SET_INDEX);
    		
    		toggleVideoPlaying(false);
    		
    	} else {
    		
    		//Log.e("MAIN", "Testing to see if any preview must be deselected");
    		if (activePreview == null) {
    			activePreview = new PreviewLoader(index);
    		} else {
    			int oldListIndex = activePreview.getListIndex();
    			if (oldListIndex != PreviewFragment.PREVIEW_NOT_SET_INDEX) {
    				list.deactivateItem(oldListIndex);
        			
    				toggleVideoPlaying(false);
    			}
    				
    			activePreview.setListIndex(index);
    		}
    		
    		Preview currentPreview = list.getPreviewAt(index);
			activePreview.attachPreview(currentPreview);
			activePreview.setVideoProvider(previewVideoProviderCache.get(currentPreview.hashCode()));
			
	    	controller = new FrameController<VideoProvider, MultiheadController>(activePreview.getVideoProvider(), headController, videoSvc);
	        videoSvc.setConfigInstance(FrameController.CONFIG_INSTANCE_KEY, controller);
	        
	        list.activateItem(index);
    	}
    }
    
    /**
     * Click handler for preview icon presses.
     * @param index
     */
    public void togglePlayPreview(int index) {
    	if (activePreview == null) {
    		setSelectedPreview(index);
    		list.setSelectedItem(index);
    		// Log.e("TOGGLE", "Create new and start playing video at index " + index);
    	}
    	
    	if (controller == null) {
    		controller = new FrameController<VideoProvider, MultiheadController>(activePreview.getVideoProvider(), headController, videoSvc);
	        videoSvc.setConfigInstance(FrameController.CONFIG_INSTANCE_KEY, controller);
    	}
    	
    	if (activePreview.getListIndex() == index) {
    		toggleVideoPlaying(!(activePreview.isPlaying()));
    		// Log.e("TOGGLE", "Start playing video at index " + index);
    	} else {
    		// Turn off old video
    		toggleVideoPlaying(false);
			
    		setSelectedPreview(index);
    		list.setSelectedItem(index);

    		// Start new video
    		toggleVideoPlaying(true);
    	}	
    }
    
    private void toggleVideoPlaying(boolean shouldPlay) {
    	if (controller != null) {
	    	controller.setAutoAdvance(shouldPlay, MainActivity.getDelayFrameRate(frameRate), null);
			activePreview.setPlaying(shouldPlay);
    	}
    }
    
    private void toggleVideoPlaying(boolean shouldPlay, int newFrameRate) {
    	if (controller != null) {
	    	controller.setAutoAdvance(shouldPlay, MainActivity.getDelayFrameRate(newFrameRate), null);
			activePreview.setPlaying(shouldPlay);
    	}
    }
    
    /**
     * Get an AlertDialog.Builder to construct the error dialog
     * when no device configuration has been set.
     * 
     * Resulting Builder must have create() called on it.
     * @return builder for alert dialog
     */
    private AlertDialog.Builder getConfigurationAlertBuilder() {
    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    	
    	alertDialogBuilder.setTitle(getResources().getString(R.string.txtNoDevices));
    	alertDialogBuilder
	    	.setMessage(getResources().getString(R.string.txtExplainNoDevices))
	    	.setCancelable(false)
	    	.setPositiveButton(R.string.txtConnect, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(MainActivity.this, MultiheadSetupActivity.class);
					startActivityForResult(i, MainActivity.MULTIHEAD_SETUP_RESULT);					
				}
			})
			.setNegativeButton(R.string.txtExit, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
	
    	return alertDialogBuilder;
    }
    
    private void loadPanelDimensionsFromPreferences() {
    	panelWidth = controllerPrefs.getInt(MainActivity.PREFS_WIDTH, panelWidth);
    	panelHeight = controllerPrefs.getInt(MainActivity.PREFS_HEIGHT, panelHeight);
    	panelDimensionsChanged = false;
    }
    
    private void setPanelDimensionsPreferences(int w, int h) {
    	SharedPreferences.Editor editor = controllerPrefs.edit();
		editor.putInt(MainActivity.PREFS_WIDTH, w);
		editor.putInt(MainActivity.PREFS_HEIGHT, h);
		editor.commit();
    	panelDimensionsChanged = true;
    }
    
    public boolean getPanelDimensionsChanged() {
    	return panelDimensionsChanged;
    }
    
    private void initControllerPreferences() {
		controllerPrefs = getSharedPreferences("controller", Context.MODE_PRIVATE);
		
		if (controllerPrefs.getInt(MainActivity.PREFS_WIDTH, 0) == 0 
				|| controllerPrefs.getInt(MainActivity.PREFS_HEIGHT, 0) == 0) {
			setPanelDimensionsPreferences(panelWidth, panelHeight);
		}
		
		loadPanelDimensionsFromPreferences();
    }
    
    //TODO remove
    public void dumpPreferences() {
    	Log.i("PREFERENCE", "prefFrameRate: " + PreferenceManager.getDefaultSharedPreferences(this).getInt(getString(R.string.prefkeyFrameRate), -1));
    	Log.i("PREFERENCE", "prefHeight: " + controllerPrefs.getInt(MainActivity.PREFS_HEIGHT, 0));
    }
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	    Log.e("INSTANCE_STATE", "SAVING INSTANCE STATE");
	    
	   savedInstanceState.putInt(getResources().getString(R.string.prefkeyFrameRate), frameRate);
	}
	
	public int getFrameRateFromPreferences(String key) {
		String[] frameRates = getResources().getStringArray(R.array.valuesFrameRate);
		
		int frameRateValueIndex = PreferenceManager.getDefaultSharedPreferences(this).getInt(key, 2); 
		return Integer.valueOf(frameRates[frameRateValueIndex]);
	}
	
	public static int getDelayFrameRate(int frameRate) {
		return (int) Math.floor(1000.0 / frameRate);
	}
	
	public int getPreviewIndexFromPreferences(String key) {
		return PreferenceManager.getDefaultSharedPreferences(this).getInt(key, PreviewLoader.UNSET_INDEX);
	}
}