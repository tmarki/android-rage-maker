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
	
	private final String DEFAULT_COMIC_PACK = "default rage pack";
//	static private AssetManager assetMan = null;
	private Map<CharSequence, Map<CharSequence, Map<CharSequence, Bitmap>>> bitmapCache = new HashMap<CharSequence, Map<CharSequence, Map<CharSequence, Bitmap>>>();
	private Map<CharSequence, Bitmap> realFileCache = new HashMap<CharSequence, Bitmap>();
	
/*	static public void setAssetManager (AssetManager am) {
		assetMan = am;
	}*/
	
	public Map<CharSequence, Map<CharSequence, Vector<String>>> getBundles (AssetManager am) {
		Map<CharSequence, Map<CharSequence, Vector<String>>> ret = new HashMap<CharSequence, Map<CharSequence, Vector<String>>>();
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
//	    	Log.d ("RAGE", "Media mounted");
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//	    	Log.d ("RAGE", "Media Read only");
	    } else {
//	    	Log.d ("RAGE", "Media unavailable");
	    }
	    File dir = new File (Environment.getExternalStorageDirectory() + "/ComicMaker");
	    File[] files = dir.listFiles();
		try {
		    Map<CharSequence, Vector<String>> imgs = getZipEntries(am.open(DEFAULT_COMIC_PACK));
			ret.put(DEFAULT_COMIC_PACK, imgs);
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
	    	Map<CharSequence, Vector<String>> imgs2 = getZipEntries(f.getAbsolutePath()); 
			ret.put(f.getName().substring(f.getName().lastIndexOf("/") + 1), imgs2);
			
	    }
		
		return ret;
	}
	private Map<CharSequence, Vector<String>> getZipEntries (InputStream is) throws IOException {
    	Map<CharSequence, Vector<String>> imgs = new HashMap<CharSequence,Vector<String>>(); 
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
					  imgs.put(folder, new Vector<String>());
				  }
				  imgs.get(folder).add(file);
			  }
	    }
		return imgs;
	}
	@SuppressWarnings("rawtypes")
	private Map<CharSequence, Vector<String>> getZipEntries (String fname) {
    	Map<CharSequence, Vector<String>> imgs = new HashMap<CharSequence,Vector<String>>(); 
		ZipFile zf = null;
		try {
			zf = new ZipFile(fname);
		} catch (IOException e) {
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
					  imgs.put(folder, new Vector<String>());
				  }
				  imgs.get(folder).add(file);
			  }
	    }
		return imgs;
	}
	public Bitmap getPackBitmap (String filename, String folder, String file, AssetManager am) {
		return getPackBitmap(filename, folder, file, 0, am);
	}
	@SuppressWarnings("rawtypes")
	public Bitmap getPackBitmap (String filename, String folder, String file, int fixedHeight, AssetManager am) {
		if (bitmapCache.containsKey(filename) && bitmapCache.get(filename).containsKey(folder) 
				&& bitmapCache.get(filename).get(folder).containsKey(file) && bitmapCache.get(filename).get(folder).get(file) != null) {
			return bitmapCache.get(filename).get(folder).get(file);
		}
		if (filename == DEFAULT_COMIC_PACK)
			return getDefaultPackDrawable(folder, file, fixedHeight, am);
		ZipFile zf;
		try {
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
						  Bitmap b = decodeStream (zf.getInputStream(ze), (int)ze.getSize ());
						  saveBitmapCache (filename, folder, file, b);
						  return b;
					  }
				  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Bitmap getDefaultPackDrawable (String folder, String file, int fixedHeight, AssetManager am) {
		if (am == null)
			return null;
		try {
			ZipInputStream zf = new ZipInputStream(am.open(DEFAULT_COMIC_PACK));
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
						  Bitmap b = decodeStream (zf, (int)ze.getSize());
						  saveBitmapCache (DEFAULT_COMIC_PACK, folder, file, b);
						  return b;
					  }
				  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	void saveBitmapCache (String filename, String folder, String file, Bitmap b) {
		if (!bitmapCache.containsKey(filename)) {
			bitmapCache.put(filename, new HashMap<CharSequence, Map<CharSequence,Bitmap>>());
		}
		if (!bitmapCache.get(filename).containsKey(folder)) {
			bitmapCache.get(filename).put(folder, new HashMap<CharSequence, Bitmap> ());
		}
		bitmapCache.get(filename).get(folder).put (file, b);
	}
	 public Bitmap decodeFile(File f){
		if (realFileCache.containsKey(f.getAbsolutePath()) && realFileCache.get(f.getAbsolutePath()) != null)
			return realFileCache.get(f.getAbsolutePath());
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
	        realFileCache.put(f.getAbsolutePath(), b);
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
	
	public void freeCache (CharSequence pack, CharSequence folder) {
		if (!bitmapCache.containsKey(pack) || !bitmapCache.get(pack).containsKey(folder))
			return;
		for (CharSequence i : bitmapCache.get(pack).get(folder).keySet()) {
			bitmapCache.get(pack).get(folder).get (i).recycle();
		}
		bitmapCache.get(pack).get(folder).clear();
		bitmapCache.get(pack).clear();
	}

	public void freeAllCache () {
		for (CharSequence p : bitmapCache.keySet()) {
			for (CharSequence f : bitmapCache.get(p).keySet()) {
				freeCache (p, f);
			}
		}
		for (CharSequence p : realFileCache.keySet()) {
			realFileCache.get (p).recycle();
		}
		realFileCache.clear ();
	}
}
