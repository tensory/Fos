package com.erogear.android.fos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.util.Log;

import com.erogear.android.bluetooth.video.VideoProvider;

public class Preview {
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
	private String resName; // TODO: remove?
	
	private VideoProvider provider;
	private boolean videoLoaded;
	
	
	
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

	/**
	 * Sets the resource name.
	 * This string will be used by /res/drawable matchers
	 * to locate images and animation XML files.
	 * @param name
	 */
	public void setResourceName(String filename) {
		String truncated = filename.substring(0, filename.lastIndexOf('.'));
		this.resName = truncated.toLowerCase(Locale.US);
	}

	public String getResourceName() {
		return this.resName;
	}
	
	/**
	 * Get the resource ID of the animation XML file in /res/drawable
	 * From http://daniel-codes.blogspot.com/2009/12/dynamically-retrieving-resources-in.html
	 * 
	 * @return int
	 */
	public int getDrawableResourceId() {
		try {
			Class res = R.drawable.class;
			Field field = res.getField(this.resName);
			return field.getInt(null);
		} catch (Exception e) {
			return 0;
		}
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
	public void confirmPreviewBitmapReady(Context context) throws Exception {
		File f = new File(context.getFilesDir(), this.getResourceName() + Preview.IMAGE_EXTENSION);
		if (!f.exists()) {
			// Image has not been created
			// Has the video file been loaded?
			if (videoLoaded) {
				// If video has been loaded, extract a frame and save it
				Bitmap bmp = FrameExtractor.getFrameBitmap(provider, 0);
				try {
					saveThumbnail(f, bmp); 					
				} catch (Exception e) {
					throw e;
				}

			} else {
				Log.e(Preview.TAG, "No video loaded");
				
			}
			// Do a final check on the new file
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
}
