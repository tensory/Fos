package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.bluetooth.video.FrameController;
import com.erogear.android.bluetooth.video.MultiheadController;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.R;

public class PreviewListItemLayoutView extends RelativeLayout {
	private static final int UNSET_FRAME_ID = -1;
	private Context context;
	private boolean isActive;
	public ImageView ivBtnPreview, ivBtnAccept;
	private int lastFrameId;
	
	public PreviewListItemLayoutView(Context context) {
		super(context);
		this.context = context;
	}

	public PreviewListItemLayoutView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public PreviewListItemLayoutView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	
	@Override
	public void onFinishInflate() {
		isActive = false;
		ivBtnPreview = (ImageView) findViewById(R.id.ivBtnPreview);
		ivBtnAccept = (ImageView) findViewById(R.id.ivBtnAccept);
	}
	
	public void toggleActive() {
		isActive = !isActive;
		setIconState();
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	/** DEPRECATED
	public void toggleAnimation(VideoProvider provider) {
		
//		if (!animation.isRunning() && isActive) {
		startAnimation(provider);
//		} else {
//			stopAndReset(animation);
//		}
	}
	*/
	public void stopAnimation() {
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		AnimationDrawable animation = (AnimationDrawable) iv.getBackground();
		stopAndReset(animation);
	}
	
	public void deactivate() {
		isActive = false;
		setIconState();
	}
	
	private void stopAndReset(AnimationDrawable d) {
		d.stop();
		d.selectDrawable(0);
	}
	
	private void setIconState() {
		if (this.isActive) {
			//TODO: replace this pair of calls with a pair of Drawables with highlight state
			activateImageButton(ivBtnPreview, R.drawable.ic_active_preview);
			activateImageButton(ivBtnAccept, R.drawable.ic_active_send);
		} else {
			deactivateImageButton(ivBtnPreview, R.drawable.ic_inactive_preview);
			deactivateImageButton(ivBtnAccept, R.drawable.ic_inactive_send);
		}	
	};
	
	private void activateImageButton(ImageView target, int resId) {
		target.setImageResource(resId);
		target.setClickable(true);
	}
	
	private void deactivateImageButton(ImageView target, int resId) {
		target.setImageResource(resId);
		target.setClickable(false);
	}
	
/** DEPRECATED
	private void startAnimation(VideoProvider provider) {
		if (lastFrameId == PreviewListItemLayoutView.UNSET_FRAME_ID) {
			lastFrameId = 0;
		}
		
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		
		for (int i = lastFrameId; i < provider.getFrameCount(); i++) {
			Bitmap bmp = Preview.FrameExtractor.getFrameBitmap(provider, i);
			PreviewAdapter.setBackgroundImage(context, iv, bmp);			
		}
		
		Log.d("PVLIV", "Done");
	}
	*/

	/**
	 * Set the height of the list item
	 * and the background ImageView.
	 * @param height
	 */
	public void setLayoutHeight(int height) {
		ViewGroup.LayoutParams dims = this.getLayoutParams();
		dims.height = height;
		this.setLayoutParams(dims);
	}
}
