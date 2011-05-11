package com.example.blahblah;

import java.io.BufferedInputStream;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.CharBuffer;
import java.util.Currency;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestResult;

import com.example.blahblah.BlahView.TouchModes;
import com.example.blahblah.ColorPickerDialog.OnColorChangedListener;
import com.example.blahblah.Picker.OnWidthChangedListener;
import com.example.blahblah.ColorPickerDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.RemoteViews.ActionException;

import com.example.blahblah.BlahView;
import com.example.blahblah.Picker;;




public class BlahGame extends Activity implements ColorPickerDialog.OnColorChangedListener, OnWidthChangedListener {
	private BlahView mainView;
	private Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> externalImages = new HashMap<CharSequence, Map<CharSequence, Vector<CharSequence>>>();
	private CharSequence packSelected;
	private CharSequence folderSelected;
	
	void readExternalFiles(){
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
		    	return;
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
				externalImages.put(zf.getName().substring(zf.getName().lastIndexOf("/") + 1), imgs);
				
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		Log.d ("RAGE", "BlahGame created");
	    Log.d ("RAGE", "Dir: " + Environment.getExternalStorageDirectory());
        super.onCreate(savedInstanceState);
        mainView = new BlahView (this);
        registerForContextMenu(mainView);
        setContentView(mainView);
        readExternalFiles();
        int ioCount = 0;
        if (savedInstanceState != null)
        	ioCount = savedInstanceState.getInt("imageObjectCount", 0);
        if (ioCount == 0) {
        	Drawable dr = getResources().getDrawable(R.drawable.trollface);
            mainView.addImageObject(dr, 0, 0, 45.0f, 0.5f, R.drawable.trollface);
        	dr = getResources().getDrawable(R.drawable.icon);
            mainView.addImageObject(dr, 300, 300, 0.0f, 2.5f, R.drawable.icon);
        }
        for (int i = 0; i < ioCount; ++i) {
        	int[] params = savedInstanceState.getIntArray(String.format("ImageObject%dpos", i));
        	float rot = savedInstanceState.getFloat(String.format("ImageObject%drot", i));
        	float sc = savedInstanceState.getFloat(String.format("ImageObject%dscale", i));
        	int rid = savedInstanceState.getInt(String.format("ImageObject%drid", i));
        	if (rid > 0) {
        		Drawable dr = getResources().getDrawable(rid);
        		mainView.addImageObject(dr, params[0], params[1], rot, sc, rid);
        	}
/*        	outState.putIntArray(String.format("ImageObject%dpos", i), params);
        	outState.putDouble(String.format("ImageObject%drot", i), ios.get(i).getRotation());
        	outState.putDouble(String.format("ImageObject%dscale", i), ios.get (i).getScale ());*/
        }
        mainView.addTextObject(100, 80, 20, Color.RED, Typeface.SANS_SERIF, "asdfghjkl", true, true);
        mainView.addTextObject(100, 120, 20, Color.GREEN, Typeface.SERIF, "zxc\n\rvbnm\ncy,", false, true);
        mainView.addTextObject(100, 160, 30, Color.BLUE, Typeface.MONOSPACE, "qwertyuiop", true, true);
//        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
/*		SubMenu sm = menu.addSubMenu("Add image");
//	    SubMenu ip = sm.addSubMenu("From image pack");
        for (CharSequence s : externalImages.keySet()) {
/*        	if (ip != null)
        		ip.add(s);
	    }
	    sm.add("From external source");*/
	    Log.d ("RAGE", "Main menu");
	    return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		ImageObject io = mainView.getSelected();
		if (menu.size() == 0 && io != null) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.edit_menu, menu);
			menu.findItem(R.id.tofront).setVisible(io.isInBack());
			menu.findItem(R.id.toback).setVisible(!io.isInBack());
			mainView.resetClick();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ImageObject io = mainView.getSelected();
		if (item.getItemId() == R.id.mode_hand)
			mainView.setmTouchMode(TouchModes.HAND);
		else if (item.getItemId() == R.id.mode_pencil)
			mainView.setmTouchMode(TouchModes.PENCIL);
		else if (item.getItemId() == R.id.mode_text)
			mainView.setmTouchMode(TouchModes.TEXT);
		else if (item.getItemId() == R.id.toback && io != null)
			io.setInBack(true);
		else if (item.getItemId() == R.id.tofront && io != null)
			io.setInBack(false);
		mainView.invalidate();
		Log.d ("RAGE", item.toString());
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d ("RAGE", item.toString());
		switch (item.getItemId())
		{
		case R.id.about:
			AlertDialog alertDialog;
			alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("About Comic Maker");
			alertDialog.setMessage("Comic Maker for Android 1.0 by Tamas\n(c) 2011");
			alertDialog.setButton("Close", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			alertDialog.show();
			break;
		case (R.id.pen_color):
		case (R.id.text_color):
			ColorPickerDialog cpd = new ColorPickerDialog(mainView.getContext(), this, mainView.getCurrentColor());
			cpd.show();
			break;
		case (R.id.pen_width):
			Picker np = new Picker (mainView.getContext(), this, mainView.getCurrentStrokeWidth());
			np.show();
			break;
		case (R.id.clear):
			AlertDialog alertDialog2;
			alertDialog2 = new AlertDialog.Builder(this).create();
			alertDialog2.setTitle("Confirmation");
			alertDialog2.setMessage("Clear comic?");
			alertDialog2.setButton("Yes", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mainView.resetObjects();
					mainView.invalidate();
				}
			});
			alertDialog2.setButton2 ("No", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			alertDialog2.show();
			break;
		case (R.id.add_pack): // comic pack
			if (externalImages.keySet().contains(item.getTitle())) {
				packSelected = item.getTitle();
//				Set<CharSequence> cs = externalImages.get(item.getTitle()).keySet().(type[]) collection.toArray(new type[collection.size()]);
				CharSequence[] cs = (CharSequence[]) externalImages.get(item.getTitle()).keySet().toArray(new CharSequence[externalImages.get(item.getTitle()).keySet().size()]);
				AlertDialog alertDialog3;
				alertDialog3 = new AlertDialog.Builder(this)
                .setTitle("Select Folder")
                .setItems(cs, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        /* User clicked so do some stuff */
        				CharSequence[] cs = (CharSequence[]) externalImages.get(packSelected).keySet().toArray(new CharSequence[externalImages.get(packSelected).keySet().size()]);
                    	new AlertDialog.Builder(mainView.getContext())
                                .setMessage("You selected: " + which + " , " + cs[which])
                                .show();
                    }
                })
                .create();
				alertDialog3.show();
				Log.d ("RAGE", "YEAH " + item.getTitle());
			}
			else {
				AlertDialog alertDialog3;
				alertDialog3 = new AlertDialog.Builder(this).create();
				alertDialog3.setTitle("Comic Packs");
				alertDialog3.setMessage("No comic packs were found. Make sure you place them in the 'ComicMaker' directory on your external storage (SD card).");
				alertDialog3.setButton("Ok", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				alertDialog3.show();
			}
			break;
			
		}
			
		return super.onOptionsItemSelected(item);
	}
	public void colorChanged(int c) {
		mainView.setCurrentColor(c);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		Log.d ("RAGE", "Save instance");
		super.onSaveInstanceState(outState);
		Vector<ImageObject> ios = mainView.getImageObjects();
		outState.putInt("imageObjectCount", ios.size ());
        for (int i = 0; i < ios.size (); ++i) {
        	int[] params = new int[2];
        	params[0] = ios.get(i).getPosition().x;
        	params[1] = ios.get(i).getPosition().y;
        	outState.putIntArray(String.format("ImageObject%dpos", i), params);
        	outState.putFloat(String.format("ImageObject%drot", i), ios.get(i).getRotation());
        	outState.putFloat(String.format("ImageObject%dscale", i), ios.get (i).getScale ());
        	outState.putInt(String.format("ImageObject%drid", i), ios.get (i).getDrawableId());
        }
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			if (!mainView.popState()) {
				AlertDialog alertDialog;
				alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Confirmation");
				alertDialog.setMessage("Are you sure you want to exit?");
				alertDialog.setButton("Yes", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish ();
					}
				});
				alertDialog.setButton2("No", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				alertDialog.show();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.findItem(R.id.pen_color).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL);
		menu.findItem(R.id.pen_width).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL);
		menu.findItem(R.id.text_color).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);
		menu.findItem(R.id.text_type).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);

	    Log.d ("RAGE", "Main menu");
		return super.onPrepareOptionsMenu(menu);
	}

	public void widthChanged(int width) {
		mainView.setCurrentStrokeWidth(width);
		
	}

}