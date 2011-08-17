package com.tmarki.comicmaker;

import com.tmarki.comicmaker.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import com.tmarki.comicmaker.ComicEditor;

public class ZoomPicker extends Dialog implements Button.OnClickListener, SeekBar.OnSeekBarChangeListener
{
    public interface OnZoomChangedListener {
        void zoomChanged(float zoom);
    }
    private OnZoomChangedListener mListener;
    private float origZoom = 1.0f;
	public ZoomPicker (Context context, OnZoomChangedListener l, float origZ) {
		super(context);
		mListener = l;
		origZoom = origZ;
	}
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zoompicker);
        setTitle("Pick zoom level: " + String.format("%.0f", origZoom * 100) + "%  ");
        Button b = (Button)findViewById(R.id.zoom_ok);
        b.setOnClickListener(this);
		SeekBar sb = (SeekBar)findViewById(R.id.zoom_bar);
		sb.setProgress((int)((origZoom - ComicEditor.CANVAS_SCALE_MIN) * 100.0f));
		sb.setOnSeekBarChangeListener(this);
    }
	public void onClick(View v) {
		SeekBar sb = (SeekBar)findViewById(R.id.zoom_bar);
		mListener.zoomChanged((sb.getProgress()) / 100.0f + ComicEditor.CANVAS_SCALE_MIN);
		dismiss ();
    }
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
        setTitle("Pick zoom level: " + String.format("%.0f", progress + ComicEditor.CANVAS_SCALE_MIN * 100.0f) + "%");
		
	}
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}
}