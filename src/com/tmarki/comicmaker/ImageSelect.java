package com.tmarki.comicmaker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;





import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ImageSelect extends Dialog {
	private ListAdapter packImageSelectAdapter = null;
	private Context context = null;
	private CharSequence folderSelected = "";
	private PackHandler packhandler = null;
	Map<CharSequence, Vector<String>> externalImages = null;
    private BackPressedListener backListener = null;
    public String[] myStuff;
    public AdapterView.OnItemClickListener clickListener;
    public interface BackPressedListener { 
    	public void backPressed ();
    }
    final LayoutInflater mInflater;/* = (LayoutInflater) getContext()
            .getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);*/
	
	public ImageSelect(Context c, CharSequence folder, Map<CharSequence, Vector<String>> externals, BackPressedListener bpl, PackHandler ph) {
		super (c);
		context = c;
		folderSelected = folder;
		externalImages = externals;
		backListener = bpl;
		packhandler = ph;
		mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	}
	
    static class QueueItem {
		public String filename;
		public ImageLoadedListener listener;
	}	    
    public interface ImageLoadedListener {
		public void imageLoaded(String filename, SoftReference<BitmapDrawable> imageBitmap );
	}
    Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> mExternalImages = null;

	private void makeImageSelectAdapter (final String[] imageNames) {

		myStuff = imageNames;
		packImageSelectAdapter = new ArrayAdapter<String>(
				context, R.layout.image_select_row, imageNames) {
           
		
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
					String filename = imageNames[position];
					Bitmap bmp = packhandler.getDefaultPackDrawable(folderSelected.toString(), filename, 0, context.getAssets());
					if (bmp != null)
						tv.setImageBitmap(bmp);
					TextView title = (TextView) row.findViewById(R.id.title);
					if (title != null)
						title.setText(filename.replace('_', ' ').replace (".png", "").replace(".jpg", ""));
				}
				return row;
		    }
		};	
	}
	

	String[] filterMemes (String filt) {
		if (!folderSelected.equals(PackHandler.ALL_THE_FACES)) {
			List<String> sv = externalImages.get(folderSelected);
			if (filt.length() > 0) {
				boolean done = false;
				while (!done) {
					done = true;
					for (String ss : sv) {
						if (!ss.toLowerCase().contains(filt)) {
							sv.remove(ss);
							done = false;
							break;
						}
					}
				}
			}
			if (sv.size () > 0) {
				String[] s = new String[sv.size()];
				sv.toArray(s);
				return s;
			}
		}
		else {
			int ALL_LIMIT = 10000;
			int cnt = 0;
			outerloop:
			for (CharSequence fold : externalImages.keySet()) {
				for (String s : externalImages.get(fold)) {
					if (filt.length() == 0 || s.toLowerCase().contains(filt))
						cnt += 1;
					if (cnt >= ALL_LIMIT)
						break outerloop;
				}
			}
			if (cnt > 0) {
				String[] ret = new String[cnt];
				cnt = 0;
				outerloop:
				for (CharSequence fold : externalImages.keySet()) {
					for (String s : externalImages.get(fold)) {
						if (filt.length() == 0 || s.toLowerCase().contains(filt)) {
							ret[cnt] = s;
							cnt += 1;
						}
						if (cnt >= ALL_LIMIT)
							break outerloop;
					}
				}
				return ret;
			}
/*			sv = new Vector<String>();
			for (CharSequence cs : externalImages.keySet()) {
				if (cs.equals(folderSelected)) continue;
				sv.addAll(externalImages.get(cs));
			}
			if (folderSelected.equals("--ALL--") && sv.size() > 50)
				sv = sv.subList(0, 50);*/
		}
		return null;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.imageselect);
		setTitle(R.string.image_select_title);
		String[] s = filterMemes ("");
		final ListView lv = (ListView)findViewById(R.id.imageList);
		if (s != null/* && !folderSelected.equals("--ALL--")*/) {
			makeImageSelectAdapter(s);
			if (lv != null) {
				lv.setAdapter(packImageSelectAdapter);
				if (clickListener != null)
					lv.setOnItemClickListener(clickListener);
	/*			String[] filt = filteredMemes (sharedPref.getString("filter", ""));
				if (filt != null)
					setupMemes (lv, filt);*/
			}
		}
		EditText et = (EditText)findViewById(R.id.searchText);
		et.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String str = s.toString();
				String[] filtered = filterMemes(str);
				if (filtered != null) {
					makeImageSelectAdapter(filtered);
					lv.setAdapter(packImageSelectAdapter);
				}
				else if (packImageSelectAdapter != null) {
					lv.setAdapter(null);
				}
			}
		});
}
	

}
