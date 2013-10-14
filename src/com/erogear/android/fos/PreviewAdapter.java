package com.erogear.android.fos;
import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.erogear.android.fos.views.PreviewListItemView;

public class PreviewAdapter extends ArrayAdapter<Preview> {
	Context context;
	
	public PreviewAdapter(Context context, List<Preview> previews) {
		super(context, 0, previews);
		this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		PreviewListItemView view = (PreviewListItemView) convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	view = (PreviewListItemView) inflater.inflate(R.layout.preview_list_item, null);
		}
		
		Preview p = getItem(position);
		TextView title = (TextView) view.findViewById(R.id.tvAnimTitle);
		title.setText(p.getName());
		ImageView previewImage = (ImageView) view.findViewById(R.id.ivPreview);
		Log.d("RES", String.valueOf(R.drawable.heartbeat));
		Log.d("RES", String.valueOf(p.getDrawableResourceId()));
		
		previewImage.setBackgroundResource(p.getDrawableResourceId());
		
		return view;
	}
	
	
}
