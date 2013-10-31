package com.erogear.android.fos;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;


public class PreviewLoader {
	public static final int UNSET_INDEX = -1;
	private Preview preview;
	private VideoProvider provider;
	private int listIndex;
	
	public PreviewLoader() {
		listIndex = UNSET_INDEX;
	}
	
	public PreviewLoader(int index) {
		listIndex = index;
	}
	
	public void attachPreview(Preview p) {
		preview = p;
	}
	
	public Preview getPreview() {
		return preview;
	}
	
	public void setVideoProvider(VideoProvider vp) {
		provider = vp;
	}
	
	public VideoProvider getVideoProvider() {
		return provider;
	}
	
	public boolean hasListIndex() {
		return (listIndex != PreviewLoader.UNSET_INDEX);
	}
	
	public void setListIndex(int index) {
		listIndex = index;
	}
	
	public int getListIndex() {
		return listIndex;
	}
}
