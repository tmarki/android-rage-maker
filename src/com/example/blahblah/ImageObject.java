package com.example.blahblah;
/*
 * Copyright (C) 2008 The Android Open Source Project
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



import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.animation.Animation;
import android.graphics.Point;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.util.Log;

public class ImageObject extends ProxyDrawable {
    
    private Animation mAnimation;
    private Transformation mTransformation = new Transformation();
    private Point mPosition = new Point ();
    private boolean mSelected = false;

    public ImageObject(Drawable target) {
        super(target);
    }
    
    public ImageObject(Drawable target, Animation animation, int posX, int posY) {
        super(target);
        mAnimation = animation;
        mPosition.x = posX;
        mPosition.y = posY;
    }
    
    public Animation getAnimation() {
        return mAnimation;
    }
    
    public void setAnimation(Animation anim) {
        mAnimation = anim;
    }

    public boolean hasStarted() {
        return mAnimation != null && mAnimation.hasStarted();
    }
    
    public boolean hasEnded() {
        return mAnimation == null || mAnimation.hasEnded();
    }
    
    public boolean toggleSelected(){
    	mSelected = !mSelected;
    	return mSelected;
    }
    
    @Override
    public void draw(Canvas canvas) {
        Drawable dr = getProxy();
        if (dr != null) {
        	int sc = canvas.save();
            Animation anim = new TranslateAnimation(mPosition.x, 0, mPosition.y, 0);
            if (anim != null) {
                anim.getTransformation(
                                    0,
                                    mTransformation);
            }
            canvas.concat(mTransformation.getMatrix());
            dr.draw(canvas);
            if (mSelected)
            {
            	Paint paint = new Paint ();
            	paint.setARGB(128, 128, 128, 128);
            	canvas.drawRect(dr.getBounds(), paint);
            }
            canvas.restoreToCount(sc);
        }
    }
    
    public boolean pointIn(int x, int y){
        Drawable dr = getProxy();
        Log.d ("RAGE", "points to test: " + String.valueOf(x) + ", " + String.valueOf(y));
        Log.d ("RAGE", "Bounds: " + String.valueOf(mPosition.x) + ", " + String.valueOf(mPosition.y) + ", " + String.valueOf(dr.getBounds().width()) + ", " + String.valueOf(dr.getBounds().height()));
        return (x >= mPosition.x) && (x <= mPosition.x + dr.getBounds().width()) &&
        	(y >= mPosition.y) && (y <= mPosition.y + dr.getBounds().height()); 
    }
    
}
    
