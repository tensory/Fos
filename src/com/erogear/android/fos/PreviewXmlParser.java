package com.erogear.android.fos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

public class PreviewXmlParser {
	private static final String PREVIEW_TAG = "preview";
	private static final String NAME_ATTR = "name";
	private static final String ns = null;
	private Context context;
	
	public PreviewXmlParser(Context c) {
		this.context = c;
	}
	
	
	public List<Preview> parse(XmlResourceParser parser) throws XmlPullParserException, IOException {
		ArrayList<Preview> previews = new ArrayList<Preview>();
		try {
			int event = parser.getEventType();
			Preview p = null;
			
			while (event != XmlPullParser.END_DOCUMENT) {
				// get the type of the event  
	            event = parser.getEventType();
	       
	            if (event == XmlPullParser.START_TAG) {
	            	if (parser.getName().equals(PreviewXmlParser.PREVIEW_TAG)) {
	            		p = new Preview();
	            		try {
	            			p.setName(getStringResourceValue(parser.getAttributeValue(ns, NAME_ATTR)));
	            		} catch (Exception e) {
	            			p.setName(parser.getAttributeValue(ns, NAME_ATTR));
	            		}
	            	}
	            	/*
	            	if (parser.getName().equals("filename")) {
	            		p.setFilename(parser.getText());
	            	}
	            	*/
	            	
	            } else if (event == XmlPullParser.END_TAG) {
	            	if (parser.getName().equals(PreviewXmlParser.PREVIEW_TAG)) {
	            		previews.add(p);
	            	}
	            }
	            parser.next(); 
			}
		} catch (Exception e) {
			Log.e("ERROR", e.getMessage());
		} finally {
			parser.close();
		}
		return previews;
	}
	
	/**
	 * Given a string that resembles a resource name
	 * such as "@string/unique_identifier",
	 * return only the unique identifier.
	 */
	private static String getUniqueId(String in) {
		if (in.contains("/")) {
			return in.substring(in.indexOf("/") + 1);
		} 
		return in;
	}
	
	/** 
	 * Given a string resource name, return its compiled value
	 */
	private String getStringResourceValue(String identifier) throws Exception {
		int id = context.getResources().getIdentifier(PreviewXmlParser.getUniqueId(identifier), "string", context.getPackageName());
		if (id == 0) throw new Exception("Invalid string resource");

		return context.getResources().getString(id);
	}
}
