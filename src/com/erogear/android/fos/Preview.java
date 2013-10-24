package com.erogear.android.fos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.erogear.android.bluetooth.video.VideoProvider;

public class Preview implements Parcelable {
	public static class FrameExtractor {
		// TODO these cannot be set here, must be derived from video
		private static int BITMAP_WIDTH = 32;
		private static int BITMAP_HEIGHT = 8;
		/**
		 * Grab a frame from a video provider with loaded video.
		 * @param videoProvider
		 * @param position the requested frame index
		 * @return frame as a bitmap
		 */
		public static Bitmap getFrameBitmap(VideoProvider provider, int position) {
			Bitmap bmp = Bitmap.createBitmap(FrameExtractor.BITMAP_WIDTH, FrameExtractor.BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
			VideoProvider.frameToBitmap(provider.getFrame(position), bmp);
			return bmp;
		}
	}
	
	public static String IMAGE_EXTENSION = ".png";
	private static String TAG = "PREVIEW";
	private String name;
	private String filename;
	private VideoProvider provider;
	private boolean videoLoaded;
	
	public Preview(Parcel in) {
		name = in.readString();
		filename = in.readString();
		videoLoaded = (in.readByte() == 1);
		// provider is not rehydrated because serialization is hard
	}
	
	public Preview() {
		// At creation time, video has not been loaded.
		videoLoaded = false;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setFilename(String name) {
		this.filename = name;
	}
	
	public String getFilename() {
		return this.filename;
	}

	public String getResourceName() {
		return this.filename.substring(0, filename.lastIndexOf('.'));
	}
	
	public static ArrayList<Preview> getAll(Context context, XmlResourceParser parser) {
		ArrayList<Preview> prevs = new ArrayList<Preview>();
		
		try {
			PreviewXmlParser prevXmlParser = new PreviewXmlParser(context);
			prevs = (ArrayList<Preview>) prevXmlParser.parse(parser);
		} catch (Exception e) {
			Log.e("FILE", "Not reading");
			Log.e("FILE", e.getMessage());
		}
		return prevs;
	}
	
	/**
	 * Confirm that this Preview has a bitmap file 
	 * ready to use as a preview image
	 * @throws Exception 
	 */
	public void confirmPreviewBitmapReady(Context context) {
		String imageFilename = this.getResourceName() + Preview.IMAGE_EXTENSION;
		File f = new File(context.getFilesDir(), imageFilename);
		if (!f.exists()) {
			// Image has not been created
			// Has the video file been loaded?
			if (videoLoaded) {
				// If video has been loaded, extract a frame and save it
				Bitmap bmp = FrameExtractor.getFrameBitmap(provider, 0);
				try {
					saveThumbnail(f, bmp); 					
				} catch (Exception e) {
					Log.e(Preview.TAG, "Could not save thumbnail: " + e.getMessage());
				}

			} else {
				Log.e(Preview.TAG, "No video loaded");
				
			}
		}
		// Do a final check on the new file
		f = new File(context.getFilesDir(), imageFilename);
		if (!f.exists()) {
			Log.e(Preview.TAG, this.name + ": No image preview found");
		}		
	}
	
	private void saveThumbnail(File file, Bitmap bitmap) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(file);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		fos.close();
	}
	
	/**
	 * @return string the filetype of the preview's video file
	 */
	public String getVideoFileType() {
		// Currently always AVI
		return "AVI";
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public void setVideoProvider(VideoProvider vp) {
        provider = vp;
	}
	
	public File saveVideoFileAsset(Context context) throws FileNotFoundException, IOException {
		File videoFile = new File(context.getFilesDir(), this.getFilename());
		if (!videoFile.exists()) {
			InputStream is = context.getAssets().open(this.getFilename());
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			
			FileOutputStream fos = context.openFileOutput(this.getFilename(), Context.MODE_PRIVATE);
			fos.write(buffer);
			fos.close();
		}
		return videoFile;
	}
	
	public void setVideoLoaded(boolean isLoaded) {
		videoLoaded = isLoaded;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(filename);
		dest.writeByte((byte)(videoLoaded ? 1 : 0));
	};
}
