package com.erogear.android.fos;
import java.io.File;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.erogear.android.fos.views.PreviewListItemLayoutView;

public class PreviewAdapter extends ArrayAdapter<Preview> {
	private int previewItemHeight;
	
	public PreviewAdapter(Context context, List<Preview> previews) {
		super(context, 0, previews);
		previewItemHeight = ((MainActivity) context).getPreviewItemHeight();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		PreviewListItemLayoutView view = (PreviewListItemLayoutView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	view = (PreviewListItemLayoutView) inflater.inflate(R.layout.preview_list_item, null);
		}

		Preview p = getItem(position);
		TextView title = (TextView) view.findViewById(R.id.tvAnimTitle);
		title.setText(p.getName());
		ImageView previewImage = (ImageView) view.findViewById(R.id.ivPreview);
		
		// Fetch background image
		String pvImagePath = new File(getContext().getFilesDir(), p.getResourceName() + Preview.IMAGE_EXTENSION).getPath();
		Bitmap bitmap = BitmapFactory.decodeFile(pvImagePath);
		BitmapDrawable btmDw = getBitmapDrawable(getContext(), bitmap);
		
		// Set the bitmap as the view's default bitmap
		view.initBitmap(bitmap);
		view.initBitmapDrawable(btmDw);
		
		// Set it as background
		PreviewAdapter.setBackgroundImage(previewImage, btmDw);
		
		return view;
	}
	
	// Multi-API level targeting because view.setBackgroundDrawable is deprecated
	// http://stackoverflow.com/questions/18806709/how-to-set-a-bitmap-to-background-for-a-view-android-api-10-18
	@TargetApi(16)
	private static void setBackgroundV16Plus(View view, BitmapDrawable bmd) {
		view.setBackground(bmd);
	}
	
	@SuppressWarnings("deprecation")
	private static void setBackgroundV16Minus(View view, BitmapDrawable bmd) {
		view.setBackgroundDrawable(bmd);
	}
	
	public static void setBackgroundImage(View view, BitmapDrawable bmd) {
		if (android.os.Build.VERSION.SDK_INT >= 16){
			setBackgroundV16Plus(view, bmd);
		} else {
			setBackgroundV16Minus(view, bmd);
		}
	}
	
	private BitmapDrawable getBitmapDrawable(Context context, Bitmap bmp) {
		return new BitmapDrawable(context.getResources(), bmp);
	}
}
