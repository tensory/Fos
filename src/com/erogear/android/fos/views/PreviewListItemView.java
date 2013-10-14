package com.erogear.android.fos.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class PreviewListItemView extends RelativeLayout {
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
		Log.d("PVLIV", "pretending to run animation");
	}
}
