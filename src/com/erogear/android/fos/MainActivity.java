package com.erogear.android.fos;

import java.io.File;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.erogear.android.bluetooth.video.ByteBufferFrame;
import com.erogear.android.bluetooth.video.FFMPEGVideoProvider;
import com.erogear.android.bluetooth.video.FrameController;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.fragments.PreviewFragment;

public class MainActivity extends SherlockFragmentActivity {
	public static int PREFERENCE_INTENT_RESULT = 1;
	PreviewFragment list;
	AssetFileWriter afwriter;
	
	// Class vars for probably a connection class
	private static final int REQUEST_ENABLE_BT = 3;
	private FrameController<VideoProvider, MultiheadController> controller;
	
    private MultiheadController headController;
	private VideoProvider provider;
	private ServiceConnection svcConn;
    private BluetoothVideoService videoSvc;
    Bitmap bm;
    BitmapDrawable bmd;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = new PreviewFragment();
		// this next call probably belongs in a loading activity
		afwriter = new AssetFileWriter(MainActivity.this);
		
        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, list).commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		Log.d("MainActivity", "+++ onResume +++");
		/* Everything going on to create a connection should be pushed to its own class */
		
        //First, put up a progress dialog because our UI isn't hooked up to anything yet.
        final View progress = findViewById(R.id.globalProgress);

        startService(new Intent(this, BluetoothVideoService.class));

		svcConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                String appName = getString(R.string.app_name);
                Notification note=new Notification(R.drawable.ic_launcher, appName, System.currentTimeMillis());
                Intent i=new Intent(MainActivity.this, MainActivity.class);

                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, i, 0);

                note.setLatestEventInfo(MainActivity.this, appName, getString(R.string.eg_service_subtitle), pi);
                note.flags|=Notification.FLAG_NO_CLEAR;

                BluetoothVideoService.MyBinder myBinder = (BluetoothVideoService.MyBinder) service;
                videoSvc = myBinder.getService();

                Intent btOn = videoSvc.start(note); //If bluetooth is off, this returns an intent that will get it turned on
                videoSvc.addHandler(mHandler);

                if(btOn != null){
                    startActivityForResult(btOn, REQUEST_ENABLE_BT);
                }

                // ConfigInstance is just an arbitrary object we want associate with the service.
                // In this app, we keep the framecontroller there from one activty invocation to another.
                controller = (FrameController) videoSvc.getConfigInstance(FrameController.CONFIG_INSTANCE_KEY);

                if (controller != null) {
                    provider = controller.getVideoProvider();
/*
                    //Already running, so make UI Right:
                    if (controller.isAutoAdvancing()) {
                        autoAdvanceButton.setImageResource(android.R.drawable.ic_media_pause);
                    }

                    mSeekBar.setMax(provider.getFrameCount());
                    mSeekBar.setProgress(controller.getCurrentFrame());

                    int[] delays = (int[]) fpsControl.getTag();
                    for(int j=0; j<delays.length; j++) {
                        if(delays[j] <= controller.getAutoAdvanceInterval()) {
                            fpsControl.setValue(j);
                        }
                    }
                    */
                }


                headController = (MultiheadController) videoSvc.getConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY);

                if (headController == null) {
                    headController = new MultiheadController(32, 24);
                    videoSvc.setConfigInstance(MultiheadController.CONFIG_INSTANCE_KEY, headController);
                    videoSvc.addHandler(headController.getHandler());
                }

                setupPreviewBitmap();

                progress.setVisibility(View.GONE);

                // If we needed to do something after the service connects, do it.
                if (afterServiceBound != null) {
                    afterServiceBound.run();
                    afterServiceBound = null;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                videoSvc.removeHandler(mHandler);
            }
        };

        bindService(new Intent(MainActivity.this, BluetoothVideoService.class), svcConn, Service.START_STICKY);
        Log.d("MAIN", "Done resuming");
		/* End connection setup */
	}
	
	public void onClickPreview(View v) {
		// Figure out which preview needs to be playing.
		
		String filename = list.getSelectedPreview().getFilename();
		
		// Gah, creating the file here, and not at load time. NOT OKAY
		File local = afwriter.fileFromAsset(filename);
		Log.d("MAIN", "Done loading file " + local.getName());
		
		FFMPEGVideoProvider ffmpeg = new FFMPEGVideoProvider(MainActivity.this, videoSvc, headController.getVirtualWidth(), headController.getVirtualHeight(), false);
		ffmpeg.loadVideo(local);
        provider = ffmpeg;
        
        if (controller != null) {
            controller.setAutoAdvance(false);
        }
        
        controller = new FrameController<VideoProvider, MultiheadController>(provider, headController, videoSvc);
        videoSvc.setConfigInstance(FrameController.CONFIG_INSTANCE_KEY, controller);
        Log.d("MAIN", "Sending now?");
		// Obtain frames for it and set the controller to use these frames.
		
		
		// set the controller to be using this preview's file
		/*
		if (controller == null) return;
        controller.advanceCurrentFrame(1);
        controller.sendCurrentFrame();
        */
	}
	
	// The Handler that gets information back from the BluetoothVideoService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
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
                addConversationLine("Me: " + byteArrayToHex(writeBuf));
                break;
            case BluetoothVideoService.MESSAGE_READ:
                //byte[] readBuf = (byte[]) msg.obj;
                //String readMessage = byteArrayToHex(readBuf);
                addConversationLine(((FrameConsumer) msg.obj).getName() + ":  " + msg.arg1 + " " + msg.arg2);
                break;
            case BluetoothVideoService.MESSAGE_NEW_FRAME:
            	previewFrame((ByteBufferFrame) msg.obj);
            	
