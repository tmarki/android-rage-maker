package com.tmarki.comicmaker;

import com.tmarki.comicmaker.R;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


import android.app.Dialog;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class ComicSettings extends Dialog implements OnSeekBarChangeListener {
	final int maxPanelCount = 20;
	private int curPanelCount = 5;
	private boolean curDrawGrid = true;
	private boolean curShowAd = true;
	private View.OnClickListener okListener = null;

	public ComicSettings(Context context, int startPanelCount, boolean startDrawGrid, boolean startShowAd, View.OnClickListener oklistener) {
		super(context);
		curPanelCount = startPanelCount - 1;
		curDrawGrid = startDrawGrid;
		curShowAd = startShowAd;
		okListener = oklistener;
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
