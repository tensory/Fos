package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.bluetooth.video.ByteBufferFrame;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.R;

public class PreviewListItemLayoutView extends RelativeLayout {
	private boolean isActive;
	public ImageView ivBtnPreview, ivBtnAccept;
	public Bitmap bm;
	public BitmapDrawable bmd;
	
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
	
	public void activate() {
		isActive = true;
		setIconState();
	}
	
	public void deactivate() {
		isActive = false;
		setIconState();
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
	
	/*
	public void toggleActive() {
		isActive = !isActive;
		setIconState();
	}
	
	public boolean isActive() {
		return isActive;
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
	
	
	
	private void activateImageButton(ImageView target, int resId) {
		target.setImageResource(resId);
		target.setClickable(true);
	}
	
	private void deactivateImageButton(ImageView target, int resId) {
		target.setImageResource(resId);
		target.setClickable(false);
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
	
	public void drawFrame(ByteBufferFrame frameData) {
		VideoProvider.frameToBitmap(frameData, bm);
		bmd = new BitmapDrawable(getResources(), bm);

		bmd.invalidateSelf();
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		iv.setBackgroundDrawable(bmd);
	}
	
	public void initBitmap(Bitmap bitmap) {
		bm = bitmap;
	}
	
	public void initBitmapDrawable(BitmapDrawable bitmapDrawable) {
		bmd = bitmapDrawable;
		bmd.setDither(false);
        bmd.setFilterBitmap(false);
	}
}
