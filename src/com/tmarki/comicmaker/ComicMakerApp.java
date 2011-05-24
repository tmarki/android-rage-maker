package com.tmarki.comicmaker;

import java.util.Map;
import java.util.HashMap;
import java.util.Vector;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.example.blahblah.R;
import com.tmarki.comicmaker.ColorPickerDialog;
import com.tmarki.comicmaker.ComicEditor;
import com.tmarki.comicmaker.Picker;
import com.tmarki.comicmaker.ComicEditor.TouchModes;
import com.tmarki.comicmaker.Picker.OnWidthChangedListener;
import com.tmarki.comicmaker.TextObject.FontType;




public class ComicMakerApp extends Activity implements ColorPickerDialog.OnColorChangedListener, OnWidthChangedListener {
	private ComicEditor mainView;
	private Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> externalImages = new HashMap<CharSequence, Map<CharSequence, Vector<CharSequence>>>();
	private CharSequence packSelected;
	private CharSequence folderSelected;
	private ImageSelect imgsel = null;
	private FontSelect fontselect = null;
	
	void readExternalFiles(){
		externalImages = PackHandler.getBundles();
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d ("RAGE", "Save instance");
		super.onSaveInstanceState(outState);
		outState.putSerializable("touchMode", mainView.getmTouchMode());
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
        	outState.putString(String.format("ImageObject%dpack", i), ios.get (i).pack);
        	outState.putString(String.format("ImageObject%dfolder", i), ios.get (i).folder);
        	outState.putString(String.format("ImageObject%dfile", i), ios.get (i).filename);
        }
		Vector<TextObject> tobs = mainView.getTextObjects();
		outState.putInt("textObjectCount", tobs.size ());
        for (int i = 0; i < tobs.size (); ++i) {
        	outState.putInt(String.format("TextObject%dx", i), tobs.get (i).getX());
        	outState.putInt(String.format("TextObject%dy", i), tobs.get (i).getY());
        	outState.putInt(String.format("TextObject%dsize", i), tobs.get (i).getTextSize());
        	outState.putInt(String.format("TextObject%dcolor", i), tobs.get (i).getColor());
        	outState.putSerializable(String.format("TextObject%dtypeface", i), tobs.get (i).getTypeface());
        	outState.putString(String.format("TextObject%dtext", i), tobs.get (i).getText());
        	outState.putBoolean(String.format("TextObject%dbold", i), tobs.get (i).isBold());
        	outState.putBoolean(String.format("TextObject%ditalic", i), tobs.get (i).isItalic());
        }
        Vector<Vector<Point>> points = mainView.getPoints();
        Vector<Paint> paints = mainView.getPaints();
		outState.putInt("lineCount", points.size ());
        for (int i = 0; i < points.size (); ++i) {
        	outState.putInt(String.format("line%dpointcount", i), points.get (i).size());
            for (int j = 0; j < points.get (i).size (); ++j) {
            	outState.putInt(String.format("line%dpoint%dx", i, j), points.get (i).get(j).x);
            	outState.putInt(String.format("line%dpoint%dy", i, j), points.get (i).get(j).y);
            }
            outState.putFloat(String.format("line%dstroke", i), paints.get (i).getStrokeWidth());
            outState.putInt(String.format("line%dcolor", i), paints.get (i).getColor());
        }
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		Log.d ("RAGE", "BlahGame created");
	    Log.d ("RAGE", "Dir: " + Environment.getExternalStorageDirectory());
        super.onCreate(savedInstanceState);
        mainView = new ComicEditor (this);
        registerForContextMenu(mainView);
        setContentView(mainView);
        readExternalFiles();
        try {
        	if (savedInstanceState.getSerializable("touchMode") != null)
        		mainView.setmTouchMode((ComicEditor.TouchModes)savedInstanceState.getSerializable("touchMode"));
        }
        catch (Exception e) {
        	
        }
        int ioCount = 0;
        if (savedInstanceState != null)
        	ioCount = savedInstanceState.getInt("imageObjectCount", 0);
        for (int i = 0; i < ioCount; ++i) {
        	int[] params = savedInstanceState.getIntArray(String.format("ImageObject%dpos", i));
        	float rot = savedInstanceState.getFloat(String.format("ImageObject%drot", i));
        	float sc = savedInstanceState.getFloat(String.format("ImageObject%dscale", i));
        	int rid = savedInstanceState.getInt(String.format("ImageObject%drid", i));
        	String pack = savedInstanceState.getString(String.format("ImageObject%dpack", i));
        	String folder = savedInstanceState.getString(String.format("ImageObject%dfolder", i));
        	String file = savedInstanceState.getString(String.format("ImageObject%dfile", i));
        	if (rid > 0) {
        		Drawable dr = getResources().getDrawable(rid);
        		mainView.addImageObject (dr, params[0], params[1], rot, sc, rid);
        	}
        	else if (pack.length() > 0) { 
    			mainView.addImageObject(PackHandler.getPackDrawable(pack, folder, file), params[0], params[1], rot,sc, rid);
        	}
        }
        int txCount = 0;
        if (savedInstanceState != null)
        	txCount = savedInstanceState.getInt("textObjectCount", 0);
        for (int i = 0; i < txCount; ++i) {
        	int x = savedInstanceState.getInt(String.format("TextObject%dx", i));
        	int y = savedInstanceState.getInt(String.format("TextObject%dy", i));
        	int s = savedInstanceState.getInt(String.format("TextObject%dsize", i));
        	int c = savedInstanceState.getInt(String.format("TextObject%dcolor", i));
        	TextObject.FontType ft = (FontType) savedInstanceState.getSerializable(String.format("TextObject%dtypeface", i));
        	String text = savedInstanceState.getString(String.format("TextObject%dtext", i));
        	Boolean bold = savedInstanceState.getBoolean(String.format("TextObject%dbold", i));
        	Boolean italic = savedInstanceState.getBoolean(String.format("TextObject%ditalic", i));
        	mainView.addTextObject(x, y, s, c, ft, text, bold, italic);
        }
        int pCount = 0;
        if (savedInstanceState != null) 
        	pCount = savedInstanceState.getInt("lineCount", 0);
        for (int i = 0; i < pCount; ++i) {
        	Vector<Point> p = new Vector<Point>();
        	int cnt = savedInstanceState.getInt(String.format("line%dpointcount", i));
        	for (int j = 0; j < cnt; ++j) {
        		int x = savedInstanceState.getInt(String.format("line%dpoint%dx", i, j));
        		int y = savedInstanceState.getInt(String.format("line%dpoint%dy", i, j));
        		p.add(new Point (x, y));
        	}
        	Paint pp = new Paint ();
        	pp.setStrokeWidth(savedInstanceState.getFloat(String.format("line%dstroke", i)));
        	pp.setColor(savedInstanceState.getInt(String.format("line%dcolor", i)));
        	mainView.addLine(p, pp); 
        }
        mainView.resetHistory();
        mainView.invalidate();
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
		case (R.id.add_pack): //  comic pack
			if (externalImages.size() > 0) {
				doComicPackSelect();
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
		case (R.id.text_type):
			fontselect = new FontSelect (this, setFontTypeListener, mainView.getDefaultFontSize(), mainView.isDefaultBold(), mainView.isDefaultItalic());
			fontselect.show();
			break;
		}
			
