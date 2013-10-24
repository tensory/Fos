package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.R;

public class PreviewListItemLayoutView extends RelativeLayout {
	private Context context;
	private boolean isActive;
	public ImageView ivBtnPreview, ivBtnAccept;
	private AnimationTask animation;

	
	private class AnimationTask extends AsyncTask<VideoProvider, Void, Void> {

		@Override
		protected Void doInBackground(VideoProvider... params) {
			/* VideoProvider does not provide a public interface
			 * to its frameList, so one must be approximated
			 * by iterating over frames
			 */
			ImageView iv = (ImageView) findViewById(R.id.ivPreview);
			VideoProvider provider = params[0];
			int frameCount = provider.getFrameCount();
			for (int i = 0; i < frameCount; i++) {
				Bitmap bmp = Preview.FrameExtractor.getFrameBitmap(provider, i);
				PreviewAdapter.setBackgroundImage(context, iv, bmp);
			}
			
			return null;
		}
	}
	
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
	
	public void toggleAnimation(VideoProvider provider) {
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		if (animation == null) {
			animation = new AnimationTask();
		}

//		if (!animation.isRunning() && isActive) {
			animation.execute();
//		} else {
//			stopAndReset(animation);
//		}
	}
	
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
}
