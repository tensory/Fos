package com.erogear.android.fos;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;


public class PreviewLoader {
	public static final int UNSET_INDEX = -1;
	public static final String PREVIEW_SELECTED_TAG = "SELECTED";
	private Preview preview;
	private VideoProvider provider;
	private int listIndex;
	private boolean playing;
	
	public PreviewLoader() {
		listIndex = UNSET_INDEX;
		playing = false;
	}
	
	public PreviewLoader(int index) {
		listIndex = index;
		playing = false;
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
	
	public void setPlaying(boolean shouldBePlaying) {
		playing = shouldBePlaying;
	}
	
	public boolean isPlaying() {
		return playing;
	}
}
