package com.tmarki.comicmaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.hardware.Camera.Size;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Config;
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.tmarki.comicmaker.ColorPickerDialog;
import com.tmarki.comicmaker.ComicEditor;
import com.tmarki.comicmaker.R;
import com.tmarki.comicmaker.WidthPicker;
import com.tmarki.comicmaker.ComicEditor.TouchModes;
import com.tmarki.comicmaker.WidthPicker.OnWidthChangedListener;
import com.tmarki.comicmaker.ComicSettings;
import com.tmarki.comicmaker.ZoomPicker.OnZoomChangedListener;
import com.tmarki.comicmaker.CommentNagger;




public class ComicMakerActivity extends Activity implements ColorPickerDialog.OnColorChangedListener, OnWidthChangedListener, OnZoomChangedListener {
	private AdRequest adRequest = new AdRequest();
	private AdView adView = null;
	private ComicEditor mainView;
	private Map<CharSequence, Vector<String>> packImages = new HashMap<CharSequence, Vector<String>>();
	private CharSequence packSelected;
	private CharSequence folderSelected;
	private FontSelect fontselect = null;
	private ComicSettings settings = null;
	private MenuItem menuitem_OtherSource = null;
	private String lastSaveName = "";
	private Map<MenuItem, CharSequence> menuitems_Packs = new HashMap<MenuItem, CharSequence> ();
	private ImageSelect imageSelector = null;
	private Intent intent = new Intent();
//	private LinearLayout layout = null; 
	private SharedPreferences mPrefs = null;
	private PackHandler packhandler = new PackHandler ();
	private DraftManager draftManager;
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("touchMode", mainView.getmTouchMode());
		outState.putInt("currentColor", mainView.getCurrentColor());
		outState.putInt("currentStrokeWidth", mainView.getCurrentStrokeWidth());
		outState.putInt("currentPanelCount", mainView.getPanelCount());
		outState.putBoolean("drawGrid", mainView.isDrawGrid());
		outState.putFloat("canvasScale", mainView.getCanvasScale());
		outState.putInt("canvasX", mainView.getmCanvasOffset().x);
		outState.putInt("canvasY", mainView.getmCanvasOffset().y);
		outState.putString ("lastSaveName", lastSaveName);
		saveImagesToBundle(outState, mainView.getImageObjects(), "");
		saveLinesToBundle (outState, mainView.getPoints(), mainView.getPaints(), "");
		Vector<ComicState> history = mainView.getHistory();
		outState.putInt("historySize", history.size ());
		for (int i = 0; i < history.size (); ++i) {
			//saveImagesToBundle(outState, history.get (i).mDrawables, String.format("h%s", i));
			saveLinesToBundle (outState, history.get (i).linePoints, history.get (i).mLinePaints, String.format("h%s", i));
			outState.putInt(String.format ("h%spanelCount", i), history.get (i).mPanelCount);
		}
	}

	private void setDetailTitle () {
		if (lastSaveName != "")
			setTitle (getString(R.string.app_name) + " - " + lastSaveName + " - " + String.format("%.0f%%", mainView.getCanvasScale() * 100.0));
		else
			setTitle (getString (R.string.app_name) + " - " + String.format("%.0f%%", mainView.getCanvasScale() * 100.0));
		
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mainView = new ComicEditor (this, new ComicEditor.ZoomChangeListener() {
			public void ZoomChanged(float newScale) {
				setDetailTitle ();
			}
		});
        registerForContextMenu(mainView);
        
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation (LinearLayout.VERTICAL);

        // Create the adView

        // Add the adView to it


        mPrefs = getSharedPreferences("RageComicMaker", 0);
        int showAd = mPrefs.getInt("ShowAd", -1);
        if (showAd == -1) { // never been set
        	showAd = 1;
        	SharedPreferences.Editor ed = mPrefs.edit();
        	ed.putInt("ShowAd", 1);
        	ed.commit();
        }
        if (showAd == 1) {
        // Initiate a generic request to load it with an ad
        	makeAdView();
        	layout.addView(adView);
            layout.addView(mainView);
            adRequest.addTestDevice("4ABF1B0878CAB4C0B1B400C2C5700EBD");
            adRequest.addTestDevice("362F2B1F5C44CF8ECD1E5576D6DBF48F");
            adRequest.addTestDevice("D2969CCA1FE9C413CCD93BE710F04DC3");
        	adView.loadAd(adRequest);
        }
        else {
            layout.addView(mainView);
        }
        setContentView(layout);
    	packImages = packhandler.getBundles(getAssets());
        if (savedInstanceState != null) {
        	if (savedInstanceState.getSerializable("touchMode") != null)
        		mainView.setmTouchMode((ComicEditor.TouchModes)savedInstanceState.getSerializable("touchMode"));
        	mainView.setCurrentColor(savedInstanceState.getInt("currentColor"));
            mainView.setCurrentStrokeWidth(savedInstanceState.getInt("currentStrokeWidth"));
            mainView.setPanelCount(savedInstanceState.getInt("currentPanelCount"));
            mainView.setDrawGrid(savedInstanceState.getBoolean("drawGrid"));
            mainView.setCanvasScale (savedInstanceState.getFloat("canvasScale"));
            mainView.setmCanvasOffset(new Point (savedInstanceState.getInt ("canvasX"), savedInstanceState.getInt ("canvasY")));
            lastSaveName = savedInstanceState.getString("lastSaveName");
            setDetailTitle ();
        }
        else {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        	if (metrics.widthPixels > mainView.getCanvasDimensions().width())
        		mainView.setCanvasScale ((float)metrics.widthPixels / (float)mainView.getCanvasDimensions().width()); 
        }
        for (ImageObject io : loadImagesFromBundle (savedInstanceState, "")) {
        	mainView.pureAddImageObject(io);
        }
        Vector<float[]> points = loadPointsFromBundle (savedInstanceState, "");
        for (int i = 0; i < points.size (); ++i) {
        	float[] p = points.get(i);
        	if (p == null)
        		continue;
        	mainView.pureAddLine (p, getPaintForPoint(savedInstanceState, i, ""));
        }
        mainView.resetHistory();
        int hs = 0;
        if (savedInstanceState != null)
        	hs = savedInstanceState.getInt("historySize", 0);
        for (int i = 0; i < hs; ++i) {
        	ComicState cs = mainView.getStateCopy();
//        	cs.mDrawables = //loadImagesFromBundle(savedInstanceState, String.format("h%s", i));
        	cs.linePoints = loadPointsFromBundle(savedInstanceState, String.format("h%s", i));
        	cs.mLinePaints = new LinkedList<Paint> ();
        	cs.mPanelCount = savedInstanceState.getInt(String.format("h%spanelCount", i));
        	for (int j = 0; j < cs.linePoints.size (); ++j) {
        		cs.mLinePaints.add(getPaintForPoint(savedInstanceState, j, String.format("h%s", i)));
        	}
        	mainView.pushHistory(cs);
        }
		CommentNagger cn = new CommentNagger(this, mPrefs);
		int c = mPrefs.getInt("runcount", 0);
		if (!cn.wasDismissed() && c % 11 == 10)
			cn.show();
		setOrient ();
        // add some test objects
        if (false && mainView.getImageObjects().size() == 0) {
//        	for (int i = 0; i < externalImages.get ("default rage pack").get("Happy").size(); ++i) {
            for (int i = 0; i < 2; ++i) {
        		Bitmap b = packhandler.getDefaultPackDrawable("Happy", packImages.get("Happy").get(i), 0, getAssets());
        		mainView.addImageObject(b, 10 * i, 10 * i, 0.0f, 1.0f, 0, "default rage pack", "Happy", packImages.get("Happy").get(i));
        	}
//        	for (int i = 0; i < externalImages.get ("default rage pack").get("Troll").size(); ++i) {
        	for (int i = 0; i < 2; ++i) {
        		Bitmap b = packhandler.getDefaultPackDrawable("Troll", packImages.get("Troll").get(i), 0, getAssets());
        		mainView.addImageObject(b, 20 * i, 20 * i, 0.0f, 1.0f, 0, "default rage pack", "Troll", packImages.get("Troll").get(i));
        	}
			TextObject to = new TextObject(100, 100,
					50, Color.RED, 0, "Moo moo yeah", false, false);
			to.setSelected(true);
			to.setInBack(false);
			mainView.pureAddImageObject(to);
			
        }
        
        mainView.invalidate();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
    		adView.destroy();
        }
        for (ImageObject io : mainView.getImageObjects()) {
        	io.recycle();
        }
        for (ComicState cs : mainView.getHistory()) {
            for (ImageObject io : cs.mDrawables) {
            	try {
            		io.recycle();
            	}
            	catch (Exception e) {
            		
            	}
            }
        }
      mainView.resetHistory();
      mainView.resetObjects();
		SharedPreferences.Editor ed = mPrefs.edit ();
		int c = mPrefs.getInt("runcount", 0);
		ed.putInt("runcount", c + 1);
		ed.commit();