//                mSeekBar.setProgress(msg.arg1);
//                mSeekBar.setMax(msg.arg2);
                break;
            case BluetoothVideoService.MESSAGE_VIDEO_LOADED:
//                mSeekBar.setMax(msg.arg1);
            	
            	// when video has loaded, THEN it's okay to start pushing it to the device continuously
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
        }
    };
    
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
	
	/* Pull into separate class */
    private void setupPreviewBitmap() {
        //Preview bitmap, so we don't recreate it every frame
        bm = Bitmap.createBitmap(headController.getVirtualWidth(), headController.getVirtualHeight(), Bitmap.Config.ARGB_8888);
        bmd = new BitmapDrawable(getResources(), bm);

        bmd.setDither(false);
        bmd.setFilterBitmap(false);
    }
    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        int i=0;

        for(byte b: a) {
            sb.append(String.format("%02x ", b&0xff));
            if(++i>32) break;
        }

        return sb.toString();
    }
    

    private Runnable afterServiceBound = null;
    private void doAfterServiceBound(Runnable r) {
        if(videoSvc == null) {
            afterServiceBound = r;
        }
        else {
            r.run();
        }
    }
    

    private void addConversationLine(String str) {
    	Log.d("CONVERSATION", str);
   // May not be necessary if  	
/*
 * 
    	if(str == null) str = "null";

        if(mConversationArrayAdapter.getCount() > 50) {
            mConversationArrayAdapter.remove(mConversationArrayAdapter.getItem(0));
        }

        mConversationArrayAdapter.add(str);
        */
    }
    
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;

    private void previewFrame(ByteBufferFrame frameData) {
        VideoProvider.frameToBitmap(frameData, bm);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bmd.invalidateSelf();
                //mImageViewGrayscale.setImageDrawable(bmd);
            }
        });
    }

    private long lastNoDeviceToast = 0;
    
    /* End methods that belong in their own class */ 
    
    @Override
    public void onStop() {
        super.onStop();
        Log.i("MAIN", "--- ONSTOP ---");
        if (svcConn != null) {
        	unbindService(svcConn);
        }
    }

}