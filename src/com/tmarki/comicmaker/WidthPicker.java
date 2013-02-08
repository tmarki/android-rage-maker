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
        setTitle(R.string.select_width);
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