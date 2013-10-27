package com.erogear.android.fos;
import com.erogear.android.bluetooth.video.VideoProvider;
import com.erogear.android.fos.Preview;


public class PreviewLoader {
	private Preview preview;
	private VideoProvider provider;
	
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
}
