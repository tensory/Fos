package com.erogear.android.fos;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;


public class PreviewLoader {
	private static final int UNSET_INDEX = -1;
	private Preview preview;
	private VideoProvider provider;
	private int listIndex;
	private boolean selected, playing;
	
	public PreviewLoader() {
		listIndex = PreviewLoader.UNSET_INDEX;
		selected = false;
		playing = false;
	}
	
	public PreviewLoader(Preview preview, VideoProvider videoProvider, int index) {
		this.preview = preview;
		provider = videoProvider;
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
	
	public int getListIndex() {
		return listIndex;
	}
	
	public void setListIndex(int idx) {
		listIndex = idx;
	}
	
	public void setHighlighted(boolean isSelected) {
		selected = isSelected;
	}
	
	public void setPlaying(boolean isPlaying) {
		playing = isPlaying;
	}
}
