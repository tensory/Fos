package com.erogear.android.fos;

import java.util.ArrayList;
import android.os.Bundle;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;

public class MainActivity extends SherlockActivity {
	ListView lv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
	}
	
	protected void setupViews() {
		lv = (ListView) findViewById(R.id.lvAnimations);
		ArrayList<Preview> previews = Preview.getAll(getApplicationContext(), getResources().getXml(R.xml.previews));
		PreviewAdapter animAdapter = new PreviewAdapter(getApplicationContext(), previews);
		lv.setAdapter(animAdapter);
	}
}