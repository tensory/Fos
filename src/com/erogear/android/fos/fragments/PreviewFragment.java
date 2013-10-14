package com.erogear.android.fos.fragments;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.R;
import com.erogear.android.fos.views.PreviewListItemView;

public class PreviewFragment extends SherlockListFragment {
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Context activity = getActivity();
		ArrayList<Preview> previews = Preview.getAll(activity, activity.getResources().getXml(R.xml.previews));
		setListAdapter(new PreviewAdapter(getActivity(), previews));
	}
	
	@Override
    public void onListItemClick(ListView l, View view, int position, long id) {
		PreviewListItemView pvView = (PreviewListItemView) view;
		try {
			pvView.toggleAnimation();			
		} catch (Exception e) {
			Log.e("PreviewListItemView", "Cannot start background animation: " + e.getMessage());
		} 
    }
}