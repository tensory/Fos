package com.erogear.android.fos;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.erogear.android.bluetooth.comm.BluetoothChatService;
import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.comm.DeviceListActivity;
import com.erogear.android.bluetooth.comm.FrameConsumer;
import com.erogear.android.bluetooth.comm.MultiheadSetupActivity;
import com.erogear.android.bluetooth.video.FFMPEGVideoProvider;
import com.erogear.android.bluetooth.video.FrameController;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.MultiheadController.Head;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	// Intent request codes
	public static final int PREFERENCE_INTENT_RESULT = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int MULTIHEAD_SETUP_RESULT = 3;

	// Preferences
	private TextView headControllerStatus;
	private ControllerPreferenceManager controllerPreferences;
	private HeadControllerManager controllerBuilder;
	private int frameRate;
	private int selectedPreviewIndex;
	private int panelWidth = 32;
	private int panelHeight = 24;

	// Tags
	public static final String TAG = "MAIN";
	public static final String VIDEO_PLAYING = "VIDEO_PLAYING";
	public static final String PREVIEWS_DATA_TAG = "previews";

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
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					conn = (DeviceConnection) msg.obj;

					if (controllerBuilder.getHeadController() != null && controllerBuilder.waiting()) {
						controllerBuilder.addHead(conn);

						// Push a status frame to the newly paired device.
						for (Entry<FrameConsumer, Head> entry : headController.getHeads().entrySet()) {
							FrameConsumer lastPairedDevice = entry.getKey();
							if (lastPairedDevice.getName() == conn.getDeviceName()) {
								int deviceIndex = controllerPreferences.getDeviceIndex(lastPairedDevice.getName());
								controllerBuilder.pushStatusFrame(entry.getValue(), deviceIndex + 1);
								break;
							}
						}
					}

					if (controllerBuilder.getHeadController() != null && controllerBuilder.ready()) {						
						// Link it to the VideoService
						videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, controllerBuilder.getHeadController());
					}

					break;
				case BluetoothChatService.STATE_CONNECTION_LOST:
					conn = (DeviceConnection) msg.obj;
					Toast.makeText(MainActivity.this, "Lost connection to " + conn.getDeviceName(), Toast.LENGTH_SHORT).show();
					break;
				case BluetoothChatService.STATE_CONNECTION_FAIL:
					conn = (DeviceConnection) msg.obj;
					if (controllerBuilder.waiting()) {
						controllerBuilder.finishConnection();
					}

					if (controllerBuilder.ready()) {						
						// Loop has finished but headController is not usable; at least one head failed to attach
						// Require the user to do a new setup

						headController = new MultiheadController(panelWidth, panelHeight);
						videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, headController);
						videoSvc.addHandler(headController.getHandler());
						alertNoControllerPaired();
					}
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
					controllerPreferences.setPanelDimensionsChangedFlag(false);
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
		}
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
		/*
		// Use different aspect ratio depending on orientation
		double aspectRatio = getPreviewAspectRatio(newConfig.orientation);

		// Redraw previews items to fill view at proportional height.
        scalePreviewItemHeights(aspectRatio);
		 */
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "--- ONRESUME ---");

		
		controllerBuilder = new HeadControllerManager(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

		// Resume state indicating that MainActivity is running.
		mainActivityRunning = true;

		// Update frame rate
		frameRate = getFrameRateFromPreferences(getResources().getString(R.string.prefkeyFrameRate));
		toggleVideoPlaying(false, frameRate);
		
		// Restore preview highlighted state
		selectedPreviewIndex = getPreviewIndexFromPreferences(PreviewLoader.PREVIEW_SELECTED_TAG);

		/*
		 * These state vars aren't useful here, not yet
		 * Video reselection must be done after displayPreviews is finished

		// Retrieve state from preferences

		boolean videoPlaying = controllerPrefs.getBoolean(MainActivity.VIDEO_PLAYING, false);
		if (list != null && activePreview != null) {
			int previewIndex = getPreviewIndexFromPreferences(PreviewLoader.PREVIEW_SELECTED_TAG);

			setSelectedPreview(previewIndex);
			list.setSelectedItem(previewIndex);
			list.activateItem(previewIndex);
			Log.e("ON_RESUME", "Resuming with preview index " + previewIndex + " and frame rate " + frameRate);

		}
		 */

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
					// This call starts up an asynchronous loop with the Handler. 
					// Device connections (including failed connections) will trigger UI status changes
					headController = controllerPreferences.getHeadController(controllerBuilder, videoSvc);
					if (headController != null) {
						videoSvc.addHandler(headController.getHandler());                		
					}
				} 

				if (headController == null) { 
					// If headController is still null, the controller could not be reconstituted from preferences
					// Require the user to do manual setup
					headController = new MultiheadController(panelWidth, panelHeight);
					videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, headController);
					videoSvc.addHandler(headController.getHandler());

					alertNoControllerPaired();
				} 
				Log.e(MainActivity.TAG, "END ONRESUME CALLBACK");
				/*FragmentManager fragmentManager = getSupportFragmentManager();
						
						.beginTransaction().replace(R.id.frameLayout, list, PreviewFragment.FRAGMENT_TAG).commit();
*/
				
				/* Bluetooth Service init finished */

				/*
				 * Restart video loading with new dimensions 
				 * if panel dimensions have changed on this resume.
				 */
				
				
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
					/*
					FragmentManager fragmentManager = getSupportFragmentManager();
					PreviewFragment pf = (PreviewFragment) fragmentManager.findFragmentByTag(PreviewFragment.FRAGMENT_TAG);
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.show(pf);
				    fragmentTransaction.commit();
				    */
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				videoSvc.removeHandler(mHandler);
			}
		};

		bindService(new Intent(MainActivity.this, BluetoothVideoService.class), svcConn, Service.START_STICKY);
		Log.d(MainActivity.TAG, "Bluetooth started");
		Log.e(MainActivity.TAG, "END ONRESUME");
	}
	
	@Override
	public void onPostResume() {
		super.onPostResume();
		Log.e(MainActivity.TAG, "ON POST RESUME");
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
		}

		// Record whether controller is currently playing
		if (controller != null) {
			SharedPreferences.Editor controllerEditor = controllerPreferences.getPreferences().edit();
			controllerEditor.putBoolean(MainActivity.VIDEO_PLAYING, controller.isAutoAdvancing());
			controllerEditor.commit();
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
			RelativeLayout controllerStatusBar = (RelativeLayout) findViewById(R.id.layoutControllerStatus);
			if (headController.getHeads().size() > 0) {
				controllerStatusBar.setVisibility(View.GONE);
			} else {
				controllerStatusBar.setVisibility(View.VISIBLE);
			}
			
			int newWidth, newHeight;
			newWidth = headController.getVirtualWidth();
			newHeight = headController.getVirtualHeight();

			loadPanelDimensionsFromPreferences();
			if (newWidth != panelWidth || newHeight != panelHeight) {
				setPanelDimensionsPreferences(newWidth, newHeight);
				
				// Prepare video queue manager to reload videos if panel dimensions have changed.
				// TODO double check
				// this was in the callback for video service being created, but it may not be a dependency
				if (controllerPreferences.getPanelDimensionsChanged() == true) {
					qManager.reset();
					previewVideoProviderCache = new SparseArray<VideoProvider>(previews.size());
				}
			}

			// Set controller address(es) in preferences for next time a headController is constructed
			controllerPreferences.storeDeviceAddresses(headController.getHeads(), BluetoothAdapter.getDefaultAdapter().getBondedDevices());

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
			getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list, PreviewFragment.FRAGMENT_TAG).commit();
			/*
			if (selectedPreviewIndex != PreviewFragment.PREVIEW_NOT_SET_INDEX) {
				setSelectedPreview(selectedPreviewIndex);
			}
			*/
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
		Runnable videoBlankProcess = new Runnable() {
			@Override
			public void run() {
				if (controller != null) {
					controller.getFrameConsumer().sendFrame(DeviceConnection.BLACK_FRAME);					
				}
			}
		};

		if (index == PreviewFragment.PREVIEW_NOT_SET_INDEX) {
			
			// Log.e("MAIN", "Deactivating any active preview");
			int oldListIndex = activePreview.getListIndex();
			if (oldListIndex != PreviewFragment.PREVIEW_NOT_SET_INDEX) {
				list.deactivateItem(oldListIndex);
				activePreview.setListIndex(PreviewFragment.PREVIEW_NOT_SET_INDEX);
			}

			toggleVideoPlaying(false);
			runOnUiThread(videoBlankProcess);

		} else {

			// Log.e("MAIN", "Testing to see if any preview must be deselected");
			if (activePreview == null) {
				activePreview = new PreviewLoader(index);
			} else {
				int oldListIndex = activePreview.getListIndex();
				if (oldListIndex != PreviewFragment.PREVIEW_NOT_SET_INDEX) {
					list.deactivateItem(oldListIndex);

					toggleVideoPlaying(false);
					runOnUiThread(videoBlankProcess);
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
		.setCancelable(true)
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				RelativeLayout controllerStatusBar = (RelativeLayout) MainActivity.this.findViewById(R.id.layoutControllerStatus);
				controllerStatusBar.setVisibility(View.VISIBLE);
			}
		})
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
				dialog.cancel();
			}
		});

		return alertDialogBuilder;
	}

	private void loadPanelDimensionsFromPreferences() {
		panelWidth = controllerPreferences.getPreferences().getInt(ControllerPreferenceManager.PREFS_WIDTH, panelWidth);
		panelHeight = controllerPreferences.getPreferences().getInt(ControllerPreferenceManager.PREFS_HEIGHT, panelHeight);
		controllerPreferences.setPanelDimensionsChangedFlag(false);
	}

	private void setPanelDimensionsPreferences(int w, int h) {
		SharedPreferences.Editor editor = controllerPreferences.getPreferences().edit();
		editor.putInt(ControllerPreferenceManager.PREFS_WIDTH, w);
		editor.putInt(ControllerPreferenceManager.PREFS_HEIGHT, h);
		editor.commit();
		controllerPreferences.setPanelDimensionsChangedFlag(true);
	}

	private void initControllerPreferences() {
		headControllerStatus = (TextView) findViewById(R.id.tvControllerStatusMessage);
		controllerPreferences = new ControllerPreferenceManager(MainActivity.this);

		SharedPreferences.Editor editor = controllerPreferences.getPreferences().edit();
		if (controllerPreferences.getPreferences().getInt(ControllerPreferenceManager.PREFS_WIDTH, 0) == 0 
				|| controllerPreferences.getPreferences().getInt(ControllerPreferenceManager.PREFS_HEIGHT, 0) == 0) {
			editor.putInt(ControllerPreferenceManager.PREFS_WIDTH, panelWidth);
			editor.putInt(ControllerPreferenceManager.PREFS_HEIGHT, panelHeight);
			editor.commit();
		}
		controllerPreferences.setPanelDimensionsChangedFlag(true);

		// Init with no controller playing
		editor.putBoolean(MainActivity.VIDEO_PLAYING, false);
		editor.commit();

		loadPanelDimensionsFromPreferences();
	}

	/**
	 * Fire an alert dialog to prompt the user to set up the controller.
	 */
	private void alertNoControllerPaired() {
		AlertDialog alertDialog = getConfigurationAlertBuilder().create();
		alertDialog.show();	
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
	
	public boolean getPanelDimensionsChanged() {
		return controllerPreferences.getPanelDimensionsChanged();
	}
}