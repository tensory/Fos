package com.erogear.android.fos.fragments;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.R;

public class PreviewFragment extends SherlockListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Context activity = getActivity();
		ArrayList<Preview> previews = Preview.getAll(activity, activity.getResources().getXml(R.xml.previews));
		setListAdapter(new PreviewAdapter(getActivity(), previews));
	}
}