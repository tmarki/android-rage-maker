package com.tmarki.comicmaker;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.tmarki.comicmaker.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;




public class ImageSelect extends Dialog {
	private PackAdapter adapter = null;
    private boolean backPressed = false;
    public interface ImageLoadedListenerOuter {
		public void imageLoaded();
	}
    public interface BackPressedListener { 
    	public void backPressed ();
    }
	static public class PackAdapter extends BaseAdapter {
	    
		private int ThumbHeight = 50;
	    private static LayoutInflater inflater=null;
	    private final Map<String, SoftReference<BitmapDrawable>> drawableMap = new HashMap<String, SoftReference<BitmapDrawable>> ();
	    private final Map<String, ViewHolder> viewMap = new HashMap<String, ViewHolder> ();
	    private Thread thread = new Thread ();
	    private ImageLoadedListenerOuter loadnotify = null;
	    Activity activity = null;
	    Vector<CharSequence> files = new Vector<CharSequence>();
	    String packName = "";
	    String folderName = "";
	    private final class QueueItem {
			public String filename;
			public ImageLoadedListener listener;
//			public ImageView view;
		}	    
	    public interface ImageLoadedListener {
			public void imageLoaded(String filename, SoftReference<BitmapDrawable> imageBitmap );
		}
	    Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> mExternalImages = null;
	    private final ArrayList<QueueItem> Queue = new ArrayList<QueueItem>();
	    private QueueRunner runner = new QueueRunner();

		private final Handler handler = new Handler();	// Assumes that this is started from the main (UI) thread
	    public PackAdapter(Activity ac, String PackName, String FolderName, int screenH, Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> externalImages, ImageLoadedListenerOuter imagel) {
	    	loadnotify = imagel;
	    	activity = ac;
	    	packName = PackName;
	    	folderName = FolderName;
	    	files = externalImages.get(PackName).get(FolderName);
	        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        ThumbHeight = screenH / 4;
	    }

	    public int getCount() {
	        return files.size();
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }
	    
