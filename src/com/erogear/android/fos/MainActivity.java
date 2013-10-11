package com.erogear.android.fos;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;

public class MainActivity extends SherlockActivity {
	ListView lvAnimations;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
	}
	
	protected void setupViews() {
		lvAnimations = (ListView) findViewById(R.id.lvAnimations);
		ArrayList<Preview> previews = new ArrayList<Preview>();
		XmlResourceParser parser = getResources().getXml(R.xml.previews);
		Preview p = null;
		
		try {
			int event = parser.getEventType();
			String TAG_PREVIEW = "preview";
			
			while (event != XmlPullParser.END_DOCUMENT) {
				// get the type of the event  
	            event = parser.getEventType();
	       
	            if (event == XmlPullParser.START_TAG) {
	            	if (parser.getName().equals(TAG_PREVIEW)) {
	            		p = new Preview();
	            		String element = parser.getAttributeValue(null, "name");
	            		
	            		int id = getResources().getIdentifier("txtAnimName_Heartbeat", "string", getPackageName());
	            		p.setName(getResources().getString(id));
	            	}
	            	/*
	            	if (parser.getName().equals("filename")) {
	            		p.setFilename(parser.getText());
	            	}
	            	*/
	            	
	            } else if (event == XmlPullParser.END_TAG) {
	            	if (parser.getName().equals(TAG_PREVIEW)) {
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
		
		for (Preview prev : previews) {
			Log.d("PREVIEWS_CONTENTS", prev.toString());			
		}

/*
		PreviewAdapter prevAdapter = PreviewAdapter.getInstance();
		prevAdapter.loadAll();
		lvAnimations.setAdapter(prevAdapter);
		*/
	}
	
	/* stick this onto the xml parser */
	public static String getStringName(String in) throws Exception {
		if (in.contains("/")) {
			return in.substring(in.indexOf("/") + 1);
		} 
		return in;
	}
}