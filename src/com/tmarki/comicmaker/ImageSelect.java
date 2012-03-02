package com.tmarki.comicmaker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;



import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ImageSelect {
	private ListAdapter packImageSelectAdapter = null;
	private Context context = null;
	private CharSequence packSelected = "";
	private CharSequence folderSelected = "";
	private PackHandler packhandler = null;
	Map<CharSequence, Vector<String>> externalImages = null;
    private Thread thread = new Thread ();
    private BackPressedListener backListener = null;
    public interface BackPressedListener { 
    	public void backPressed ();
    }
    final LayoutInflater mInflater;/* = (LayoutInflater) getContext()
            .getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);*/
	
	public ImageSelect(Context c, CharSequence pack, CharSequence folder, Map<CharSequence, Vector<String>> externals, BackPressedListener bpl, PackHandler ph) {
		context = c;
		packSelected = pack;
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
    private  final ArrayList<QueueItem> Queue = new ArrayList<QueueItem>();
    private QueueRunner runner = new QueueRunner();
    public Map<String, SoftReference<BitmapDrawable>> drawableMap = new HashMap<String, SoftReference<BitmapDrawable>> ();
	private final Handler handler = new Handler();	// Assumes that this is started from the main (UI) thread
	private Vector<ViewHolder> holders = new Vector<ViewHolder>();

    class QueueRunner implements Runnable {
		public void run() {
			synchronized(this) {
				while(Queue.size() > 0) {
					final QueueItem item = Queue.remove(0);
					if (item == null)
						break;

					if( drawableMap.containsKey(item.filename.toString()) && drawableMap.get(item.filename.toString()) != null) {
						handler.post(new Runnable() {
							public void run() {
								if( item.listener != null) {
									SoftReference<BitmapDrawable> ref = drawableMap.get(item.filename.toString());
									if( ref != null && ref.get () != null && ref.get ().getBitmap() != null && !ref.get().getBitmap().isRecycled()) {
										item.listener.imageLoaded(item.filename, ref);
									}
								}
							}
						});
					} else if (packhandler != null && packSelected != null && folderSelected != null) {
						Bitmap src = packhandler.getDefaultPackDrawable(folderSelected.toString(), item.filename, 0, context.getAssets());
						if (src != null && src.getWidth() > 0 && src.getHeight() > 0) {
							Bitmap b = src;//Bitmap.createScaledBitmap(src, 96 * src.getWidth() / src.getHeight(), 96, true);
							if (b == null || b.isRecycled())
								continue;
							final SoftReference<BitmapDrawable> bmp = new SoftReference<BitmapDrawable>(new BitmapDrawable (b));
							if( bmp != null ) {
								handler.post(new Runnable() {
									public void run() {
										if( item.listener != null) {
											item.listener.imageLoaded(item.filename, bmp);
										}
									}
								});
							}
						}

					}

				}
			}
		}
	}
    static class ViewHolder {
        ImageView icon;
        TextView title;
}

	private void makeImageSelectAdapter (final String[] imageNames) {

		packImageSelectAdapter = new ArrayAdapter<String>(
				context, R.layout.image_select_row, imageNames) {
           
		    ViewHolder holder;
		
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
					String filename = externalImages.get(folderSelected).get(position);
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

	public void showImageSelect (DialogInterface.OnClickListener ocl) {
		Vector<String> sv = externalImages.get(folderSelected);
		String[] s = new String[sv.size()];
		sv.toArray(s);
		makeImageSelectAdapter(s);
	    AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.image_select_title));
        builder.setAdapter(packImageSelectAdapter, ocl);
        builder.setOnKeyListener(new OnKeyListener() {
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && backListener != null) {
					dialog.dismiss();
					backListener.backPressed();
//					cleanUp();
				}
				return false;
			}
		});
        AlertDialog alert = builder.create();
        alert.show();


	}
	
	public void cleanUp () {
		for (SoftReference<BitmapDrawable> bd : drawableMap.values()){
		}
		drawableMap.clear();
		
	}

}
