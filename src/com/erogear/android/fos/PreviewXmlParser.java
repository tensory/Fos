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
	private static final String FILENAME_TAG = "filename";
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
			Preview preview = null;
			
			while (event != XmlPullParser.END_DOCUMENT) {
				// get the type of the event  
	            event = parser.getEventType();
	       
	            if (event == XmlPullParser.START_TAG) {
	            	if (parser.getName().equals(PreviewXmlParser.PREVIEW_TAG)) {
	            		preview = new Preview();
	            		String n;
	            		
	            		try {
	            			n = getStringResourceValue(parser.getAttributeValue(ns, NAME_ATTR));
	            		} catch (Exception e) {
	            			n = parser.getAttributeValue(ns, NAME_ATTR);
	            		}
            			preview.setName(n);
	            	}
	            	
	            	if (parser.getName().equals(PreviewXmlParser.FILENAME_TAG)) {
	            		parser.next();
	            		if (parser.getEventType() == XmlPullParser.TEXT) {
		            		String filename = parser.getText();
		            		preview.setFilename(filename);
		            		preview.setResourceName(filename);
	            		}
	            	}
	            	
	            } else if (event == XmlPullParser.END_TAG) {
	            	if (parser.getName().equals(PreviewXmlParser.PREVIEW_TAG)) {
	            		if (preview.getFilename() == null) {
	            			// Previews must have a filename specified
	            			throw new Exception("Invalid preview: no filename was set");
	            		}
	            		previews.add(preview);
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
