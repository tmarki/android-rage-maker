package com.tmarki.comicmaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class PackHandler {
	
	private final String DEFAULT_COMIC_PACK = "Default pack";
	
	public Map<CharSequence, Vector<String>> getBundles (AssetManager am) {
		Map<CharSequence, Vector<String>> imgs = new HashMap<CharSequence, Vector<String>>();
		try {
			String[] nameList = am.list(DEFAULT_COMIC_PACK);
			for (String folder : nameList) {
				String[] subNameList = am.list(DEFAULT_COMIC_PACK + "/" + folder);
				Vector<String> fns = new Vector<String>();
				for (String fn : subNameList) {
					fns.add(fn);
				}
				imgs.put(folder, fns);
			}
		} catch (IOException e) {
			Log.d ("RAGE", e.getMessage());
			e.printStackTrace();
		}
		return imgs;
	}
	public Bitmap getDefaultPackDrawable (String folder, String file, int fixedHeight, AssetManager am) {
		if (am == null)
			return null;
		try {
	    	InputStream is;
			is = am.open(DEFAULT_COMIC_PACK + "/" + folder + "/" + file);
			Bitmap bmp = BitmapFactory.decodeStream(is);
			if (bmp != null) {
				if (fixedHeight > 0) {
					Bitmap tmp = bmp;
					bmp = Bitmap.createScaledBitmap(bmp, fixedHeight, bmp.getWidth() * fixedHeight / tmp.getHeight(), true);
					tmp.recycle();
				}
				return bmp;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	 public Bitmap decodeFile(File f){
	    Bitmap b = null;
	    try {
	        //Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;

	        FileInputStream fis = new FileInputStream(f);
	        BitmapFactory.decodeStream(fis, null, o);
	        fis.close();

	        int scale = 1;
	        if (o.outHeight > ImageObject.maxImageHeight || o.outWidth > ImageObject.maxImageWidth) {
	            scale = (int)Math.pow(2, (int) Math.round(Math.log(ImageObject.maxImageHeight / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
	        }

	        //Decode with inSampleSize
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize = scale;
	        fis = new FileInputStream(f);
	        b = BitmapFactory.decodeStream(fis, null, o2);
	        fis.close();
	    } catch (Exception e) {
	    }
	    return b;
	}

	static public Bitmap decodeStream(InputStream f, int size){
	    Bitmap b = null;
	    try {
	        b = BitmapFactory.decodeStream(f);
	    } catch (Exception e) {
	    	Log.d ("RAGE", e.toString ());
    	} catch (OutOfMemoryError e) {
    		Log.d ("RAGE", e.toString ());
    	}
	    return b;
	}
	
}
