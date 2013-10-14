package com.erogear.android.fos;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

public class Preview {
	private String name;
	private String resName;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the resource name.
	 * This string will be used by /res/drawable matchers
	 * to locate images and animation XML files.
	 * @param name
	 */
	public void setResourceName(String name) {
		this.resName = name.toLowerCase(Locale.US);
	}

	public String getResourceName() {
		return this.resName;
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