//    	packhandler.freeAllCache();
        super.onDestroy();
//        mainView = null;
//        layout = null;
    }

    
    private void saveImagesToBundle (Bundle outState, Vector<ImageObject> ios, String tag) {
		outState.putInt(tag + "imageObjectCount", ios.size ());
        for (int i = 0; i < ios.size (); ++i) {
        	int[] params = new int[2];
        	params[0] = ios.get(i).getPosition().x;
        	params[1] = ios.get(i).getPosition().y;
        	outState.putIntArray(String.format(tag + "ImageObject%dpos", i), params);
        	outState.putFloat(String.format(tag + "ImageObject%drot", i), ios.get(i).getRotation());
        	outState.putFloat(String.format(tag + "ImageObject%dscale", i), ios.get (i).getScale ());
        	outState.putString(String.format(tag + "ImageObject%dpack", i), ios.get (i).pack);
        	outState.putString(String.format(tag + "ImageObject%dfolder", i), ios.get (i).folder);
        	outState.putString(String.format(tag + "ImageObject%dfile", i), ios.get (i).filename);
        	outState.putBoolean(String.format(tag + "ImageObject%dfv", i), ios.get(i).isFlipVertical());
        	outState.putBoolean(String.format(tag + "ImageObject%dfh", i), ios.get(i).isFlipHorizontal());
        	outState.putBoolean(String.format(tag + "ImageObject%dselected", i), ios.get(i).isSelected());
        	outState.putBoolean(String.format(tag + "ImageObject%dback", i), ios.get(i).isInBack());
        	outState.putBoolean(String.format(tag + "ImageObject%dlocked", i), ios.get(i).locked);
        	try {
        		TextObject to = (TextObject)ios.get(i);
        		if (to != null) {
        			outState.putInt(String.format(tag + "TextObject%dtextSize", i), to.getTextSize());
        			outState.putInt(String.format(tag + "TextObject%dcolor", i), to.getColor());
        			outState.putInt(String.format(tag + "TextObject%dtypeface", i), to.getTypeface());
        			outState.putString(String.format(tag + "ImageObject%dtext", i), to.getText());
        			outState.putBoolean(String.format(tag + "TextObject%dbold", i), to.isBold());
        			outState.putBoolean(String.format(tag + "TextObject%ditalic", i), to.isItalic());
        		}
        	}
        	catch (Exception e) {
        		Log.w ("RAGE", e.toString());
    			outState.putString(String.format(tag + "ImageObject%dtext", i), "");
        	}
//    		ios.get(i).recycle();
        }
    }
    
    private void saveLinesToBundle (Bundle outState, Vector<float[]> points, LinkedList<Paint> paints, String tag) {
		outState.putInt(tag + "lineCount", points.size ());
		for (int i = 0; i < points.size (); ++i) {
			outState.putFloatArray(String.format(tag + "line%s", i), points.get(i));
            outState.putFloat(String.format(tag + "line%dstroke", i), paints.get (i).getStrokeWidth());
            outState.putInt(String.format(tag + "line%dcolor", i), paints.get (i).getColor());
		}
    }

    private Vector<ImageObject> loadImagesFromBundle (Bundle savedInstanceState, String tag) {
    	Vector<ImageObject> ret = new Vector<ImageObject> ();
        int ioCount = 0;
        if (savedInstanceState != null)
        	ioCount = savedInstanceState.getInt(tag + "imageObjectCount", 0);
        for (int i = 0; i < ioCount; ++i) {
        	int[] params = savedInstanceState.getIntArray(String.format(tag + "ImageObject%dpos", i));
        	float rot = savedInstanceState.getFloat(String.format(tag + "ImageObject%drot", i));
        	float sc = savedInstanceState.getFloat(String.format(tag + "ImageObject%dscale", i));
        	String text = savedInstanceState.getString(String.format(tag + "ImageObject%dtext", i));
        	String pack = savedInstanceState.getString(String.format(tag + "ImageObject%dpack", i));
        	String folder = savedInstanceState.getString(String.format(tag + "ImageObject%dfolder", i));
        	String file = savedInstanceState.getString(String.format(tag + "ImageObject%dfile", i));

        	ImageObject io = null;
        	Bitmap dr = null;
        	try {
	        	if (text.length() > 0) {
	            	int ts = savedInstanceState.getInt(String.format(tag + "TextObject%dtextSize", i), 20);
	            	int col = savedInstanceState.getInt(String.format(tag + "TextObject%dcolor", i), Color.BLACK);
	            	int tf = savedInstanceState.getInt(String.format(tag + "TextObject%dtypeface", i), 0);
	            	boolean bld = savedInstanceState.getBoolean(String.format(tag + "TextObject%dbold", i));
	            	boolean itlic = savedInstanceState.getBoolean(String.format(tag + "TextObject%ditalic", i));
	        		io = new TextObject(params[0], params[1], ts, col, tf, text, bld, itlic);
	        		io.setScale(sc);
	        		io.setRotation(rot);
	        	}
	        	else if (pack.length() > 0) {
	        		dr = packhandler.getDefaultPackDrawable(folder, file, 0, getAssets());
	        		if (dr != null) {
	        			dr = dr.copy(Bitmap.Config.ARGB_8888, false);
	        		}
	        	}
	        	else if (file.length() > 0) {
//					BitmapFactory.Options options=new BitmapFactory.Options();
//					options.inSampleSize = 8;
//					dr = BitmapFactory.decodeFile(file, options);
	        		dr = packhandler.decodeFile(new File (file));
	        	}
				if (dr != null) {
					io = new ImageObject(dr, params[0], params[1], rot, sc, 0, pack, folder, file);
				}
        	}
        	catch (Exception e) {
				Toast.makeText(this, "Comic Maker internal problem: " + e.toString(),Toast.LENGTH_SHORT).show();
        	}
        	if (io != null) {
				io.locked = savedInstanceState.getBoolean(String.format(tag + "ImageObject%dlocked", i));
        		io.setSelected(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dselected", i)));
        		io.setFlipHorizontal(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dfh", i)));
        		io.setFlipVertical(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dfv", i)));
        		io.setInBack(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dback", i)));
        		ret.add (io);
        	}
        }
//        packhandler.freeAllCache();
        return ret;

    }

    private Vector<float[]> loadPointsFromBundle (Bundle savedInstanceState, String tag) {
    	Vector<float[]> ret = new Vector<float[]>();
    	if (savedInstanceState == null)
    		return ret;
    	int pc = savedInstanceState.getInt(tag + "lineCount", 0);
    	for (int i = 0; i < pc; ++i) {
    		float p[] = savedInstanceState.getFloatArray(String.format(tag + "line%s", i));
    		ret.add(p);
    	}
    	return ret;
    }
    
    private Paint getPaintForPoint (Bundle savedInstanceState, int lineInd, String tag) {
    	Paint pp = new Paint ();
    	pp.setStrokeWidth(savedInstanceState.getFloat(String.format(tag + "line%dstroke", lineInd)));
    	pp.setColor(savedInstanceState.getInt(String.format(tag + "line%dcolor", lineInd)));
    	if (pp.getAlpha() == 0)
    		pp.setXfermode(mainView.transparentXfer);
    	return pp;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
		SubMenu sm = menu.addSubMenu(R.string.add);
	    inflater.inflate(R.menu.main_menu, menu);
		menuitems_Packs.clear();
		String s = getResources().getString(R.string.built_in);//"Built-in rage faces";
		MenuItem mi = sm.add(s);
		menuitems_Packs.put(mi, s);
/*        for (CharSequence s : packImages.keySet()) {
        	if (sm != null) {
        		MenuItem mi = sm.add(getResources ().getString (R.string.pack) + " " + s);
        		menuitems_Packs.put(mi, s);
        	}
	    }*/
	    menuitem_OtherSource = sm.add(R.string.add_other);
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
			TextObject to = null;
			try {
				to =(TextObject)io;
			}
			catch (Exception e) {
				
			}
			menu.findItem(R.id.edit).setVisible(to != null);
			menu.findItem(R.id.color).setVisible(to != null);
			mainView.resetClick();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ImageObject io = mainView.getSelected();
		if (item.getItemId() == R.id.toback && io != null)
			io.setInBack(true);
		else if (item.getItemId() == R.id.tofront && io != null)
			io.setInBack(false);
		else if (item.getItemId() == R.id.remove && io != null) {
			mainView.removeImageObject(io);
		}
		else if (item.getItemId() == R.id.flipH && io != null) {
			io.setFlipHorizontal(!io.isFlipHorizontal());
		}
		else if (item.getItemId() == R.id.flipV && io != null) {
			io.setFlipVertical(!io.isFlipVertical());
		}
		else if (item.getItemId() == R.id.lock && io != null) {
			io.locked = !io.locked;
		}
		else if (item.getItemId() == R.id.edit && io != null) {
			try {
				final TextObject to = (TextObject)io;
				AlertDialog.Builder salert = new AlertDialog.Builder(this);
				
				salert.setTitle(R.string.enter_text);
				// Set an EditText view to get user input 
				final EditText sinput = new EditText(this);
				sinput.setText(to.getText());
				salert.setView(sinput);
		
				salert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						to.setText(sinput.getText().toString());
						mainView.invalidate();
				  }
				});
		
				salert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
				  }
				});
		
				salert.show();
			}
			catch (Exception e) {
				
			}
		}
		else if (item.getItemId() == R.id.color && io != null) {
			try {
				final TextObject to = (TextObject)io;
				ColorPickerDialog.OnColorChangedListener ocl = new ColorPickerDialog.OnColorChangedListener() {
					public void colorChanged(String key, int color) {
						to.setColor(color);
						to.regenerateBitmap();
						mainView.invalidate();
					}
				};
				ColorPickerDialog cpd = new ColorPickerDialog(this, ocl, "key", to.getColor(), to.getColor());
				cpd.show();
			}
			catch (Exception e) {
				
			}
		}
		mainView.invalidate();
		return super.onContextItemSelected(item);
	}
	
	private void makeAdView () {
		if (adView == null)
			adView = new AdView(this, AdSize.SMART_BANNER, "a14e6b86ed7b452");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        ActivityManager am = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        Log.d ("RAGE", "Memory: " + String.valueOf(am.getMemoryClass()));
        int[] z = new int[1];
        z[0] = android.os.Process.myPid();
        Debug.MemoryInfo[] mis = am.getProcessMemoryInfo(z);
        // Print to log and read in DDMS
//        Log.i( "RAGE", " minfo.lowMemory " + mInfo.lowMemory );
//        Log.i( "RAGE", " minfo.threshold " + mInfo.threshold );
		switch (item.getItemId())
		{
		case R.id.about:
			AlertDialog alertDialog;
			alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(R.string.abouttitle);
			String versionname = "?";
			try {
				PackageInfo manager=getPackageManager().getPackageInfo(getPackageName(), 0);
				versionname = manager.versionName;
			}
			catch (NameNotFoundException nof) {
				
			}
		    alertDialog.setMessage("Rage Comic Maker v"+versionname+"\nfor Android\n\n(c) 2011-2012 Tamas Marki\n" + getResources().getString(R.string.abouttext));
			alertDialog.setButton(getResources().getString(R.string.home_page), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = "http://code.google.com/p/android-rage-maker/";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);					
				}
			});
			alertDialog.setButton2(getResources().getString(R.string.report_bug), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = "http://code.google.com/p/android-rage-maker/issues/entry";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);					
				}
			});
			alertDialog.setIcon(R.drawable.icon);
			alertDialog.show();
			break;
		case (R.id.pen_color):
		case (R.id.text_color):
			ColorPickerDialog cpd = new ColorPickerDialog(this, this, "key", mainView.getCurrentColor(), mainView.getCurrentColor());
			cpd.show();
			break;
		case (R.id.pen_width):
			WidthPicker np = new WidthPicker (this, this, mainView.getCurrentStrokeWidth());
			np.show();
			break;
		case (R.id.zoom):
			ZoomPicker zp = new ZoomPicker (this, this, mainView.getCanvasScale());
			zp.show();
			break;
		case (R.id.drafts):
	        draftManager = new DraftManager(this, mainView, packhandler);
			draftManager.show();
			draftManager.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					mainView.resetLinesCache();
					mainView.invalidate();
				}
			});
			break;
		case (R.id.clear):
			AlertDialog alertDialog2;
			alertDialog2 = new AlertDialog.Builder(this).create();
			alertDialog2.setTitle(R.string.confirmation);
			alertDialog2.setMessage(getResources().getString(R.string.clear_question));
			alertDialog2.setButton(getResources().getString(R.string.yes), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					lastSaveName = "";
					mainView.resetObjects();
					mainView.invalidate();
				}
			});
			alertDialog2.setButton2 (getResources().getString(R.string.no), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			alertDialog2.show();
			break;
		
		case (R.id.text_type):
			fontselect = new FontSelect (this, setFontTypeListener, mainView.getDefaultFontSize(), mainView.isDefaultBold(), mainView.isDefaultItalic());
			fontselect.show();
			break;
		case (R.id.objmenu):
			if (mainView.getSelected() == null) {
				Toast.makeText(this, R.string.select_object_first, Toast.LENGTH_SHORT).show();
			}
			else {
				mainView.showContextMenu();
			}
			break;
		case (R.id.settings):
			settings = new ComicSettings (this, mainView.getPanelCount(), mainView.isDrawGrid(), adView != null, mPrefs.getInt("orient", 0), mPrefs.getString("format", "JPG"), new View.OnClickListener() {

				public void onClick(View v) {
					mainView.setPanelCount(settings.getPanelCount ());
					mainView.setDrawGrid(settings.getDrawGrid());
		        	SharedPreferences.Editor ed = mPrefs.edit();
					if (settings.getShowAd() && adView == null) {
//						makeAdView();
//			        	layout.addView(adView, 0);
//			        	adView.loadAd(adRequest);
			        	ed.putInt("ShowAd", 1);
					}
					else if (!settings.getShowAd() && adView != null) {
//						layout.removeView(adView);
//						adView = null;
			        	ed.putInt("ShowAd", 0);
						Toast.makeText(mainView.getContext(), "Ads will be off when the app is restarted.", Toast.LENGTH_LONG).show();
					}
					ed.putInt("orient", settings.getOrientation());
					ed.putString("format", settings.getSaveFormat());
		        	ed.commit();
		        	setOrient ();
					settings.dismiss();
					mainView.invalidate();
				}
			});
			settings.show();
			break;
		case (R.id.share):
			if (lastSaveName == "") {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(R.string.select_name);
				final EditText input = new EditText(this);
				alert.setView(input);
				alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						doSave (input.getText().toString(), true);
				  }
				});
		
				alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
				  }
				});
		
				alert.show();
			}
			else {
				doSave (lastSaveName, true);
			}
			break;
		case (R.id.save):
			AlertDialog.Builder salert = new AlertDialog.Builder(this);
	
			salert.setTitle(R.string.select_name);
			// Set an EditText view to get user input 
			final EditText sinput = new EditText(this);
			sinput.setText(lastSaveName);
			salert.setView(sinput);
	
			salert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					doSave (sinput.getText().toString(), false);
			  }
			});
	
			salert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			  }
			});
	
			salert.show();
			break;
		case (R.id.exit):
			finish ();
			System.runFinalization();
			System.exit(2);
			break;
		case (R.id.redo):
			mainView.unpopState();
			break;
		default:
			if (menuitem_OtherSource == item) {
				
			// To open up a gallery browser
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, getResources ().getString (R.string.select_picture)),1);
			}
			else if (menuitems_Packs.containsKey(item)) {
				packSelected = menuitems_Packs.get(item);
				doComicPackFolderSelect();
			}
			return true;
		}
			
		return super.onOptionsItemSelected(item);
	}
	
	private void setOrient () {
		int i = mPrefs.getInt("orient", 0);
		switch (i) {
		case 0:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			break;
		case 1:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case 2:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		}
	}
	
	private void doSave (String fname, boolean doShare) {
		CharSequence text = getResources ().getString (R.string.comic_saved_as) + " ";
		FlurryAgent.logEvent("Save start");
		try {
			String ReservedChars = "|\\?*<\":>+[]/'";
			for (char c : ReservedChars.toCharArray()) {
				fname = fname.replace(c, '_');
			}
			String value = fname;
			Bitmap b = mainView.getSaveBitmap();
			if (b == null) {
				text = getResources ().getString (R.string.comic_save_fail_1);;
				Toast.makeText(this, text, Toast.LENGTH_LONG).show();
				FlurryAgent.logEvent("Save failed: null bitmap");
				return;
			}
			File folder = getFilesDir();
			if (externalStorageAvailable()) {
				try {
					folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
					if (!folder.exists() || !folder.canWrite()) {
						folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					}
					if (!folder.exists() || !folder.canWrite()) {
						folder = Environment.getExternalStorageDirectory();
					}
				}
				catch (Exception e) {
					folder = Environment.getExternalStorageDirectory();
				}
				catch (Error e) {
					folder = Environment.getExternalStorageDirectory();
				}
				if (!folder.exists() || !folder.canWrite()) {
					folder = getFilesDir();
				}
			}
/*			String folder = Environment.getExternalStorageDirectory().toString() + "/Pictures";
			try {
				folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
			}
			catch (NoSuchFieldError e) {
				
			}*/
			String ext = ".jpg";
			if (mPrefs.getString("format", "JPG").equals("PNG"))
				ext = ".png";
			String fullname = folder.getAbsolutePath() + File.separator + value + ext;
			Map<String, String> hm = new HashMap<String, String> ();
			hm.put("filename", fullname);
			FlurryAgent.logEvent("Save image", hm);
			FileOutputStream fos;
			if (folder == getFilesDir())
				fos = openFileOutput(value + ext, Context.MODE_WORLD_WRITEABLE);
			else {
				File f2 = new File (fullname);//openFileOutput(fname, Context.MODE_PRIVATE);//new FileOutputStream(fullname);
				fos = new FileOutputStream(f2);
			}
			if (ext.equals(".png"))
				b.compress(CompressFormat.PNG, 95, fos);
			else
				b.compress(CompressFormat.JPEG, 95, fos);
			fos.close ();
			FlurryAgent.logEvent("Save done");
			String[] str = new String[1];
			str[0] = fullname;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO){
				MediaScannerConnection.scanFile(this, str, null, null);
			}
			text = text + value + ext + " " + getResources ().getString (R.string.saved_end);;
			lastSaveName = value;
			setDetailTitle ();
			if (doShare) {
				FlurryAgent.logEvent("Share start");
	            Intent share = new Intent(Intent.ACTION_SEND);
				if (ext.equals(".png"))
					share.setType("image/png");
				else
					share.setType("image/jpeg");
	
	            share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + fullname.replace(" ", "%20")));
	            share.putExtra(Intent.EXTRA_TITLE, value);
	
	            startActivity(Intent.createChooser(share, getResources ().getString (R.string.share_comic)));
	    		FlurryAgent.logEvent("Share done");
			}
		} catch (Exception e) {
			Map<String, String> hm = new HashMap<String, String> ();
			hm.put("text", e.toString());
			FlurryAgent.logEvent("Save exception", hm);
			e.printStackTrace();
			text = getResources ().getString (R.string.comic_save_fail_2) + e.toString();
		} catch (Error e) {
			Map<String, String> hm = new HashMap<String, String> ();
			hm.put("text", e.toString());
			FlurryAgent.logEvent("Save error", hm);
			e.printStackTrace();
			text = getResources ().getString (R.string.comic_save_fail_2) + e.toString();
		}
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	private boolean externalStorageAvailable () {
		boolean mExternalStorageAvailable;
		boolean mExternalStorageWriteable;
		String state = Environment.getExternalStorageState();
		
		if (state.equals(Environment.MEDIA_MOUNTED)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (state.equals (Environment.MEDIA_MOUNTED_READ_ONLY)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}		
		return mExternalStorageAvailable && mExternalStorageWriteable;
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean success = false;

		if (resultCode == RESULT_OK) {

			if (requestCode == 1) {
				String fname = data.getData ().toString();
				if (fname.startsWith("content://"))
					fname = getRealPathFromURI (data.getData ());
				if (fname.startsWith("file://"))
					fname = fname.replace("file://", "");
				if (fname != "") {
					BitmapFactory.Options options=new BitmapFactory.Options();
					options.inSampleSize = 8;
//					Bitmap b = BitmapFactory.decodeFile(fname, options);
					Bitmap b = packhandler.decodeFile(new File (fname));
					if (b != null) {
						mainView.addImageObject(b, -mainView.getmCanvasOffset().x, -mainView.getmCanvasOffset().y, 0.0f, 1.0f, 0, "", "", fname);
						success = true;
						mainView.setmTouchMode(ComicEditor.TouchModes.HAND);
					}
				}
			}
			if (!success) {
				Toast.makeText(this, R.string.error_adding_image,Toast.LENGTH_LONG).show();
				
			}
		}
	}

	// And to convert the image URI to the direct file system path of the image file
	public String getRealPathFromURI(Uri contentUri) {
		if (contentUri == null)
			return "";

		// can post image
		try {
			String [] proj={MediaStore.Images.Media.DATA};
			Cursor cursor = managedQuery( contentUri,
					proj, // Which columns to return
					null,       // WHERE clause; which rows to return (all rows)
					null,       // WHERE clause selection arguments (none)
					null); // Order-by clause (ascending by name)
			if (cursor == null)
				return "";
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
	
			return cursor.getString(column_index);
		}
		catch (Exception e) {
			FlurryAgent.logEvent("getRealPathFromURI exception: " + e.toString());
		}
		return "";
	}
	private void doComicPackFolderSelect () {
		CharSequence[] ccs = (CharSequence[]) packImages.keySet().toArray(new CharSequence[packImages.keySet().size()]);
		Arrays.sort(ccs);
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.select_folder)
        .setItems(ccs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which2) {

                /* User clicked so do some stuff */
				CharSequence[] ccs = (CharSequence[]) packImages.keySet().toArray(new CharSequence[packImages.keySet().size()]);
				Arrays.sort(ccs);
				folderSelected = ccs[which2];
				doComicPackImageSelect();
            }
        })
        .create();
		alertDialog.show();
	}

	private OnItemClickListener setFontTypeListener = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			fontselect.dismiss();
			mainView.setCurrentFont(arg2);
			mainView.setDefaultBold(fontselect.isBold());
			mainView.setDefaultItalic(fontselect.isItalic());
			mainView.invalidate();
		}
    };

	private void doComicPackImageSelect () {
		imageSelector = new ImageSelect(this, folderSelected, packImages, new ImageSelect.BackPressedListener() {
			
			public void backPressed() {
				doComicPackFolderSelect();
//				packhandler.freeCache(packSelected, folderSelected);
			}
		}, packhandler);
		imageSelector.clickListener = new AdapterView.OnItemClickListener() {
								public void onItemClick(AdapterView<?> arg0,
										View arg1, int arg2, long arg3) {
                        			String fname = imageSelector.myStuff[arg2];//packImages.get(folderSelected).get (arg2).toString();
                        			Bitmap id = packhandler.getDefaultPackDrawable(folderSelected.toString(), fname, 0, getAssets());
                        			boolean rec = true;
                        			Bitmap.Config conf = null;
                        			if (id != null) {
                        				conf = id.getConfig();
                        				rec = id.isRecycled();
                        			}
                        			if (conf == null)
                        				conf = Bitmap.Config.ARGB_8888;
                        			if (id != null && conf != null && !rec) {
                        				mainView.addImageObject(id.copy (conf, false), -mainView.getmCanvasOffset().x, -mainView.getmCanvasOffset().y, 0.0f, 1.0f, 0, packSelected.toString(), folderSelected.toString(), fname);
                						mainView.setmTouchMode(ComicEditor.TouchModes.HAND);
                        			}
                        			else {
                        				Toast.makeText(getApplicationContext(), R.string.error_adding_image, Toast.LENGTH_LONG).show ();
                        			}
                        			imageSelector.dismiss();
								}
                        };
        imageSelector.show();
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
				alertDialog.setTitle(R.string.confirmation);
				alertDialog.setMessage(getResources ().getString (R.string.confirm_exit));
				alertDialog.setButton(getResources ().getString (R.string.yes), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences.Editor ed = mPrefs.edit ();
						int c = mPrefs.getInt("runcount", 0);
						ed.putInt("runcount", c + 1);
						ed.commit();
						finish();
						System.runFinalization();
						System.exit(2);
					}
				});
				alertDialog.setButton3(getResources ().getString (R.string.no), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				alertDialog.setButton2(getResources().getString(R.string.drafts), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
				        draftManager = new DraftManager(ComicMakerActivity.this, mainView, packhandler);
						draftManager.show();
						draftManager.setOnDismissListener(new DialogInterface.OnDismissListener() {
							public void onDismiss(DialogInterface dialog) {
								mainView.resetLinesCache();
								mainView.invalidate();
							}
						});
						
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
		menu.findItem(R.id.pen_color).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL || mainView.getmTouchMode() == TouchModes.LINE);
		menu.findItem(R.id.pen_width).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL || mainView.getmTouchMode() == TouchModes.LINE || mainView.getmTouchMode() == TouchModes.ERASER);
		menu.findItem(R.id.text_color).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);
		menu.findItem(R.id.text_type).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);
		menu.findItem(R.id.redo).setVisible(mainView.isRedoAvailable());

		return super.onPrepareOptionsMenu(menu);
	}

	public void widthChanged(int width) {
		mainView.setCurrentStrokeWidth(width);
		
	}


	public void colorChanged(String key, int color) {
		
		mainView.setCurrentColor(color);
	}

	private boolean handleKeyEvent (KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
			mainView.moveEvent(-1, 0);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
			mainView.moveEvent(1, 0);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
			mainView.moveEvent(0, -1);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
			mainView.moveEvent(0, 1);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_A) {
			mainView.rotateEvent((float)ComicEditor.ROTATION_STEP);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_D) {
			mainView.rotateEvent(-(float)ComicEditor.ROTATION_STEP);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_W) {
			mainView.scaleEvent((float)ComicEditor.ZOOM_STEP);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_S) {
			mainView.scaleEvent(-(float)ComicEditor.ZOOM_STEP);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_R) {
			mainView.unpopState();
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_SPACE) {
			ImageObject io = mainView.getSelected();
			if (io != null)
				io.setInBack(!io.isInBack());
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
			ImageObject io = mainView.getSelected();
			if (io != null) {
				mainView.removeImageObject(io);
				mainView.invalidate();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (handleKeyEvent(event))
			return true;
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (handleKeyEvent(event))
			return true;
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}


	public void zoomChanged(float zoom) {
		mainView.setCanvasScale(zoom);
		mainView.invalidate();
		setDetailTitle();
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int w = mainView.getWidth();
	}


	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "HUEFXH162YB8H9SA9HYY");
	}


	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

}