package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.fos.R;

public class PreviewListItemLayoutView extends RelativeLayout {
	private boolean isActive;
	public ImageView ivBtnPreview, ivBtnAccept;
	
	public PreviewListItemLayoutView(Context context) {
		super(context);
	}

	public PreviewListItemLayoutView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PreviewListItemLayoutView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
	
	public void toggleAnimation() {
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		AnimationDrawable animation = (AnimationDrawable) iv.getBackground();
		if (!animation.isRunning() && isActive) {
			animation.start();
		} else {
			stopAndReset(animation);
		}
	}
	
	public void stopAnimation() {
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		AnimationDrawable animation = (AnimationDrawable) iv.getBackground();
		stopAndReset(animation);
	}
	
	private void stopAndReset(AnimationDrawable d) {
		d.stop();
		d.selectDrawable(0);
	}
	
	private void setIconState() {
		if (this.isActive) {
			//TODO: replace this pair of calls with a pair of Drawables with highlight state
			ivBtnPreview.setImageResource(R.drawable.ic_active_preview);
			ivBtnAccept.setImageResource(R.drawable.ic_active_send);
		} else {
			ivBtnPreview.setImageResource(R.drawable.ic_inactive_preview);
			ivBtnAccept.setImageResource(R.drawable.ic_inactive_send);
		}	
	};
}
