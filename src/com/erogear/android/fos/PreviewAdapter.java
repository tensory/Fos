package com.erogear.android.fos;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PreviewAdapter extends ArrayAdapter<Preview> {
	Context context;
	
	public PreviewAdapter(Context context, List<Preview> previews) {
		super(context, 0, previews);
		this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	view = inflater.inflate(R.layout.preview_item, null);
		}
		Preview p = getItem(position);
		
		TextView tvAnimationName = (TextView) view.findViewById(R.id.tvAnimationName);
		tvAnimationName.setText(p.getName());
		
		return view;
	}	
}
