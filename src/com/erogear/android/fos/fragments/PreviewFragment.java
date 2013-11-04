package com.erogear.android.fos.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erogear.android.bluetooth.video.ByteBufferFrame;
import com.erogear.android.fos.MainActivity;
import com.erogear.android.fos.Preview;
import com.erogear.android.fos.PreviewAdapter;
import com.erogear.android.fos.views.PreviewListItemLayoutView;

public class PreviewFragment extends SherlockListFragment {
	public static String FRAGMENT_TAG = "PREVIEWS";
	public static int PREVIEW_NOT_SET_INDEX = -1;
	private ArrayList<Preview> previews;
	private int selectedPreviewIndex;
	private static final String TAG = "PF";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		selectedPreviewIndex = PreviewFragment.PREVIEW_NOT_SET_INDEX;
		previews = getArguments().getParcelableArrayList(MainActivity.PREVIEWS_DATA_TAG);
		setListAdapter(new PreviewAdapter(getActivity(), previews));
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (selectedPreviewIndex != PreviewFragment.PREVIEW_NOT_SET_INDEX) {
			((MainActivity) getActivity()).setSelectedPreview(selectedPreviewIndex);
		}
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		if (selectedPreviewIndex == position) {
			selectedPreviewIndex = PreviewFragment.PREVIEW_NOT_SET_INDEX;
		} else {
			selectedPreviewIndex = position;
		}
		((MainActivity) getActivity()).setSelectedPreview(selectedPreviewIndex);
	}

	public Preview getPreviewAt(int index) {
		return (Preview) getListAdapter().getItem(index);
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
    }
    
    /**
     * Receives a ByteBufferFrame
     * from the BluetoothVideoService callback handler in MainActivity,
     * and pushes it to the currently selected Preview's layout view 
     * to be drawn in the background.
     * 
     * @param frame
     */
    public void drawFrameInCurrentPreview(Object frame) {
    	if (selectedPreviewIndex == PreviewFragment.PREVIEW_NOT_SET_INDEX) {
    		Log.e(PreviewFragment.TAG, "Can't draw a frame for an unselected preview");
    	}
    	
    	PreviewListItemLayoutView layout = (PreviewListItemLayoutView) getListView().getChildAt(selectedPreviewIndex);
    	layout.drawFrame((ByteBufferFrame) frame);
    }
    
    public void setSelectedItem(int index) {
    	selectedPreviewIndex = index;
    }
    
    public void activateItem(int index) {
    	PreviewListItemLayoutView layout = (PreviewListItemLayoutView) getListView().getChildAt(index);
    	layout.activate();
    }

    public void deactivateItem(int index) {
    	PreviewListItemLayoutView layout = (PreviewListItemLayoutView) getListView().getChildAt(index);
    	layout.deactivate();
    }
}