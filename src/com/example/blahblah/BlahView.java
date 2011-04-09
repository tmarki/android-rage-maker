package com.example.blahblah;


import java.util.Vector;
import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;


public class BlahView extends View {
    private Vector<ImageObject> mDrawables = new Vector<ImageObject>();

    public BlahView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

        Drawable dr = context.getResources().getDrawable(R.drawable.trollface);
        addImageObject(dr, 0, 0, 0, 500, false);
        dr = context.getResources().getDrawable(R.drawable.awesome);
        addImageObject(dr, 0, 200, 0, 400, false);
        dr = context.getResources().getDrawable(R.drawable.icon);
        addImageObject(dr, 0, 400, 0, 200, true);
        
    }
    
    private void addImageObject (Drawable dr, int a, int b, int c, int d, boolean noanim) {
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        
        Animation an = new TranslateAnimation(a, b, c, d);
        an.setDuration(4000);
        an.setRepeatCount(-1);
        an.initialize(10, 10, 10, 10);
        Random r = new Random();
//        mDrawables.add(new ImageObject(dr, an, r.nextInt(480), r.nextInt(800)));
        mDrawables.add(new ImageObject(dr, an, 0, 0));
        an.startNow();
    }
    
    @Override 
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        for (ImageObject ad : mDrawables) {
        	if (ad != null) {
        		ad.draw(canvas);
        	}
        }
        invalidate();
    }

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		Log.d("RAGE", event.toString ());
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			for (int i = mDrawables.size() - 1; i >= 0; --i) {
				ImageObject io = mDrawables.elementAt(i);
				if (io.pointIn ((int) event.getX(), (int) event.getY())){
					Log.d ("RAGE", "Point in!" + io.toString());
					io.toggleSelected();
					break;
				}
			}
		}
		return true;
		//return super.onTouchEvent(event);
	}
    
}