package com.tmarki.comicmaker;

import java.util.Vector;

import com.example.blahblah.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FontSelect extends Dialog {
	private PackAdapter adapter = null;
	static public class PackAdapter extends BaseAdapter {
	    
		private int SampleHeight = 50;
	    private static LayoutInflater inflater=null;
	    private Activity activity = null;
	    private boolean Bold = false;
	    private boolean Italic = false;
	    private int maxFontHeight = 70;
	    public PackAdapter(Activity ac, int fontSize, int maxFS, boolean b, boolean i) {
	    	activity = ac;
	        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        SampleHeight = fontSize;
	        Bold = b;
	        Italic = i;
	        maxFontHeight = maxFS;
	    }

	    public int getCount() {
	        return TextObject.FontType.values().length;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }
	    
	    public static class ViewHolder{
	        public TextView text;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        View vi=convertView;
	        ViewHolder holder;
	        if(convertView==null){
	            vi = inflater.inflate(R.layout.fontselectrow, null);
	            holder=new ViewHolder();
	            holder.text=(TextView)vi.findViewById(R.id.rowText);
	            holder.text.setTypeface(TextObject.getTypefaceObj(TextObject.FontType.values()[position], Bold, Italic));
	            holder.text.setTextSize(SampleHeight);
	            holder.text.setMinHeight(maxFontHeight);
	            holder.text.setText(TextObject.FontType.values()[position].toString());
	        }
	        else
	            holder=(ViewHolder)vi.getTag();
	        return vi;
	    }

		public boolean isBold() {
			return Bold;
		}

		public void setBold(boolean bold) {
			Bold = bold;
		}

		public boolean isItalic() {
			return Italic;
		}

		public void setItalic(boolean italic) {
			Italic = italic;
		}
		
		public void setFontSize (int fontSize) {
			SampleHeight = fontSize;
		}
	}
	private Activity activity = null;
	private OnItemClickListener listener = null;
	private int fontSize = 20;
	private boolean Bold = false;
	private boolean Italic = false;
	private int minFontSize = 10;
	private int maxFontSize = 70;
/*	public int getFontSize () {
		return fontSize;
	}*/
	private OnCheckedChangeListener boldClicked = new OnCheckedChangeListener() {
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Bold = isChecked;
			adapter.setBold(isChecked);
	        ListView list = (ListView)findViewById(R.id.imageList);
	        adapter = new PackAdapter(activity, fontSize, maxFontSize, Bold, Italic);
	        list.setAdapter(adapter);
		}
	};
	private OnCheckedChangeListener italicClicked = new OnCheckedChangeListener() {
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Italic = isChecked;
			adapter.setItalic(isChecked);
	        ListView list = (ListView)findViewById(R.id.imageList);
	        adapter = new PackAdapter(activity, fontSize, maxFontSize, Bold, Italic);
	        list.setAdapter(adapter);
		}
	};
/*	private OnSeekBarChangeListener fontsizeChange = new OnSeekBarChangeListener() {
		
		public void onStopTrackingTouch(SeekBar seekBar) {
			
		}
		
		public void onStartTrackingTouch(SeekBar seekBar) {
			
		}
		
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			fontSize = progress + minFontSize;
	        ListView list = (ListView)findViewById(R.id.imageList);
	        adapter = new PackAdapter(activity, fontSize, maxFontSize, Bold, Italic);
			adapter.setFontSize(progress + minFontSize);
	        list.setAdapter(adapter);
		}
	};*/
	public FontSelect (Activity ac, OnItemClickListener listnr, int fontS, boolean b, boolean i) {
		super (ac);
		activity = ac;
		listener = listnr;
		fontSize = 20;
		Bold = b;
		Italic = i;
	}
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fontselect);
        ListView list = (ListView)findViewById(R.id.imageList);
        adapter = new PackAdapter(activity, fontSize, maxFontSize, Bold, Italic);
        list.setAdapter(adapter);
        list.setOnItemClickListener(listener);
        
        CheckBox bold = (CheckBox)findViewById(R.id.bold);
        bold.setChecked(Bold);
        bold.setOnCheckedChangeListener(boldClicked);
        CheckBox italic = (CheckBox)findViewById(R.id.italic);
        italic.setOnCheckedChangeListener(italicClicked);
        italic.setChecked(Italic);
/*        SeekBar fontsize = (SeekBar)findViewById(R.id.fontsize);
        fontsize.setMax (maxFontSize - minFontSize);
        fontsize.setOnSeekBarChangeListener(fontsizeChange);
        fontsize.setProgress(fontSize);*/
        super.setTitle(R.string.select_font);
    }
	public boolean isBold() {
		return Bold;
	}
	public void setBold(boolean bold) {
		Bold = bold;
	}
	public boolean isItalic() {
		return Italic;
	}
	public void setItalic(boolean italic) {
		Italic = italic;
	}


}