		return super.onOptionsItemSelected(item);
	}
	
	private void doComicPackSelect () {
		CharSequence[] cs = (CharSequence[]) externalImages.keySet().toArray(new CharSequence[externalImages.keySet().size()]);
		AlertDialog alertDialog3;
		alertDialog3 = new AlertDialog.Builder(this)
        .setTitle("Select Pack")
        .setItems(cs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
				CharSequence[] cs = (CharSequence[]) externalImages.keySet().toArray(new CharSequence[externalImages.keySet().size()]);
				packSelected = cs[which];
				doComicPackFolderSelect();
            }
        })
        .create();
		alertDialog3.show();
	}
	
	private void doComicPackFolderSelect () {
		CharSequence[] ccs = (CharSequence[]) externalImages.get (packSelected).keySet().toArray(new CharSequence[externalImages.get (packSelected).keySet().size()]);
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(mainView.getContext())
        .setTitle("Select Folder")
        .setItems(ccs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which2) {

                /* User clicked so do some stuff */
				CharSequence[] ccs = (CharSequence[]) externalImages.get (packSelected).keySet().toArray(new CharSequence[externalImages.get (packSelected).keySet().size()]);
				folderSelected = ccs[which2];
				doComicPackImageSelect();
/*            	new AlertDialog.Builder(mainView.getContext())
                        .setMessage("You selected: " + which2 + " , " + ccs[which2])
                        .show();*/
            }
        })
        .create();
		alertDialog.show();
	}

	private OnItemClickListener setFontTypeListener = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Log.d ("RAGE", "Clicked: " + String.valueOf(arg2));
			fontselect.dismiss();
			mainView.setCurrentFont(TextObject.FontType.values()[arg2]);
			mainView.setDefaultBold(fontselect.isBold());
			mainView.setDefaultItalic(fontselect.isItalic());
			mainView.setDefaultFontSize(fontselect.getFontSize());
			mainView.invalidate();
		}
    };

	private OnItemClickListener addFromPackListener = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Log.d ("RAGE", "Clicked: " + String.valueOf(arg2));
			imgsel.dismiss();
			String fname = externalImages.get (packSelected).get(folderSelected).get (arg2).toString();
			mainView.addImageObject(PackHandler.getPackDrawable(packSelected.toString(), folderSelected.toString(), fname), 100, 100, 0.0f, 1.0f, 0, packSelected.toString(), folderSelected.toString(), fname);
		}
    };
	private void doComicPackImageSelect () {
		imgsel = new ImageSelect(this, packSelected.toString(), folderSelected.toString(), addFromPackListener, mainView.getWidth() > mainView.getHeight () ? mainView.getWidth () : mainView.getHeight ());
		imgsel.show();
	}
	

	public void colorChanged(int c) {
		mainView.setCurrentColor(c);
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