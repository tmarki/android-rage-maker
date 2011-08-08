package com.tmarki.comicmaker;

import com.tmarki.comicmaker.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class WidthPicker extends Dialog implements Button.OnClickListener
{
    public interface OnWidthChangedListener {
        void widthChanged(int width);
    }
    private OnWidthChangedListener mListener;
    private int origWidth = 3;
	public WidthPicker (Context context, OnWidthChangedListener l, int origW) {
		super(context);
		mListener = l;
		origWidth = origW;
	}
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picker);
        setTitle("Pick a Width");
        Button b = (Button)findViewById(R.id.width_ok);
        b.setOnClickListener(this);
		SeekBar sb = (SeekBar)findViewById(R.id.width_bar);
		sb.setProgress(origWidth);
    }
	public void onClick(View v) {
		SeekBar sb = (SeekBar)findViewById(R.id.width_bar);
		mListener.widthChanged(sb.getProgress());
		dismiss ();
    }
}