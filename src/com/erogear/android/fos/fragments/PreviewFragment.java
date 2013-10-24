package com.erogear.android.fos.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.MainActivity;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.views.PreviewListItemLayoutView;

public class PreviewFragment extends SherlockListFragment {
	private ArrayList<Preview> previews;
	private static int PREVIEW_NOT_SET_INDEX = -1;
	private int selectedPreviewIndex;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		selectedPreviewIndex = PreviewFragment.PREVIEW_NOT_SET_INDEX;
		previews = getArguments().getParcelableArrayList(MainActivity.PREVIEWS_DATA_TAG);
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
			}
		} 
		pvView.toggleActive();

		selectedPreviewIndex = position;
	}

	public void onClickPreview(View v) {
		// Get the clicked parent view
		PreviewListItemLayoutView pvView = (PreviewListItemLayoutView) getListView().getChildAt(selectedPreviewIndex);
		// Get the selected Preview object
		Preview selected = (Preview) getListAdapter().getItem(selectedPreviewIndex);
		
		// Provide the preview's VideoProvider to the view controller
		VideoProvider provider = ((MainActivity) getActivity()).getVideoProviderCache().get(selected.hashCode());
		pvView.toggleAnimation(provider);
		Log.d("CLICK_PREVIEW", "Currently looking at " + selected.toString());
		
	}
}