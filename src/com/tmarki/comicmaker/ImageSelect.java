package com.tmarki.comicmaker;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
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
	Map<CharSequence, Map<CharSequence, Vector<String>>> externalImages = null;
    private Thread thread = new Thread ();
    private BackPressedListener backListener = null;
    public interface BackPressedListener { 
    	public void backPressed ();
    }
	
	public ImageSelect(Context c, CharSequence pack, CharSequence folder, Map<CharSequence, Map<CharSequence, Vector<String>>> externals, BackPressedListener bpl) {
		context = c;
		packSelected = pack;
		folderSelected = folder;
		externalImages = externals;
		backListener = bpl;
	}
	
    class QueueItem {
		public String filename;
		public ImageLoadedListener listener;
	}	    
    public interface ImageLoadedListener {
		public void imageLoaded(String filename, SoftReference<BitmapDrawable> imageBitmap );
	}
    Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> mExternalImages = null;
    private final ArrayList<QueueItem> Queue = new ArrayList<QueueItem>();
    private QueueRunner runner = new QueueRunner();
    private Map<String, SoftReference<BitmapDrawable>> drawableMap = new HashMap<String, SoftReference<BitmapDrawable>> ();
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
								if( item.listener != null ) {
									SoftReference<BitmapDrawable> ref = drawableMap.get(item.filename.toString());
									if( ref != null ) {
										item.listener.imageLoaded(item.filename, ref);
									}
								}
							}
						});
					} else {
						final SoftReference<BitmapDrawable> bmp = new SoftReference<BitmapDrawable>(PackHandler.getPackDrawable(packSelected.toString(), folderSelected.toString(), item.filename));
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
    class ViewHolder {
        ImageView icon;
        TextView title;
}

	private void makeImageSelectAdapter (final String[] imageNames) {

		packImageSelectAdapter = new ArrayAdapter<String>(
				context, R.layout.image_select_row, imageNames) {
           
		    ViewHolder holder;
		
		    public View getView(int position, View convertView,
		                    ViewGroup parent) {
		    		String fn = imageNames[position];
		            final LayoutInflater inflater = (LayoutInflater) getContext()
		                            .getSystemService(
		                                            Context.LAYOUT_INFLATER_SERVICE);
		
		            if (convertView == null) {
		                    convertView = inflater.inflate(
		                                    R.layout.image_select_row, null);
		
		                    holder = new ViewHolder();
		                    holder.icon = (ImageView) convertView
		                                    .findViewById(R.id.icon);
		                    holder.title = (TextView) convertView
		                                    .findViewById(R.id.title);
		                    convertView.setTag(holder);
		    	            holder.icon.setTag(R.id.filename, fn);
		            } else {
		                    holder = (ViewHolder) convertView.getTag();
		    	            holder.icon.setTag(R.id.filename, fn);
		            }              
		
		            Drawable tile = null;
		            
		            if (drawableMap.containsKey(fn)) {
		            	tile = drawableMap.get(fn).get ();
		            	if (tile == null) {
		            		drawableMap.remove(fn);
		            	}
		            }
		            
	            
		            holder.title.setText(fn);
		            if (tile != null)
		            	holder.icon.setImageDrawable(tile);
		            else
		            	holder.icon.setImageResource(R.drawable.loading);
					if (!holders.contains(holder) || !drawableMap.containsKey(fn)) {
						if (!holders.contains(holder))
							holders.add(holder);
				        QueueItem item = new QueueItem();
						item.filename =fn;
						item.listener = new ImageLoadedListener() {
							public void imageLoaded(String ffn, SoftReference<BitmapDrawable> imageBitmap) {
								if (imageBitmap.get() != null) {
									drawableMap.put(ffn, imageBitmap);
									ViewHolder h = null;
									for (int i = 0; i < holders.size(); ++i) {
										if ((String)holders.get (i).icon.getTag (R.id.filename) == ffn) {
											h = holders.get (i);
											break;
										}
									}
									if (h != null) {
										h.icon.setImageDrawable(imageBitmap.get());
										h.icon.setAdjustViewBounds(true);
									}
								}
								
							}
			
						};
						Queue.add(item);
						if( thread.getState() == Thread.State.NEW) {
							thread.start();
						} else if( thread.getState() == Thread.State.TERMINATED) {
							thread = new Thread(runner);
							thread.start();
						}	        
					}
		
		            return convertView;
		    }
		};	
	}

	public void showImageSelect (DialogInterface.OnClickListener ocl) {
		Vector<String> sv = externalImages.get (packSelected).get(folderSelected);
		String[] s = new String[sv.size()];
		sv.toArray(s);
		makeImageSelectAdapter(s);
	    AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.image_select_title));
        builder.setAdapter(packImageSelectAdapter, ocl);
        builder.setOnKeyListener(new OnKeyListener() {
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && backListener != null) {
					backListener.backPressed();
				}
				return false;
			}
		});
        AlertDialog alert = builder.create();
        alert.show();


	}

}
