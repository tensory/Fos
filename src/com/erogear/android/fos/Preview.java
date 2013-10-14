package com.erogear.android.fos;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

public class Preview {
	private String name;
	private String filename;
	private String resName;
	
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
	
	@Override
	public String toString() {
		return this.name;
	}
}