	    public static class ViewHolder{
	        public TextView text;
	        public ImageView image;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        String fn = files.get(position).toString();
	        View vi=convertView;
	        ViewHolder holder;
	        if(convertView==null)
	        {
	            vi = inflater.inflate(R.layout.row, null);
	            holder=new ViewHolder();
	            holder.text=(TextView)vi.findViewById(R.id.rowText);;
	            holder.image=(ImageView)vi.findViewById(R.id.rowImage);
	            vi.setTag(holder);
	            viewMap.remove(fn);
	        }
	        else
	            holder=(ViewHolder)vi.getTag();
	        if (fn.lastIndexOf('.') > 0)
	        	holder.text.setText(fn.substring(0, fn.lastIndexOf('.')));
	        else
	        	holder.text.setText(fn);
	        if (drawableMap.containsKey(fn))
	        	holder.image.setImageDrawable(drawableMap.get (fn).get ());
	        else
	        	holder.image.setImageResource(R.drawable.loading);
			holder.image.setAdjustViewBounds(true);
			holder.image.setMaxHeight(ThumbHeight);
			holder.image.setMaxWidth(ThumbHeight);
			if (!viewMap.containsKey(fn)) {
		        viewMap.put(fn, holder);
		        QueueItem item = new QueueItem();
				item.filename =fn;
				item.listener = new ImageLoadedListener() {
					public void imageLoaded(String fn, SoftReference<BitmapDrawable> imageBitmap) {
						if (imageBitmap.get() != null) {
							ViewHolder h = viewMap.get(fn);
							h.image.setImageDrawable(imageBitmap.get());
							h.image.setAdjustViewBounds(true);
							h.image.setMaxHeight(ThumbHeight);
							h.image.setMaxWidth(ThumbHeight);
							if (loadnotify != null)
								loadnotify.imageLoaded();
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
//			item.view = holder.image;
//	        fetchDrawableOnThread(fn, holder.image);
/*	        BitmapDrawable dr = PackHandler.getPackDrawable(packName, folderName, fn);
	        holder.image.setImageDrawable(dr);
	        holder.image.setAdjustViewBounds(true);
	        holder.image.setMaxHeight(ThumbHeight);
	        holder.image.setMaxWidth(ThumbHeight);*/
	        return vi;
	    }
	    private class QueueRunner implements Runnable {
			public void run() {
				synchronized(this) {
					while(Queue.size() > 0) {
						final QueueItem item = Queue.remove(0);

						// If in the cache, return that copy and be done
						if( drawableMap.containsKey(item.filename.toString()) && drawableMap.get(item.filename.toString()) != null) {
							// Use a handler to get back onto the UI thread for the update
							handler.post(new Runnable() {
								public void run() {
									if( item.listener != null ) {
										// NB: There's a potential race condition here where the cache item could get
										//     garbage collected between when we post the runnable and it's executed.
										//     Ideally we would re-run the network load or something.
										SoftReference<BitmapDrawable> ref = drawableMap.get(item.filename.toString());
										if( ref != null ) {
/*											item.view.setImageDrawable(ref);
							                item.view.setAdjustViewBounds(true);
							                item.view.setMaxHeight(ThumbHeight);
							                item.view.setMaxWidth(ThumbHeight);*/
											item.listener.imageLoaded(item.filename, ref);
										}
									}
								}
							});
						} else {
							final SoftReference<BitmapDrawable> bmp = new SoftReference<BitmapDrawable>(PackHandler.getPackDrawable(packName, folderName, item.filename));
							if( bmp != null ) {
								drawableMap.put(item.filename.toString(), bmp);

								// Use a handler to get back onto the UI thread for the update
								handler.post(new Runnable() {
									public void run() {
										if( item.listener != null) {
/*											item.view.setImageDrawable(bmp);
//							                item.view.setAdjustViewBounds(true);
							                item.view.setMaxHeight(ThumbHeight);
							                item.view.setMaxWidth(ThumbHeight);*/
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
	    public void fetchDrawableOnThread(final String fileName, final ImageView imageView) {
	        if (drawableMap.containsKey(fileName)) {
	            imageView.setImageDrawable(drawableMap.get(fileName).get ());
	            return;
	        }

	        final Handler handler = new Handler() {
	            @Override
	            public void handleMessage(Message message) {
	                imageView.setImageDrawable((Drawable) message.obj);
	            }
	        };

	        Thread thread = new Thread() {
	            @Override
	            public void run() {
	                Drawable drawable = PackHandler.getPackDrawable(packName, folderName, fileName);
	                if (drawable != null) {
//	                	drawableMap.put(fileName, drawable);
	                	Message message = handler.obtainMessage(1, drawable);
	                	handler.sendMessage(message);
	                }
	            }
	        };
	        thread.setPriority(Thread.MIN_PRIORITY);
	        thread.start();
	    }
	}
	private String PackName = "";
	private String FolderName = "";
	private Activity activity = null;
	private OnItemClickListener listener = null;
	private int screenHeight = 320;
	private Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> mExternalImages = null;
	private BackPressedListener backListener = null;
	public ImageSelect (Activity ac, Map<CharSequence, Map<CharSequence, Vector<CharSequence>>> externalImages, String pn, String fn, OnItemClickListener listnr, int scrnHeight, BackPressedListener bpl) {
		super (ac);
		activity = ac;
		PackName = pn;
		FolderName = fn;
		listener = listnr;
		screenHeight = scrnHeight;
		mExternalImages = externalImages;
		backListener = bpl;
	}
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imageselect);
        final ListView list = (ListView)findViewById(R.id.imageList);
        adapter = new PackAdapter(activity, PackName, FolderName, screenHeight, mExternalImages, new ImageLoadedListenerOuter() {
			public void imageLoaded() {
			}
		});
        list.setAdapter(adapter);
        list.setOnItemClickListener(listener);
        super.setTitle(R.string.select_image);
    }
	@Override
	public void onBackPressed() {
		backPressed = true;
		if (backListener != null)
			backListener.backPressed();
		super.onBackPressed();
	}
	public boolean wasBackPressed () {
		return backPressed;
	}
}
