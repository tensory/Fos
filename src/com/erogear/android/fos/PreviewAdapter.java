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
import android.widget.TextView;

import com.erogear.android.fos.views.PreviewListItemLayoutView;

public class PreviewAdapter extends ArrayAdapter<Preview> {
	//Context context;
	
	public PreviewAdapter(Context context, List<Preview> previews) {
		super(context, 0, previews);
	//	this.context = context;
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
		// Set it as background
		if (android.os.Build.VERSION.SDK_INT >= 16){
            setBackgroundV16Plus(previewImage, bitmap);
        } else {
            setBackgroundV16Minus(previewImage, bitmap);
        }
		
		return view;
	}
	
	// Multi-API level targeting because setBackgroundDrawable is deprecated
	// http://stackoverflow.com/questions/18806709/how-to-set-a-bitmap-to-background-for-a-view-android-api-10-18
	@TargetApi(16)
	private void setBackgroundV16Plus(View view, Bitmap bmp) {
		view.setBackground(new BitmapDrawable(getContext().getResources(), bmp));
	}
	
	@TargetApi(16)
	private void setBackgroundV16Minus(View view, Bitmap bmp) {
		view.setBackgroundDrawable(new BitmapDrawable(bmp));
	}
}
