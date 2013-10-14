package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.fos.R;

public class PreviewListItemView extends RelativeLayout {
	private boolean isRunning = false;
	
	public PreviewListItemView(Context context) {
		super(context);
	}

	public PreviewListItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PreviewListItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void toggleAnimation() {
		ImageView iv = (ImageView) findViewById(R.id.ivPreview);
		AnimationDrawable animation = (AnimationDrawable) iv.getBackground();
		if (!isRunning) {
			animation.start();
		} else {
			animation.stop();
			animation.selectDrawable(0);
		}
		isRunning = !isRunning;
	}
}
