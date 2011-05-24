package com.tmarki.comicmaker;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;

public class PackHandler {
	static public Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> getBundles () {
		Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> ret = new HashMap<CharSequence, Map<CharSequence, Vector<CharSequence>>>();
		try {
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
		    if (files == null)
		    	return ret;
		    for (File f : files) {
		    	if (!f.getName().toLowerCase().endsWith(".zip")) {
		    		continue;
		    	}
		    	Map<CharSequence, Vector<CharSequence>> imgs = new HashMap<CharSequence,Vector<CharSequence>>(); 
				ZipFile zf = new ZipFile(f.getAbsolutePath());
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
/*					  System.out.println("Read " + ze.getName());
					  long size = ze.getSize();
					  if (size > 0) {
				          mainView.addImageObject(new BitmapDrawable (BitmapFactory.decodeStream(zf.getInputStream(ze))), 0, 0, 45.0f, 0.5f, R.drawable.trollface);
					  }*/
			    }
				ret.put(zf.getName().substring(zf.getName().lastIndexOf("/") + 1), imgs);
				
		    }
		} catch (IOException e) {
/*			AlertDialog alertDialog3;
			alertDialog3 = new AlertDialog.Builder(this).create();
			alertDialog3.setTitle("Comic Packs");
			alertDialog3.setMessage(e.getStackTrace().toString());
			alertDialog3.setButton("Ok", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			alertDialog3.show();*/
		}
		
		return ret;
	}
	static public BitmapDrawable getPackDrawable (String filename, String folder, String file) {
		return getPackDrawable(filename, folder, file, 0);
	}
	static public BitmapDrawable getPackDrawable (String filename, String folder, String file, int fixedHeight) {
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

}
