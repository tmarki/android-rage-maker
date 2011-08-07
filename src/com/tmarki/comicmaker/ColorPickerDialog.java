/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmarki.comicmaker;

import com.tmarki.comicmaker.R;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class ColorPickerDialog extends Dialog {
    public interface OnColorChangedListener {
        void colorChanged(String key, int color);
    }

    private OnColorChangedListener mListener;
    private int mInitialColor, mDefaultColor;
    private String mKey;
    

	private static class ColorPickerView extends View {
		private Paint mPaint;
		private float mCurrentHue = 0;
		private int mCurrentX = 0, mCurrentY = 0;
		private int mCurrentColor, mDefaultColor;
		private OnColorChangedListener mListener;
		
	    private int hueBarLeft = 5;
	    private int hueBarTop = 0;
	    private int hueBarRight = 190;
	    private int hueBarBottom = 40;
	    
	    private int mainFieldLeft = 5;
	    private int mainFieldTop = 50;
	    private int mainFieldRight = 190;
	    private int mainFieldBottom = 200;

	    private int button1Left = 5;
	    private int button1Top = 205;
	    private int button1Right = 90;
	    private int button1Bottom = 250;

	    private int button2Left = 95;
	    private int button2Top = 205;
	    private int button2Right = 200;
	    private int button2Bottom = 250;

		ColorPickerView(Context c, OnColorChangedListener l, int color, int defaultColor) {
			super(c);
			mListener = l;
			mDefaultColor = defaultColor;

			// Get the current hue from the current color and update the main color field
			float[] hsv = new float[3];
			Color.colorToHSV(color, hsv);
			mCurrentHue = hsv[0];

			mCurrentColor = color;
			
			// Initializes the Paint that will draw the View
			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setTextAlign(Paint.Align.CENTER);
			mPaint.setTextSize(14);
		}
		
		private int getHueBarColor (int x) {
			int hueW = hueBarRight - hueBarLeft;
			x -= hueBarLeft;
			int normalX = ((x * 256) / hueW);
			int section = normalX / 43;
			int i = normalX;
			if (section != 0)
				i %= 43;
			i *= 6;
			switch (section) {
				case 0:
					return Color.rgb(255, 0, i);
				case 1:
					return Color.rgb(255- i, 0, 255);
				case 2:
					return Color.rgb(0, i, 255);
				case 3:
					return Color.rgb(0, 255, 255-i);
				case 4:
					return Color.rgb(i, 255, 0);
				case 5:
					return Color.rgb(255, 255-i, 0);
				default:
					return Color.rgb (0, 0, 0);
			}
		}

		
		private int getMainColor (int rawx, int rawy){
			int mfW = mainFieldRight - mainFieldLeft;
			int mfH = mainFieldBottom - mainFieldTop;
			int x = ((rawx - mainFieldLeft) * 255) / mfW;
			int y = ((rawy - mainFieldTop) * 255) / mfH;
			if (y == 0)
			{
				return Color.rgb(255 - (255-Color.red ((int)mCurrentHue))*x/255, 255-(255-Color.green((int)mCurrentHue))*x/255, 255-(255-Color.blue((int)mCurrentHue))*x/255);
			}
			int tc = getMainColor(rawx, mainFieldTop); 
			return Color.rgb((255-y)*Color.red(tc)/255, (255-y)*Color.green(tc)/255, (255-y)*Color.blue(tc)/255);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// Display all the colors of the hue bar with lines
			hueBarRight = getWidth() - hueBarLeft * 2;
			mainFieldRight = getWidth() - mainFieldLeft * 2;
			int bh = button1Bottom - button1Top;
			button1Top = getHeight() - bh - hueBarLeft;
			button1Right = getWidth() / 2 - button1Left;
			button2Top = getHeight() - bh - button1Left;
			button2Left = getWidth() / 2;
			button2Right = button2Left + button1Right - button1Left;
			button1Bottom = button1Top + bh;
			button2Bottom = button2Top + bh;
			mainFieldBottom = getHeight() - bh - hueBarLeft;
			
			for (int x=hueBarLeft; x<hueBarRight; x++)
			{
				int col = getHueBarColor(x);
				// If this is not the current selected hue, display the actual color
				if (mCurrentHue != col)
				{
					mPaint.setColor(col);
					mPaint.setStrokeWidth(1);
				}
				else // else display a slightly larger black line
				{
					mPaint.setColor(Color.BLACK);
					mPaint.setStrokeWidth(3);
				}
				canvas.drawLine(x, hueBarTop, x, hueBarBottom, mPaint);
			}

			// Display the main field colors using LinearGradient
			mPaint.setStrokeWidth(1);
			for (int x=mainFieldLeft; x<mainFieldRight; x++)
			{
				int[] colors = new int[2];
				colors[0] = getMainColor(x, mainFieldTop);//mMainColors[x];
				colors[1] = Color.BLACK;
				Shader shader = new LinearGradient(x, mainFieldTop, x, mainFieldBottom, colors, null, Shader.TileMode.REPEAT);
				mPaint.setShader(shader);
				canvas.drawLine(x, mainFieldTop, x, mainFieldBottom, mPaint);
			}
			mPaint.setShader(null);

			// Display the circle around the currently selected color in the main field
			if (mCurrentX != 0 && mCurrentY != 0)
			{
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setColor(Color.BLACK);
				canvas.drawCircle(mCurrentX, mCurrentY, 10, mPaint);
			}

			// Draw a 'button' with the currently selected color
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(mCurrentColor);
			canvas.drawRect(button1Left, button1Top, button1Right, button1Bottom, mPaint);

			// Set the text color according to the brightness of the color
			if (Color.red(mCurrentColor)+Color.green(mCurrentColor)+Color.blue(mCurrentColor) < 384)
				mPaint.setColor(Color.WHITE);
			else
				mPaint.setColor(Color.BLACK);
			canvas.drawText(getResources().getString(R.string.settings_bg_color_confirm), button1Left + (button1Right - button1Left) / 2, button1Top + (button1Bottom - button1Top) / 2, mPaint);

			// Draw a 'button' with the default color
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(mDefaultColor);
			canvas.drawRect(button2Left, button2Top, button2Right, button2Bottom, mPaint);

			// Set the text color according to the brightness of the color
			if (Color.red(mDefaultColor)+Color.green(mDefaultColor)+Color.blue(mDefaultColor) < 384)
				mPaint.setColor(Color.WHITE);
			else
				mPaint.setColor(Color.BLACK);
			canvas.drawText(getResources().getString(R.string.settings_default_color_confirm), button2Left + (button2Right - button2Left) / 2, button2Top + (button2Bottom - button2Top) / 2, mPaint);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
			float x = event.getX();
			float y = event.getY();

			// If the touch event is located in the hue bar
			if (x > hueBarLeft && x < hueBarRight && y > hueBarTop && y < hueBarBottom)
			{
				// Update the main field colors
				mCurrentHue = getHueBarColor((int)x);
				mCurrentColor = getMainColor(mCurrentX, mCurrentY);
				invalidate();
			}

			// If the touch event is located in the main field
			if (x > mainFieldLeft && x < mainFieldRight && y > mainFieldTop && y < mainFieldBottom)
			{
				mCurrentColor = getMainColor((int)x, (int)y);
				invalidate();
				mCurrentX = (int) x;
				mCurrentY = (int) y;
			}

			// If the touch event is located in the left button, notify the listener with the current color
			if (x > button1Left && x < button1Right && y > button1Top && y < button1Bottom)
				mListener.colorChanged("", mCurrentColor);

			// If the touch event is located in the right button, notify the listener with the default color
			if (x > button2Left && x < button2Right && y > button2Top && y < button2Bottom)
				mListener.colorChanged("", mDefaultColor);

			return true;
		}
	}

    public ColorPickerDialog(Context context, OnColorChangedListener listener, String key, int initialColor, int defaultColor) {
        super(context);

        mListener = listener;
        mKey = key;
        mInitialColor = initialColor;
        mDefaultColor = defaultColor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnColorChangedListener l = new OnColorChangedListener() {
            public void colorChanged(String key, int color) {
                mListener.colorChanged(mKey, color);
                dismiss();
            }
        };
 
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindow ().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ColorPickerView cpv = new ColorPickerView(getContext(), l, mInitialColor, mDefaultColor);
        setContentView(cpv);
        setTitle(R.string.settings_bg_color_dialog);
        
    }
}
