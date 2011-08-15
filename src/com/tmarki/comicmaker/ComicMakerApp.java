package com.tmarki.comicmaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.tmarki.comicmaker.ColorPickerDialog;
import com.tmarki.comicmaker.ComicEditor;
import com.tmarki.comicmaker.R;
import com.tmarki.comicmaker.ComicEditor.ComicState;
import com.tmarki.comicmaker.WidthPicker;
import com.tmarki.comicmaker.ComicEditor.TouchModes;
import com.tmarki.comicmaker.WidthPicker.OnWidthChangedListener;
import com.tmarki.comicmaker.ComicSettings;




public class ComicMakerApp extends Activity implements ColorPickerDialog.OnColorChangedListener, OnWidthChangedListener {
	private ComicEditor mainView;
	private Map<CharSequence, Map<CharSequence, Vector<String>>> externalImages = new HashMap<CharSequence, Map<CharSequence, Vector<String>>>();
	private CharSequence packSelected;
	private CharSequence folderSelected;
	private FontSelect fontselect = null;
	private ComicSettings settings = null;
	private MenuItem menuitem_OtherSource = null;
	private String lastSaveName = "";
	private Map<MenuItem, CharSequence> menuitems_Packs = new HashMap<MenuItem, CharSequence> ();
	private ImageSelect imageSelector = null;
	
