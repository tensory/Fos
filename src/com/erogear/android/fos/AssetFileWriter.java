package com.erogear.android.fos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;

public class AssetFileWriter {
	Context ctx;
	
	public AssetFileWriter(Context context) {
		ctx = context;
	}
	
	/**
	 * Given an asset filename, write a temporary file with its data.
	 * Return a handle to the file.
	 * 
	 * @param filename
	 * @return File
	 * @throws RuntimeException
	 */
	public File fileFromAsset(String filename) throws RuntimeException {
		File f = new File(ctx.getFilesDir(), filename);
		if (!f.exists()) {
			try {
				InputStream is = ctx.getAssets().open(filename);
				int size = is.available();
				byte[] buffer = new byte[size];
				is.read(buffer);
				is.close();
				
				FileOutputStream fos = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
				fos.write(buffer);
				fos.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		return f;
	}
}
