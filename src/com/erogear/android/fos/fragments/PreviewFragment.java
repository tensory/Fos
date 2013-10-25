package com.erogear.android.fos.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erogear.android.bluetooth.video.FrameController;
import com.erogear.android.bluetooth.video.MultiheadController;
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

	public Preview getSelectedPreview() {
		return (Preview) getListAdapter().getItem(selectedPreviewIndex);
	}
	
	// DEPRECATED
	public void onClickPreview(View v) {
		// Get the selected Preview object
		//Preview selected = (Preview) getListAdapter().getItem(selectedPreviewIndex);
		
		//((MainActivity) getActivity()).togglePreviewVideo(selected, selectedPreviewIndex);
		
		/*
	
		VideoProvider provider = ((MainActivity) getActivity()).getVideoProviderCache().get(selected.hashCode());
		FrameController<VideoProvider, MultiheadController> controller = 
				new FrameController<VideoProvider, MultiheadController>(
						provider,
						
						);
		
		public void toggleVideo(Preview preview, VideoProvider provider) {
			VideoProvider videoProvider = context.
	        controller = new FrameController<VideoProvider, MultiheadController>(provider, headController, videoSvc);
	        videoSvc.setConfigInstance(FrameController.CONFIG_INSTANCE_KEY, controller);

			
		}
		 /*
		
		// Get the clicked parent view
		PreviewListItemLayoutView pvView = (PreviewListItemLayoutView) getListView().getChildAt(selectedPreviewIndex);
		
		// Provide the preview's VideoProvider to the view controller
		try {
			VideoProvider provider = ((MainActivity) getActivity()).getVideoProviderCache().get(selected.hashCode());
			pvView.toggleAnimation(provider);
			Log.d("CLICK_PREVIEW", "Currently looking at " + selected.toString());
		} catch (Exception e) {
			Log.e("PLILV", e.getMessage());
		}
		*/
	}
	
	/**
	 * Reset the heights of all list items dynamically
	 * when the device is rotated
	 * to preserve visible ratio.
	 */
    public void redrawPreviewItems(int containerWidth, double ratio) {
    	int height = (int) (containerWidth * ratio);
    	
    	ListView list = getListView();
    	for (int i = 0; i < list.getCount(); i++) {
    		PreviewListItemLayoutView layout = (PreviewListItemLayoutView) list.getChildAt(i);
    		layout.setLayoutHeight(height);
    	}
    	
    	Log.d("PF", "Changing height ratio");
    }
}