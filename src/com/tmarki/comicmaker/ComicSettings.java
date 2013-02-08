//    Rage Comic Maker for Android (c) Tamas Marki 2011-2013
//	  This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.tmarki.comicmaker;

import java.util.Arrays;

import com.tmarki.comicmaker.R;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;


import android.app.Dialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.widget.CheckBox;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class ComicSettings extends Dialog implements OnSeekBarChangeListener {
	final int maxPanelCount = 20;
	private int curPanelCount = 5;
	private boolean curDrawGrid = true;
	private boolean curShowAd = true;
	private String curFormat = "JPG";
	private int orient = 0;
	private View.OnClickListener okListener = null;
    String[] cs = { "JPG", "PNG" };

	public ComicSettings(Context context, int startPanelCount, boolean startDrawGrid, boolean startShowAd, int ori, String format, View.OnClickListener oklistener) {
		super(context);
		curPanelCount = startPanelCount - 1;
		curDrawGrid = startDrawGrid;
		curShowAd = startShowAd;
		okListener = oklistener;
		curFormat = format;
		orient = ori;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_title);
        setContentView(R.layout.settings);
        SeekBar pc = (SeekBar)findViewById(R.id.panelCount);
        pc.setMax(maxPanelCount - 1);
        pc.setProgress(curPanelCount);
        pc.setOnSeekBarChangeListener(this);
        setPCLabel(curPanelCount);
        CheckBox dg = (CheckBox)findViewById(R.id.drawGrid);
        dg.setChecked(curDrawGrid);
//        CheckBox dg2 = (CheckBox)findViewById(R.id.showAds);
//        dg2.setChecked(curShowAd);
        Button ok = (Button)findViewById(R.id.settingsOk);
        ok.setOnClickListener(okListener);
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        Spinner sp = (Spinner)findViewById(R.id.orientSpin);
        if (sp != null) {
        	String[] ss = new String[3];
        	ss[0] = getContext().getResources().getString(R.string.rotate_auto);
        	ss[1] = getContext().getResources().getString(R.string.rotate_portrait);
        	ss[2] = getContext().getResources().getString(R.string.rotate_landscape);
        	sp.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, ss));
        }
        setOrientation(orient);
        sp = (Spinner)findViewById(R.id.saveFormat);
        if (sp != null) {
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, cs);
    		sp.setAdapter(adapter);
        }
        setSaveFormat (curFormat);
	}
	public int getPanelCount () {
		return curPanelCount + 1;
	}
	public boolean getDrawGrid () {
        CheckBox dg = (CheckBox)findViewById(R.id.drawGrid);
		return dg.isChecked(); 
	}
	public boolean getShowAd () {
//        CheckBox dg = (CheckBox)findViewById(R.id.showAds);
//		return dg.isChecked();
		return true;
	}
	public int getOrientation () {
        Spinner sp = (Spinner)findViewById(R.id.orientSpin);
        if (sp != null) {
        	if (sp.getSelectedItem().equals(getContext().getResources().getString(R.string.rotate_auto)))
        		return 0;
        	else if (sp.getSelectedItem().equals(getContext().getResources().getString(R.string.rotate_portrait)))
        		return 1;
        	else if (sp.getSelectedItem().equals(getContext().getResources().getString(R.string.rotate_landscape)))
        		return 2;
        }
        return 0;
	}
	private void setOrientation (int i) {
        Spinner sp = (Spinner)findViewById(R.id.orientSpin);
        if (sp != null) {
        	sp.setSelection(i);
        }
	}
	
	public String getSaveFormat () {
        Spinner sp = (Spinner)findViewById(R.id.saveFormat);
        if (sp != null) {
    		return sp.getSelectedItem().toString();
        }
        return "";
	}
	
	private void setSaveFormat (String form) {
        Spinner sp = (Spinner)findViewById(R.id.saveFormat);
        if (sp != null) {
        	int ind = 0;
        	for (int i = 0; i < cs.length; ++i) {
        		if (cs[i].equals (form)) {
        			ind = i;
        			break;
        		}
        	}
        	sp.setSelection(ind);
        }
	}
	
	private void setPCLabel (int val) {
		TextView tv = (TextView)findViewById(R.id.panelCountLabel);
		tv.setText(getContext ().getResources ().getString (R.string.panel_count) + " " + String.valueOf(val + 1));
		curPanelCount = val;
	}
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
        setPCLabel(progress);
	}
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}

}
