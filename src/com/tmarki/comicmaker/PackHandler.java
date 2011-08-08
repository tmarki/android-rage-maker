package com.tmarki.comicmaker;

import java.io.File;
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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;

public class PackHandler {
	
	static private final String DEFAULT_COMIC_PACK = "default rage pack";
	static private AssetManager assetMan = null;
	
	static public void setAssetManager (AssetManager am) {
		assetMan = am;
	}
	
	static public Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> getBundles (AssetManager am) {
		Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> ret = new HashMap<CharSequence, Map<CharSequence, Vector<CharSequence>>>();
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	Log.d ("RAGE", "Media mounted");
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	    	Log.d ("RAGE", "Media Read only");
	    } else {
	    	Log.d ("RAGE", "Media unavailable");
	    }
	    File dir = new File (Environment.getExternalStorageDirectory() + "/ComicMaker");
	    File[] files = dir.listFiles();
		try {
		    Map<CharSequence, Vector<CharSequence>> imgs = getZipEntries(am.open(DEFAULT_COMIC_PACK));
			ret.put(DEFAULT_COMIC_PACK, imgs);
			assetMan = am;
		} catch (IOException e) {
			Log.d ("RAGE", e.getMessage());
			e.printStackTrace();
		}
	    if (files == null)
	    	return ret;
	    for (File f : files) {
	    	if (!f.getName().toLowerCase().endsWith(".zip")) {
	    		continue;
	    	}
	    	Map<CharSequence, Vector<CharSequence>> imgs2 = getZipEntries(f.getAbsolutePath()); 
			ret.put(f.getName().substring(f.getName().lastIndexOf("/") + 1), imgs2);
			
	    }
		
		return ret;
	}
	static private Map<CharSequence, Vector<CharSequence>> getZipEntries (InputStream is) throws IOException {
    	Map<CharSequence, Vector<CharSequence>> imgs = new HashMap<CharSequence,Vector<CharSequence>>(); 
		ZipInputStream zf = new ZipInputStream(is);
		while (true) {
			  ZipEntry ze = zf.getNextEntry();
			  if (ze == null)
				  break;
			  if (ze.getName().toLowerCase().endsWith(".png") 
					  || ze.getName().toLowerCase().endsWith(".jpg"))
			  {
				  String folder = ze.getName().substring(0, ze.getName().lastIndexOf("/"));
				  String file = ze.getName().substring(ze.getName().lastIndexOf("/") + 1);
				  if (file.startsWith(".") || folder.startsWith("."))
					  continue;
				  if (!imgs.containsKey(folder)) {
					  imgs.put(folder, new Vector<CharSequence>());
				  }
				  imgs.get(folder).add(file);
				  Log.d ("RAGE", "Folder: " + folder + ", file: " + file);
			  }
	    }
		return imgs;
	}
	static private Map<CharSequence, Vector<CharSequence>> getZipEntries (String fname) {
    	Map<CharSequence, Vector<CharSequence>> imgs = new HashMap<CharSequence,Vector<CharSequence>>(); 
		ZipFile zf = null;
		try {
			zf = new ZipFile(fname);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (zf == null) return imgs;
		Enumeration entries = zf.entries();

	    
		while (entries.hasMoreElements()) {
			  ZipEntry ze = (ZipEntry) entries.nextElement();
			  if (ze.getName().toLowerCase().endsWith(".png") 
					  || ze.getName().toLowerCase().endsWith(".jpg"))
			  {
				  String folder = ze.getName().substring(0, ze.getName().lastIndexOf("/"));
				  String file = ze.getName().substring(ze.getName().lastIndexOf("/") + 1);
				  if (file.startsWith(".") || folder.startsWith("."))
					  continue;
				  if (!imgs.containsKey(folder)) {
					  imgs.put(folder, new Vector<CharSequence>());
				  }
				  imgs.get(folder).add(file);
				  Log.d ("RAGE", "Folder: " + folder + ", file: " + file);
			  }
	    }
		return imgs;
	}
	static public BitmapDrawable getPackDrawable (String filename, String folder, String file) {
		return getPackDrawable(filename, folder, file, 0);
	}
	static public BitmapDrawable getPackDrawable (String filename, String folder, String file, int fixedHeight) {
		if (filename == DEFAULT_COMIC_PACK)
			return getDefaultPackDrawable(folder, file, fixedHeight);
		ZipFile zf;
		try {
			Log.d ("RAGE", "Trying to return " + filename + "/" + folder + "/" + file);
			zf = new ZipFile(Environment.getExternalStorageDirectory() + "/ComicMaker/" + filename);
			Enumeration entries = zf.entries();
			while (entries.hasMoreElements()) {
				  ZipEntry ze = (ZipEntry) entries.nextElement();
				  if (ze.getName().toLowerCase().endsWith(".png") 
						  || ze.getName().toLowerCase().endsWith(".jpg"))
				  {
					  String sfolder = ze.getName().substring(0, ze.getName().lastIndexOf("/"));
					  String sfile = ze.getName().substring(ze.getName().lastIndexOf("/") + 1);
					  if (sfile.equals(file) && sfolder.equals(folder)) {
						  BitmapDrawable bd = new BitmapDrawable(BitmapFactory.decodeStream(zf.getInputStream(ze)));
						  if (fixedHeight > 0)
							  bd.setBounds(0, 0, fixedHeight, (fixedHeight * bd.getIntrinsicWidth()) / bd.getIntrinsicHeight());
						  else
							  bd.setBounds(0, 0, bd.getIntrinsicHeight(), bd.getIntrinsicWidth());
						  Log.d ("RAGE", "Returning " + bd.toString());
						  return bd;
					  }
				  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public BitmapDrawable getDefaultPackDrawable (String folder, String file, int fixedHeight) {
		if (assetMan == null)
			return null;
		try {
			ZipInputStream zf = new ZipInputStream(assetMan.open(DEFAULT_COMIC_PACK));
			while (true) {
				  ZipEntry ze = zf.getNextEntry();
				  if (ze == null) break;
				  if (ze.getName().toLowerCase().endsWith(".png") 
						  || ze.getName().toLowerCase().endsWith(".jpg"))
				  {
					  String sfolder = ze.getName().substring(0, ze.getName().lastIndexOf("/"));
					  if (!sfolder.equals(folder)) continue;
					  String sfile = ze.getName().substring(ze.getName().lastIndexOf("/") + 1);
					  if (sfile.equals(file)) {
						  BitmapDrawable bd = new BitmapDrawable(BitmapFactory.decodeStream(zf));
						  if (fixedHeight > 0)
							  bd.setBounds(0, 0, fixedHeight, (fixedHeight * bd.getIntrinsicWidth()) / bd.getIntrinsicHeight());
						  else
							  bd.setBounds(0, 0, bd.getIntrinsicHeight(), bd.getIntrinsicWidth());
						  Log.d ("RAGE", "Returning " + bd.toString());
						  return bd;
					  }
				  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
