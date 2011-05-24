package com.tmarki.comicmaker;

import java.util.Vector;

import com.example.blahblah.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
	static public class PackAdapter extends BaseAdapter {
	    
		private int ThumbHeight = 50;
	    private static LayoutInflater inflater=null;
	    Activity activity = null;
	    Vector<CharSequence> files = new Vector<CharSequence>();
	    String packName = "";
	    String folderName = "";
	    public PackAdapter(Activity ac, String PackName, String FolderName, int screenH) {
	    	activity = ac;
	    	packName = PackName;
	    	folderName = FolderName;
	    	files = PackHandler.getBundles().get(PackName).get(FolderName);
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
	        View vi=convertView;
	        ViewHolder holder;
	        if(convertView==null){
	            vi = inflater.inflate(R.layout.row, null);
	            holder=new ViewHolder();
	            holder.text=(TextView)vi.findViewById(R.id.rowText);;
	            holder.image=(ImageView)vi.findViewById(R.id.rowImage);
	            vi.setTag(holder);
	        }
	        else
	            holder=(ViewHolder)vi.getTag();
	        String fn = files.get(position).toString();
	        if (fn.lastIndexOf('.') > 0)
	        	holder.text.setText(fn.substring(0, fn.lastIndexOf('.')));
	        else
	        	holder.text.setText(fn);
	        BitmapDrawable dr = PackHandler.getPackDrawable(packName, folderName, fn);
	        holder.image.setImageDrawable(dr);
	        holder.image.setAdjustViewBounds(true);
	        holder.image.setMaxHeight(ThumbHeight);
	        holder.image.setMaxWidth(ThumbHeight);
	        return vi;
	    }
	}
	private String PackName = "";
	private String FolderName = "";
	private Activity activity = null;
	private OnItemClickListener listener = null;
	private int screenHeight = 320;
	public ImageSelect (Activity ac, String pn, String fn, OnItemClickListener listnr, int scrnHeight) {
		super (ac);
		activity = ac;
		PackName = pn;
		FolderName = fn;
		listener = listnr;
		screenHeight = scrnHeight;
	}
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imageselect);
        ListView list = (ListView)findViewById(R.id.imageList);
        adapter = new PackAdapter(activity, PackName, FolderName, screenHeight);
        list.setAdapter(adapter);
        list.setOnItemClickListener(listener);
        super.setTitle(R.string.select_image);
    }

}
