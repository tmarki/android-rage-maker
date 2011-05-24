/*
 * Copyright 2010 QuietlyCoding <mike@quietlycoding.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tmarki.comicmaker;

import com.example.blahblah.R;
import com.tmarki.comicmaker.ColorPickerDialog.OnColorChangedListener;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;

public class Picker extends Dialog implements Button.OnClickListener
{
    public interface OnWidthChangedListener {
        void widthChanged(int width);
    }
    private OnWidthChangedListener mListener;
    private int origWidth = 3;
	public Picker (Context context, OnWidthChangedListener l, int origW) {
		super(context);
		mListener = l;
		origWidth = origW;
//        setContentView(R.layout.picker);
	}
    /** Called when the activity is first created. */
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