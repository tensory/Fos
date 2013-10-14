package com.erogear.android.fos.views;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.erogear.android.fos.R;

public class PreviewListItemView extends RelativeLayout {
	AnimationDrawable animation;
	
	public PreviewListItemView(Context context) {
		super(context);
	}

	public PreviewListItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PreviewListItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void runAnimation() {			
		try {
			ImageView iv = (ImageView) findViewById(R.id.ivPreview);
			animation = (AnimationDrawable) iv.getBackground();
			animation.start();
		} catch (Exception e) {
			Log.e("PVLIV", e.getMessage());
		}
	}
}
