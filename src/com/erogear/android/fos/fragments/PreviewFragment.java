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
import com.erogear.android.fos.views.PreviewListItemLayoutView;

public class PreviewFragment extends SherlockListFragment {
	private static int PREVIEW_NOT_SET_INDEX = -1;
	private int selectedPreviewIndex;	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		selectedPreviewIndex = PreviewFragment.PREVIEW_NOT_SET_INDEX;
		Context activity = getActivity();
		ArrayList<Preview> previews = Preview.getAll(activity, activity.getResources().getXml(R.xml.previews));
		setListAdapter(new PreviewAdapter(getActivity(), previews));
	}
	
	@Override
    public void onListItemClick(ListView l, View view, int position, long id) {
		int lastSelectedIndex = selectedPreviewIndex;
		PreviewListItemLayoutView pvView = (PreviewListItemLayoutView) view;
		if (lastSelectedIndex > PreviewFragment.PREVIEW_NOT_SET_INDEX) {
			// A preview was selected before this event fired
			
			if (lastSelectedIndex != position) {
				// New selection.
				// Stop the animation on the last active preview, 
				// and start it on the current one.
				
				PreviewListItemLayoutView oldView = (PreviewListItemLayoutView) l.getChildAt(lastSelectedIndex);
				oldView.deactivate();
				oldView.stopAnimation();
			}
		} 
		pvView.toggleActive();
		pvView.toggleAnimation();
		
		selectedPreviewIndex = pvView.getIsActive() ? position : PreviewFragment.PREVIEW_NOT_SET_INDEX;
    }
	
	/**
	 * Get the Preview object at the currently activated list position.
	 * @return Preview
	 */
	public Preview getSelectedPreview() {
		return (Preview) getListAdapter().getItem(selectedPreviewIndex);
	}
	
}