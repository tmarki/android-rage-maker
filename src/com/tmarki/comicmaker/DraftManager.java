package com.tmarki.comicmaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.Vector;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DraftManager extends Dialog {

	private class DraftDatabase extends SQLiteOpenHelper {
		
		public DraftDatabase(Context context) {
			super (context, "comic_drafts", null, 1);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE draft (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, panel_count INTEGER, draw_grid INTEGER, autosave INTEGER)");
			db.execSQL("CREATE TABLE draft_line (id INTEGER PRIMARY KEY AUTOINCREMENT, draft_id INTEGER NOT NULL, points BLOB, stroke_width REAL, color INTEGER)");
			db.execSQL("CREATE TABLE draft_object (id INTEGER PRIMARY KEY AUTOINCREMENT, draft_id INTEGER NOT NULL, position BLOB,rotation REAL, scale REAL," +
					"pack BLOB, folder BLOB, file BLOB, flip_vertical INTEGER, flip_horizontal INTEGER,bck INTEGER, locked INTEGER," +
					"text_size INTEGER, color INTEGER, typeface INTEGER, txt BLOB, bold INTEGER, italic INTEGER)");
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
		public long putDraft (String name, int panelCount, boolean drawGrid, boolean autosave) {
			if (autosave) {
				int aid = getAutodraftId();
				if (aid > 0)
					removeDraft(aid);
			}
			SQLiteDatabase db = getWritableDatabase();
			ContentValues cv=new ContentValues();
			cv.put("name", name);
			cv.put("panel_count", panelCount);
			cv.put("draw_grid", drawGrid ? 1 : 0);
			cv.put("autosave", autosave ? 1 : 0);
			return db.insert("draft", null, cv);
		}
		public int getAutodraftId () {
			SQLiteDatabase rdb=this.getReadableDatabase();
			Cursor c = rdb.query("draft", new String[]{"id"},
					"autosave=?", new String[]{"1"}, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return c.getInt(c.getColumnIndex("id"));
			}
			return 0;
		}
		public long putLine (long draft_id, float[] points, double strokeWidth, int color) {
			String spoints = "";
			for (int i = 0; i < points.length; ++i) {
				if (spoints.length() > 0)
					spoints += "|";
				spoints += String.valueOf(points[i]);
			}
			SQLiteDatabase db = getWritableDatabase();
			ContentValues cv=new ContentValues();
			cv.put("draft_id", draft_id);
			cv.put("points", spoints);
			cv.put("stroke_width", strokeWidth);
			cv.put("color", color);
			return db.insert("draft_line", null, cv);
		}
		public long putObject (long draft_id, ImageObject io) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues cv=new ContentValues();
			cv.put("draft_id", draft_id);
			cv.put("position", String.format("%d,%d", io.getPosition().x, io.getPosition().y));
			cv.put("rotation", io.getRotation());
			cv.put("scale", io.getScale());
			cv.put("pack", io.pack);
			cv.put("folder", io.folder);
			cv.put("file", io.filename);
			cv.put("flip_vertical", io.flipVertical ? 1 : 0);
			cv.put("flip_horizontal", io.flipHorizontal ? 1 : 0);
			cv.put("bck", io.isInBack() ? 1 : 0);
			cv.put("locked", io.locked ? 1 : 0);
			try {
				TextObject to = (TextObject)io;
				if (to != null) {
					cv.put("text_size", to.getTextSize());
					cv.put("color", to.getColor());
					cv.put("typeface", to.getTypeface());
					cv.put("txt", to.getText());
					cv.put("bold", to.isBold() ? 1 : 0);
					cv.put("italic", to.isItalic() ? 1 : 0);
				}
			}
			catch (Exception e) {
				cv.put("txt", "");
			}
			return db.insert("draft_object", null, cv);
		}
		public Integer[] getDraftIds() {
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft", new String[]{"id as _id"},
					"autosave=?", new String[]{"0"}, null, null, null);
			Integer[] ret = new Integer[c.getCount()];
			for (int i = 0; i < ret.length; ++i) {
				c.moveToPosition(i);
				ret[i] = c.getInt(c.getColumnIndex("_id"));
			}
			return ret;
		}
		public String getDraftName (int id) {
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft", new String[]{"name"},
					"id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			return c.getString(c.getColumnIndex("name"));
		}
		public void removeDraft (int id) {
			SQLiteDatabase db = getWritableDatabase();
			db.delete("draft_object", "draft_id=?", new String [] {String.valueOf(id)});
			db.delete("draft_line", "draft_id=?", new String [] {String.valueOf(id)});
			db.delete("draft", "id=?", new String [] {String.valueOf(id)});
			db.close();
		}
		public int getDraftPanelCount (int id) {
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft", new String[]{"panel_count"},
					"id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			return c.getInt(c.getColumnIndex("panel_count"));
		}
		public boolean getDraftDrawGrid (int id) {
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft", new String[]{"draw_grid"},
					"id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			return c.getInt(c.getColumnIndex("draw_grid")) == 1;
		}
		public Vector<float[]> getDraftLines (int id) {
			Vector<float[]> ret = new Vector<float[]>();
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft_line", new String[]{"points"},
					"draft_id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				String tmp = c.getString(c.getColumnIndex("points"));
				int cnt = 1;
				for (int i = 0; i < tmp.length(); ++i)
					cnt += (tmp.charAt(i) == '|' ? 1 : 0);
				float[] ff = new float[cnt];
				int i = 0;
				for (String s : tmp.split("\\|")) {
					if (s.length() > 0)
						ff[i++] = Float.parseFloat(s);
				}
				ret.add(ff);
				c.moveToNext();
			}
			return ret;
		}
		public LinkedList<Paint> getDraftPaints (int id) {
			LinkedList<Paint> ret = new LinkedList<Paint>();
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft_line", new String[]{"stroke_width", "color"},
					"draft_id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				Paint p = new Paint ();
				p.setColor(c.getInt(c.getColumnIndex("color")));
				p.setStrokeWidth(c.getFloat(c.getColumnIndex("stroke_width")));
				ret.add(p);
				c.moveToNext();
			}
			return ret;
		}
		private Vector<ImageObject> getDraftObjects (int id) { 
			Vector<ImageObject> ret = new Vector<ImageObject> ();
			SQLiteDatabase db=this.getReadableDatabase();
			Cursor c = db.query("draft_object", new String[]{"position", "rotation", "scale", "pack", "folder", "file", "flip_vertical", "flip_horizontal",
					"bck", "locked", "text_size", "color", "typeface", "txt", "bold", "italic"},
					"draft_id=?", new String[]{String.valueOf(id)}, null, null, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				String[] tmp = c.getString(c.getColumnIndex("position")).split(",");
	        	float rot = c.getFloat(c.getColumnIndex("rotation"));
	        	float sc = c.getFloat(c.getColumnIndex("scale"));
	        	String text = c.getString(c.getColumnIndex("txt"));
	        	String pack = c.getString(c.getColumnIndex("pack"));
	        	String folder = c.getString(c.getColumnIndex("folder"));
	        	String file = c.getString(c.getColumnIndex("file"));
	        	ImageObject io = null;
	        	Bitmap dr = null;
	        	try {
		        	if (text.length() > 0) {
		            	int ts = c.getInt(c.getColumnIndex("text_size"));
		            	int col = c.getInt(c.getColumnIndex("color"));
		            	int tf = c.getInt(c.getColumnIndex("typeface"));
		            	boolean bld = c.getInt(c.getColumnIndex("bold")) == 1;
		            	boolean itlic = c.getInt(c.getColumnIndex("italic")) == 1;
		        		io = new TextObject(Integer.valueOf (tmp[0]), Integer.valueOf(tmp[1]), ts, col, tf, text, bld, itlic);
		        		io.setScale(sc);
		        		io.setRotation(rot);
		        	}
		        	else if (pack.length() > 0) {
		        		dr = packhandler.getDefaultPackDrawable(folder, file, 0, getContext().getAssets());
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
						io = new ImageObject(dr, Integer.valueOf (tmp[0]), Integer.valueOf(tmp[1]), rot, sc, 0, pack, folder, file);
					}
	        	}
	        	catch (Exception e) {
//					Toast.makeText(this, "Comic Maker internal problem: " + e.toString(),Toast.LENGTH_SHORT).show();
	        	}
	        	if (io != null) {
					io.locked = c.getInt(c.getColumnIndex("locked")) == 1;
	        		io.setFlipHorizontal(c.getInt(c.getColumnIndex("flip_horizontal")) == 1);
	        		io.setFlipVertical(c.getInt(c.getColumnIndex("flip_vertical")) == 1);
	        		io.setInBack(c.getInt(c.getColumnIndex("bck")) == 1);
	        		ret.add (io);
	        	}
				
				c.moveToNext();
			}
			return ret;
		}
	}
	
	private DraftDatabase draftDB = new DraftDatabase(getContext());
	private ComicState curState = null;
	private ComicEditor editor;
	private PackHandler packhandler = null;
    final LayoutInflater mInflater;

	public DraftManager(Context context, ComicEditor ed, PackHandler ph) {
		super(context);
		packhandler = ph;
		editor = ed;
		curState = ed.getStateRef();
		mInflater = (LayoutInflater)getContext ().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public long saveDraft (ComicState cs, String name, boolean autosave) {
		long id = draftDB.putDraft(name, cs.mPanelCount, cs.drawGrid, autosave);
		for (ImageObject io : cs.mDrawables)
			draftDB.putObject(id, io);
		for (int i = 0; i < cs.linePoints.size(); ++i) {
			Paint p = cs.mLinePaints.get(i);
			draftDB.putLine(id, cs.linePoints.get(i), p.getStrokeWidth(), p.getColor());
		}
		if (!autosave) {
			Bitmap b = editor.getThumbBitmap();
			FileOutputStream fos;
			try {
				File dir = getContext().getDir("thumbs", 0);
				File myFile = new File(dir, String.format("%d.jpg", id));
				fos = new FileOutputStream(myFile);
				b.compress(CompressFormat.JPEG, 80, fos);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return id;
	}
	
	public void autoLoad (ComicState cs) {
		int aid = draftDB.getAutodraftId();
		if (aid > 0) {
			loadDraft(cs, aid);
		}
	}
	
	private void loadDraft (ComicState cs, int id) {
		cs.drawGrid = draftDB.getDraftDrawGrid(id);
		cs.mPanelCount = draftDB.getDraftPanelCount(id);
		cs.linePoints = draftDB.getDraftLines(id);
		cs.mLinePaints = draftDB.getDraftPaints(id);
		cs.mDrawables = draftDB.getDraftObjects(id);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drafts);
		findViewById(R.id.saveDraft).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
				alert.setTitle(R.string.select_name);
				final EditText input = new EditText(getContext());
				alert.setView(input);
				alert.setPositiveButton(getContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						saveDraft(curState, input.getText().toString(), false);
						populateList();
				  }
				});
		
				alert.setNegativeButton(getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
				  }
				});
		
				alert.show();
			}
		});
		populateList();
		setTitle(R.string.drafts);
	}
	
	private void populateList () {
		ListView lv = (ListView)findViewById(R.id.draftList);
		final Integer[] dids = draftDB.getDraftIds();
		lv.setAdapter(new ArrayAdapter<Integer>(
				getContext(), R.layout.image_select_row, dids) {
           
		
		    public View getView(int position, View convertView,
		                    ViewGroup parent) {
				View row;
				 
				if (null == convertView) {
					row = mInflater.inflate(R.layout.image_select_row, null);
				} else {
					row = convertView;
				}
		 
				ImageView tv = (ImageView) row.findViewById(R.id.icon);
				if (tv != null) {
/*					String filename = imageNames[position];
					Bitmap bmp = packhandler.getDefaultPackDrawable(folderSelected.toString(), filename, 0, context.getAssets());
					if (bmp != null)
						tv.setImageBitmap(bmp);*/
					try {
						File dir = getContext().getDir("thumbs", 0);
						File myFile = new File(dir, String.format("%d.jpg", dids[position]));
						Bitmap bmp = BitmapFactory.decodeFile(myFile.getAbsolutePath());
						if (bmp != null)
							tv.setImageBitmap(bmp);
					}
					catch (Exception e) {
						
					}
					TextView title = (TextView) row.findViewById(R.id.title);
					if (title != null)
						title.setText(draftDB.getDraftName(dids[position]));
				}
				return row;
		    }
		});
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
				alert.setTitle(R.string.draft_options);
				final int did = arg2;
				alert.setPositiveButton(getContext().getResources().getString(R.string.load), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						loadDraft (curState, draftDB.getDraftIds()[did]);
						dismiss();
				  }
				});
		
				alert.setNegativeButton(getContext().getResources().getString(R.string.remove), new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
					  int draftid = draftDB.getDraftIds()[did];
					  draftDB.removeDraft (draftid);
						File dir = getContext().getDir("thumbs", 0);
						File myFile = new File(dir, String.format("%d.jpg", draftid));
						myFile.delete();
					  populateList();
				  }
				});
		
				alert.show();
				
			}
		});
		
	}

}






