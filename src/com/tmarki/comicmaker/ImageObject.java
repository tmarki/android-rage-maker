package com.tmarki.comicmaker;
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
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.util.Log;

public class ImageObject extends ProxyDrawable {
	
	public String pack = "";
	public String folder = "";
	public String filename = "";
    private Point mPosition = new Point ();
    private float mRotation = 0.0f;
    private float mScale = 0.0f;
    private boolean mSelected = false;
    private boolean InBack = true;
    private boolean flipVertical = false;
    private boolean flipHorizontal = false;
    private final int resizeBoxSize = 32;
    static boolean resizeMode = false; // admittedly this is not the nicest way to do it
    static void setResizeMode (boolean rm) {
    	resizeMode = rm;
    }
    static boolean interactiveMode = false;
    static void setInteractiveMode (boolean rm) {
    	interactiveMode = rm;
    }
    public boolean isInBack() {
		return InBack;
	}

	public void setInBack(boolean inBack) {
		InBack = inBack;
	}
	
	private int mDrawableId = -1;

    public ImageObject(Drawable target) {
        super(target);
    }
    
    public ImageObject(ImageObject other) {
    	super (other);
    	setProxy(other.getProxy());
        mPosition = new Point (other.mPosition);
        mRotation = other.mRotation;
        mScale = other.mScale;
        mSelected = other.mSelected;
        InBack = other.InBack;
        filename = other.filename;
        pack = other.pack;
        folder = other.folder;
    	
    }
    
    public ImageObject (Drawable target, int posX, int posY, float rot, float scale, int drawableId, String pac, String foldr, String fil) {
        super(target);
        mPosition.x = posX;
        mPosition.y = posY;
        mRotation = rot;
        mScale = scale;
        mDrawableId = drawableId;
        filename = fil;
        pack = pac;
        folder = foldr;
        target.setBounds(-target.getIntrinsicWidth() / 2, -target.getIntrinsicHeight() / 2, target.getIntrinsicWidth() / 2, target.getIntrinsicHeight() / 2);
        Log.d ("RAGE", "Initialized ImageObject at" + mPosition.toString());
    }
    
      
    public void moveBy (int x, int y) {
    	mPosition.x += x;
    	mPosition.y += y;
    }
    
    @Override
    public void draw(Canvas canvas) {
        Drawable dr = getProxy();
        if (dr != null) {
        	int sc = canvas.save();
        	canvas.translate(mPosition.x, mPosition.y);
        	canvas.scale( (float)mScale, (float)mScale);
        	int sc2 = canvas.save();
        	canvas.rotate((float)mRotation);
        	canvas.scale((flipHorizontal ? -1 : 1), (flipVertical ? -1 : 1));
            dr.draw(canvas);
            canvas.restoreToCount(sc2);
            if (mSelected && interactiveMode)
            {
            	Paint paint = new Paint ();
            	paint.setARGB(128, 128, 128, 128);
            	Rect imgrect = dr.getBounds(); 
            	canvas.drawRect(imgrect, paint);
            	Rect resizerect = new Rect ();
            	resizerect.set(imgrect.right - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.bottom - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.right, imgrect.bottom);
            	paint.setARGB(255, 0, 0, 0);
            	if (!resizeMode)
            		paint.setStyle(Style.STROKE);
            	paint.setStrokeWidth(2.0f);
            	canvas.drawRect(resizerect, paint);
            	canvas.drawText(String.valueOf(mRotation), 0, 0, paint);
            }
            canvas.restoreToCount(sc);
        }
    }
    
    public boolean pointIn(int x, int y){
        Drawable dr = getProxy();
        int wp2 = (int)(((float)dr.getBounds().width() / 2.0) * mScale);
        int hp2 = (int)((dr.getBounds().height() / 2.0) * mScale);
        return (x >= mPosition.x - wp2) && (x <= mPosition.x + wp2) &&
        	(y >= mPosition.y - hp2) && (y <= mPosition.y + hp2); 
    }

    public boolean pointInResize(int x, int y){
        Drawable dr = getProxy();
        int wp2 = (int)(((float)dr.getBounds().width() / 2.0) * mScale);
        int hp2 = (int)((dr.getBounds().height() / 2.0) * mScale);
        return (x >= mPosition.x + wp2 - resizeBoxSize) && (x <= mPosition.x + wp2) &&
        	(y >= mPosition.y + hp2 - resizeBoxSize) && (y <= mPosition.y + hp2); 
    }

	public Point getPosition() {
		return mPosition;
	}

	public void setPosition(Point Position) {
		this.mPosition = Position;
	}

	public float getRotation() {
		return mRotation;
	}

	public void setRotation(float Rotation) {
		this.mRotation = Rotation;
	}

	public float getScale() {
		return mScale;
	}

	public void setScale(float Scale) {
        Drawable dr = getProxy();
        Rect r = dr.getBounds();
		if (r.width() * Scale >= resizeBoxSize && r.height() * Scale >= resizeBoxSize)
			this.mScale = Scale;
	}

	public boolean isSelected() {
		if (mSelected){
//			getProxy().setState (new int [android.R.attr.state_selected]);
		}
		return mSelected;
	}

	public void setSelected(boolean Selected) {
		this.mSelected = Selected;
	}
	
	public int getDrawableId () {
		return mDrawableId;
	}
	public boolean isFlipVertical() {
		return flipVertical;
	}
	public void setFlipVertical(boolean flipVertical) {
		this.flipVertical = flipVertical;
	}
	public boolean isFlipHorizontal() {
		return flipHorizontal;
	}
	public void setFlipHorizontal(boolean flipHorizontal) {
		this.flipHorizontal = flipHorizontal;
	}
    
}
    
