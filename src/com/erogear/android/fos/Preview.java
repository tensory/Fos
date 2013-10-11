package com.erogear.android.fos;

import java.util.ArrayList;

import android.content.res.XmlResourceParser;
import android.util.Log;

public class Preview {
	private String name;
	
	public void setName(String n) {
		this.name = n;
	}
	
	public String getName() {
		return this.name;
	}
	
	public static ArrayList<Preview> getAll(XmlResourceParser res) {
		ArrayList<Preview> prevs = new ArrayList<Preview>();
		
		try {
			PreviewXmlParser prevXmlParser = new PreviewXmlParser();
			prevs = (ArrayList<Preview>) prevXmlParser.parse(res);
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