	void readExternalFiles(){
		externalImages = PackHandler.getBundles(getAssets ());
		for (CharSequence p : externalImages.keySet()) {
			for (CharSequence f : externalImages.get (p).keySet()) {
				Collections.sort(externalImages.get (p).get (f));
			}
		}
	}
	
	
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
		saveExternalSources(outState);
		saveImagesToBundle(outState, mainView.getImageObjects(), "");
		saveLinesToBundle (outState, mainView.getPoints(), mainView.getPaints(), "");
		Vector<ComicEditor.ComicState> history = mainView.getHistory();
		outState.putInt("historySize", history.size ());
		for (int i = 0; i < history.size (); ++i) {
			saveImagesToBundle(outState, history.get (i).mDrawables, String.format("h%s", i));
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
        mainView = new ComicEditor (this, new ComicEditor.ZoomChangeListener() {
			public void ZoomChanged(float newScale) {
				setDetailTitle ();
			}
		});
        registerForContextMenu(mainView);
        setContentView(mainView);
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
            PackHandler.setAssetManager(getAssets ());
            loadExternalSources(savedInstanceState);
            setDetailTitle ();
        }
        else
        	readExternalFiles();
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
        	cs.mDrawables = loadImagesFromBundle(savedInstanceState, String.format("h%s", i));
        	cs.linePoints = loadPointsFromBundle(savedInstanceState, String.format("h%s", i));
        	cs.mLinePaints = new LinkedList<Paint> ();
        	cs.mPanelCount = savedInstanceState.getInt(String.format("h%spanelCount", i));
        	for (int j = 0; j < cs.linePoints.size (); ++j) {
        		cs.mLinePaints.add(getPaintForPoint(savedInstanceState, j, String.format("h%s", i)));
        	}
        	mainView.pushHistory(cs);
        }
        mainView.invalidate();
    }
    
    private void saveExternalSources (Bundle outState) {
    	int i = 0;
    	outState.putInt("packCount", externalImages.keySet().size ());
    	for (CharSequence pack : externalImages.keySet()) {
    		outState.putCharSequence(String.format ("pack%s", i), pack);
        	outState.putInt(String.format ("folderCount%s", i), externalImages.get (pack).keySet().size ());
        	int j = 0;
        	for (CharSequence folder : externalImages.get(pack).keySet()) {
        		outState.putCharSequence(String.format ("folder%s-%s", i, j), folder);
            	outState.putInt(String.format ("fileCount%s-%s", i, j), externalImages.get (pack).get (folder).size ());
            	int k = 0;
            	for (String file : externalImages.get (pack).get (folder)) {
            		outState.putString(String.format ("file%s-%s-%s", i, j, k++), file);
            	}
            	j++;
        	}
        	i++;
    	}
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
        	outState.putInt(String.format(tag + "ImageObject%drid", i), ios.get (i).getDrawableId());
        	outState.putString(String.format(tag + "ImageObject%dpack", i), ios.get (i).pack);
        	outState.putString(String.format(tag + "ImageObject%dfolder", i), ios.get (i).folder);
        	outState.putString(String.format(tag + "ImageObject%dfile", i), ios.get (i).filename);
        	outState.putBoolean(String.format(tag + "ImageObject%dfv", i), ios.get(i).isFlipVertical());
        	outState.putBoolean(String.format(tag + "ImageObject%dfh", i), ios.get(i).isFlipHorizontal());
        	try {
        		TextObject to = (TextObject)ios.get(i);
        		if (to != null) {
        			outState.putInt(String.format(tag + "TextObject%dtextSize"), to.getTextSize());
        			outState.putInt(String.format(tag + "TextObject%dcolor"), to.getColor());
        			outState.putInt(String.format(tag + "TextObject%dtypeface"), to.getTypeface());
        			outState.putString(String.format(tag + "TextObject%dtext"), to.getText());
        			outState.putBoolean(String.format(tag + "TextObject%dbold"), to.isBold());
        			outState.putBoolean(String.format(tag + "TextObject%ditalic"), to.isItalic());
        		}
        	}
        	catch (Exception e) {
        		
        	}
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

    private void loadExternalSources (Bundle savedInstanceState) {
    	externalImages.clear();
    	int pc = savedInstanceState.getInt("packCount");
    	for (int i = 0; i < pc; ++i) {
    		CharSequence pack = savedInstanceState.getCharSequence(String.format ("pack%s", i));
    		Map<CharSequence, Vector<String>> folders = new HashMap<CharSequence, Vector<String>> ();
    		int foc = savedInstanceState.getInt(String.format ("folderCount%s", i));
    		for (int j = 0; j < foc; ++j) {
    			Vector<String> files = new Vector<String> ();
    			CharSequence folder = savedInstanceState.getCharSequence(String.format ("folder%s-%s", i, j));
    			int fic = savedInstanceState.getInt(String.format ("fileCount%s-%s", i, j));
    			for (int k = 0; k < fic; ++k) {
    				files.add(savedInstanceState.getString(String.format ("file%s-%s-%s", i, j, k)));
    			}
    			folders.put(folder, files);
    		}
    		externalImages.put(pack, folders);
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
        	String text = savedInstanceState.getString(String.format(tag + "ImageObject%dtext", ""));
        	String pack = savedInstanceState.getString(String.format(tag + "ImageObject%dpack", i));
        	String folder = savedInstanceState.getString(String.format(tag + "ImageObject%dfolder", i));
        	String file = savedInstanceState.getString(String.format(tag + "ImageObject%dfile", i));
        	ImageObject io = null;
        	BitmapDrawable dr = null;
        	try {
	        	if (text.length() > 0) {
	            	int ts = savedInstanceState.getInt(String.format(tag + "TextObject%dtextSize", 20));
	            	int col = savedInstanceState.getInt(String.format(tag + "TextObject%dcolor", Color.BLACK));
	            	int tf = savedInstanceState.getInt(String.format(tag + "TextObject%dtypeface", 0));
	            	String ss = savedInstanceState.getString(String.format(tag + "TextObject%dtext", ""));
	            	boolean bld = savedInstanceState.getBoolean(String.format(tag + "TextObject%dbold"));
	            	boolean itlic = savedInstanceState.getBoolean(String.format(tag + "TextObject%ditalic"));
	        		io = new TextObject(params[0], params[1], ts, col, tf, ss, bld, itlic);
	        	}
	        	else if (pack.length() > 0) {
	        		dr = PackHandler.getPackDrawable(pack, folder, file);
	        	}
	        	else if (file.length() > 0) {
					dr = new BitmapDrawable(file);
	        	}
				if (dr != null) {
					io = new ImageObject(dr, params[0], params[1], rot, sc, 0, pack, folder, file);
				}
        	}
        	catch (Exception e) {
				Toast.makeText(getWindow ().getContext(), "Comic Maker internal problem in restoring state!",Toast.LENGTH_SHORT).show();
        	}
        	if (io != null) {
        		io.setFlipHorizontal(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dfh", i)));
        		io.setFlipVertical(savedInstanceState.getBoolean(String.format(tag + "ImageObject%dfv", i)));
        		ret.add (io);
        	}
        }
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
    	return pp;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
		SubMenu sm = menu.addSubMenu("Add Image");
	    inflater.inflate(R.menu.main_menu, menu);
		menuitems_Packs.clear();
        for (CharSequence s : externalImages.keySet()) {
        	if (sm != null) {
        		MenuItem mi = sm.add("Pack: " + s);
        		menuitems_Packs.put(mi, s);
        	}
	    }
	    menuitem_OtherSource = sm.add("From other source");
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
		mainView.invalidate();
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.about:
			AlertDialog alertDialog;
			alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("About Rage Comic Maker");
			String versionname = "?";
			try {
				PackageInfo manager=getPackageManager().getPackageInfo(getPackageName(), 0);
				versionname = manager.versionName;
			}
			catch (NameNotFoundException nof) {
				
			}
		    alertDialog.setMessage("Rage Comic Maker v"+versionname+"\nfor Android\n\n(c) 2011 Tamas Marki\nThis is open source software. Use it at your own risk.\nThe source code is available at the home page.");
			alertDialog.setButton("Home Page", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = "http://code.google.com/p/android-rage-maker/";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);					
				}
			});
			alertDialog.setButton2("Report a Bug", new OnClickListener() {
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
			ColorPickerDialog cpd = new ColorPickerDialog(mainView.getContext(), this, "key", mainView.getCurrentColor(), mainView.getCurrentColor());
			cpd.show();
			break;
		case (R.id.pen_width):
			WidthPicker np = new WidthPicker (mainView.getContext(), this, mainView.getCurrentStrokeWidth());
			np.show();
			break;
		case (R.id.clear):
			AlertDialog alertDialog2;
			alertDialog2 = new AlertDialog.Builder(this).create();
			alertDialog2.setTitle("Confirmation");
			alertDialog2.setMessage("Clear comic?");
			alertDialog2.setButton("Yes", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					lastSaveName = "";
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
		
		case (R.id.text_type):
			fontselect = new FontSelect (this, setFontTypeListener, mainView.getDefaultFontSize(), mainView.isDefaultBold(), mainView.isDefaultItalic());
			fontselect.show();
			break;
		case (R.id.settings):
			settings = new ComicSettings (this, mainView.getPanelCount(), mainView.isDrawGrid(), new View.OnClickListener() {

				public void onClick(View v) {
					mainView.setPanelCount(settings.getPanelCount ());
					mainView.setDrawGrid(settings.getDrawGrid());
					settings.dismiss();
					mainView.invalidate();
				}
			});
			settings.show();
			break;
		case (R.id.share):
			if (lastSaveName == "") {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("Enter file name");
				final EditText input = new EditText(this);
				alert.setView(input);
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						doSave (input.getText().toString(), true);
				  }
				});
		
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
	
			salert.setTitle("Enter file name");
			// Set an EditText view to get user input 
			final EditText sinput = new EditText(this);
			sinput.setText(lastSaveName);
			salert.setView(sinput);
	
			salert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					doSave (sinput.getText().toString(), false);
			  }
			});
	
			salert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			  }
			});
	
			salert.show();
			break;
		case (R.id.exit):
			finish ();
			break;
		default:
			if (menuitem_OtherSource == item) {
				
			// To open up a gallery browser
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, "Select Picture"),1);
			}
			else if (menuitems_Packs.containsKey(item)) {
				packSelected = menuitems_Packs.get(item);
				doComicPackFolderSelect();
			}
			break;
		}
			
		return super.onOptionsItemSelected(item);
	}
	
	private void doSave (String fname, boolean doShare) {
		String value = fname;
		Bitmap b = mainView.getSaveBitmap();
		CharSequence text = "Comic saved as ";
		try {
			String folder = Environment.getExternalStorageDirectory().toString() + "/Pictures";
			try {
				folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
			}
			catch (Exception e) {
				
			}
			java.io.File f = new java.io.File (folder);
			if (!f.exists())
				f.mkdirs();
			b.compress(CompressFormat.JPEG, 95, new FileOutputStream(Environment.getExternalStorageDirectory() + "/Pictures/" + value + ".jpg"));
			text = text + value + ".jpg" + " in the Pictures folder on the SD card. It should appear in the gallery shortly.";
			lastSaveName = value;
			setDetailTitle ();
			if (doShare) {
	            Intent share = new Intent(Intent.ACTION_SEND);
	            share.setType("image/jpeg");
	
	            share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Pictures/" + value + ".jpg"));
	            share.putExtra(Intent.EXTRA_TITLE, value);
	
	            startActivity(Intent.createChooser(share, "Share Comic"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			text = "There was an error while saving the comic: " + e.toString();
		}
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
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
					BitmapDrawable bdr = new BitmapDrawable(fname);
					if (bdr != null) {
						mainView.addImageObject(bdr, -mainView.getmCanvasOffset().x + bdr.getIntrinsicWidth(), -mainView.getmCanvasOffset().y + bdr.getIntrinsicWidth(), 0.0f, 1.0f, 0, "", "", fname);
						success = true;
					}
				}
			}
			if (!success) {
				Toast.makeText(getWindow ().getContext(), "There was an error adding the image!",Toast.LENGTH_LONG).show();
				
			}
		}
	}

	// And to convert the image URI to the direct file system path of the image file
	public String getRealPathFromURI(Uri contentUri) {
		if (contentUri == null)
			return "";

		// can post image
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
	private void doComicPackFolderSelect () {
		CharSequence[] ccs = (CharSequence[]) externalImages.get (packSelected).keySet().toArray(new CharSequence[externalImages.get (packSelected).keySet().size()]);
		Arrays.sort(ccs);
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(mainView.getContext())
        .setTitle("Select Folder")
        .setItems(ccs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which2) {

                /* User clicked so do some stuff */
				CharSequence[] ccs = (CharSequence[]) externalImages.get (packSelected).keySet().toArray(new CharSequence[externalImages.get (packSelected).keySet().size()]);
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
		imageSelector = new ImageSelect(getWindow().getContext(), packSelected, folderSelected, externalImages, new ImageSelect.BackPressedListener() {
			
			public void backPressed() {
				doComicPackFolderSelect();
			}
		});
		imageSelector.showImageSelect(new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                int item) {
//                                        Toast.makeText(getContext(), "You selected: " + item,Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                        			String fname = externalImages.get (packSelected).get(folderSelected).get (item).toString();
                        			BitmapDrawable id = PackHandler.getPackDrawable(packSelected.toString(), folderSelected.toString(), fname);
                        			if (id != null) {
                        				mainView.addImageObject(id, -mainView.getmCanvasOffset().x + id.getIntrinsicWidth() / 2, -mainView.getmCanvasOffset().y + id.getIntrinsicWidth() / 2, 0.0f, 1.0f, 0, packSelected.toString(), folderSelected.toString(), fname);
                        			}
                        			else {
                        				Toast.makeText(getApplicationContext(), "Failed to add image!", Toast.LENGTH_LONG);
                        			}
                                }
                        });
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
		menu.findItem(R.id.pen_color).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL || mainView.getmTouchMode() == TouchModes.LINE);
		menu.findItem(R.id.pen_width).setVisible(mainView.getmTouchMode() == TouchModes.PENCIL || mainView.getmTouchMode() == TouchModes.LINE);
		menu.findItem(R.id.text_color).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);
		menu.findItem(R.id.text_type).setVisible(mainView.getmTouchMode() == TouchModes.TEXT);

		return super.onPrepareOptionsMenu(menu);
	}

	public void widthChanged(int width) {
		mainView.setCurrentStrokeWidth(width);
		
	}


	public void colorChanged(String key, int color) {
		
		mainView.setCurrentColor(color);
	}



}