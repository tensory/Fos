package com.erogear.android.fos;

import android.content.Intent;
import android.util.Log;

import com.erogear.android.bluetooth.comm.BluetoothVideoService;
import com.erogear.android.bluetooth.comm.DeviceConnection;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.TranslateVirtualFrame;

public class HeadControllerManager {
	public String DEVICE_INTENT_KEY;
	private int connectionsExpected, connectionsCompleted;
	private MultiheadController headController;
	
	// Create a HeadControllerManager that can wait for multiple heads to be attached
	public HeadControllerManager(final String key) {
		DEVICE_INTENT_KEY = key;
		connectionsExpected = 0;
		connectionsCompleted = 0;
	}
	
	public MultiheadController getNewHeadController(int width, int height) {
		headController = new MultiheadController(width, height);
		return headController;
	}
	
	public MultiheadController getHeadController() {
		return headController;
	}
	
	public void connectDevices(String[] addresses, BluetoothVideoService svc) {
		Intent intent = new Intent();
		connectionsExpected = addresses.length;
		connectionsCompleted = 0;
			
		for (int i = 0; i < addresses.length; i++) {
			String address = addresses[i];
			intent.putExtra(DEVICE_INTENT_KEY, address);
			
			svc.connectDevice(intent, true);
		}
	}
	
	public boolean waiting() {
		return (connectionsCompleted < connectionsExpected);
	}
	
	public boolean ready() {
		return waiting();
	}
	
	public void addHead(DeviceConnection connection) {
		headController.mapHead(connection, new TranslateVirtualFrame(connection.getInputWidth(), connection.getInputHeight(), 0, 0), true);
		connectionsCompleted += 1;
	}
	
	public void finishConnection() {
		connectionsCompleted += 1;
	}
	
	public void pushStatusFrame() {
		Log.i(MainActivity.BLUETOOTH_TAG, "Pushing status frame to controller");
	}
	
	/*
	 * public void notifySetupChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                final float scaleX = (float)getWidth()/(float)getPanelWidth();
                final float scaleY = (float)getHeight()/(float)getPanelHeight();

                removeAllViews();

                int idx = 0;

                for (final MultiheadController.Head e : controller.getHeads().values()) {
                    if(e.map instanceof TranslateVirtualFrame) {
                        final TranslateVirtualFrame tf = (TranslateVirtualFrame) e.map;
                        ++idx;

                        if(!e.enabled) {
                            e.consumer.sendFrame(DeviceConnection.BLACK_FRAME);
                        }
                        else {
                            e.consumer.sendFrame(new NumberFrame(idx));
                        }

                        final Point touchPoint = new Point();
                        final TextView tv = new TextView(getContext()) {
                            @Override
                            protected void onDraw(Canvas canvas) {
                                super.onDraw(canvas);
                                canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
                            }

                            @Override
                            public boolean onTouchEvent(final MotionEvent event) {
                                if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                    touchPoint.x = (int) event.getX();
                                    touchPoint.y = (int) event.getY();

                                    Log.i("wtf", "Touch point is now " + touchPoint.x + " " + touchPoint.y);
                                }

                                return super.onTouchEvent(event);
                            }

                            @Override
                            public boolean onDragEvent(DragEvent event) {
                                switch (event.getAction()) {
                                    case DragEvent.ACTION_DRAG_STARTED:
                                        return true;
                                    case DragEvent.ACTION_DRAG_ENDED:
                                        setVisibility(View.VISIBLE);
                                }

                                return false;
                            }

                            @Override
                            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                                setMeasuredDimension((int) (((float)tf.getWidth())*scaleX), (int) (((float) tf.getHeight())*scaleY));
                            }
                        };

                        tv.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                controller.enableHead(e.consumer, !e.enabled);

                                notifySetupChanged();
                            }
                        });

                        tv.setOnLongClickListener(new OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                ClipData clipData = ClipData.newPlainText("", "");
                                TextView.DragShadowBuilder dsb = new TextView.DragShadowBuilder(tv) {
                                    @Override
                                    public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
                                        super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);

                                        shadowTouchPoint.x = touchPoint.x;
                                        shadowTouchPoint.y = touchPoint.y;
                                    }
                                };

                                startDrag(clipData, dsb, new DragData(touchPoint.x, touchPoint.y, tf, e.consumer), 0);
                                tv.setVisibility(View.INVISIBLE);
                                return true;
                            }
                        });

                        tv.setClickable(true);
                        tv.setLongClickable(true);

                        addView(tv);

                        tv.setText(new SpannableString(idx + ": " + e.consumer.getName()));
                        if(e.enabled) {
                            tv.setBackgroundColor(Color.argb(128, 64, 255, 64));
                        }
                        else {
                            tv.setBackgroundColor(Color.argb(128, 64, 64, 64));
                        }

                        tv.setPadding(10, 10, 10, 10);

                        /*tv.setWidth(100);
                        tv.setHeight(100);
                        tv.setTranslationX(32*idx);
                        tv.setTranslationY(32*idx);*/
/*
                        //tv.setWidth((int) (((float)tf.getWidth())*scaleX));
                        //tv.setHeight((int) (((float) tf.getHeight())*scaleY));
                        tv.setTranslationX(((float) tf.getTx()) * scaleX);
                        tv.setTranslationY(((float) tf.getTy()) * scaleY);

                        Log.i("wtf", scaleX + " " + scaleY + " " +  tv.getX() + " " + tv.getY() + " " + tv.getWidth() + " " + tv.getHeight());
                    }
                }
            }
	 */
}