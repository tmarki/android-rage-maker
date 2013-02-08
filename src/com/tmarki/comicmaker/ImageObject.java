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



import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.util.Log;

public class ImageObject {
	
	public String pack = "";
	public String folder = "";
	public String filename = "";
	public boolean locked = false;
	public Bitmap padlock = null;
    protected Point mPosition = new Point ();
    protected float mRotation = 0.0f;
    protected float mScale = 1.0f;
    protected boolean mSelected = false;
    protected boolean InBack = true;
    protected boolean flipVertical = false;
    protected boolean flipHorizontal = false;
    protected final int resizeBoxSize = 32;
    static public final int maxImageWidth = 640;
    static public final int maxImageHeight = 500;
    static boolean resizeMode = false; // admittedly this is not the nicest way to do it
    protected Bitmap content = null; 
    static void setResizeMode (boolean rm) {
    	resizeMode = rm;
    }
    static boolean interactiveMode = false;
    static void setInteractiveMode (boolean rm) {
    	interactiveMode = rm;
    }
    public void recycle () {
        Log.w ("RAGE", "RECYCLE ImageObject at" + mPosition.toString());
        if (!content.isRecycled())
        	content.recycle();
    }
    public boolean isInBack() {
		return InBack;
	}

    public Bitmap getContentBitmap () {
    	return content;
    }
	public void setInBack(boolean inBack) {
		InBack = inBack;
	}
	
	private int mDrawableId = -1;

    public ImageObject(Bitmap target) {
    	content = target;
        imageSizeCheck();
    }
    
    protected ImageObject () {
    	
    }
    public ImageObject(ImageObject other) {
    	if (other != null) {
    		content = other.content;
            imageSizeCheck();
	        mPosition = new Point (other.mPosition);
	        mRotation = other.mRotation;
	        mScale = other.mScale;
	        mSelected = other.mSelected;
	        InBack = other.InBack;
	        filename = other.filename;
	        pack = other.pack;
	        folder = other.folder;
    	}
    }
    
    public ImageObject (Bitmap target, int posX, int posY, float rot, float scale, int drawableId, String pac, String foldr, String fil) {
        content = target;
        imageSizeCheck();
        mPosition.x = posX;
        mPosition.y = posY;
        mRotation = rot;
        mScale = scale;
        mDrawableId = drawableId;
        filename = fil;
        pack = pac;
        folder = foldr;
        Log.w ("RAGE", "Initialized ImageObject at" + mPosition.toString());
    }
    
    private void imageSizeCheck () {
        if (content.getWidth() > maxImageWidth) {
        	int newWidth = maxImageWidth;
        	int newHeight = (content.getHeight() * newWidth) / content.getWidth();
            float scaleWidth = ((float) newWidth) / content.getWidth();
            float scaleHeight = ((float) newHeight) / content.getHeight();
        	Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
//            content = Bitmap.createBitmap(content, 0, 0, content.getWidth(), content.getHeight(), matrix, true);
            content = Bitmap.createScaledBitmap(content, newWidth, newHeight, true);
        }
        if (content.getHeight() > maxImageHeight) {
        	int newHeight = maxImageHeight;
        	int newWidth = (content.getWidth() * newHeight) / content.getHeight();
            float scaleWidth = ((float) newWidth) / content.getWidth();
            float scaleHeight = ((float) newHeight) / content.getHeight();
        	Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
//            content = Bitmap.createBitmap(content, 0, 0, content.getWidth(), content.getHeight(), matrix, true);         	
            content = Bitmap.createScaledBitmap(content, newWidth, newHeight, true);
        }
    }
    
    public int getWidth () {
    	if (content != null)
    		return content.getWidth();
    	else
    		return 0;
    }
    
    public int getHeight () {
    	if (content != null)
    		return content.getHeight();
    	else
    		return 0;
    }
    
      
    public void moveBy (int x, int y) {
    	mPosition.x += x;
    	mPosition.y += y;
    }
    
	Paint paint = new Paint ();
    public void draw(Canvas canvas) {
    	paint.setAntiAlias(true);
    	paint.setFilterBitmap(true);
    	//paint.setDither(true);
    	int sc = canvas.save();
    	try {
	    	canvas.translate(mPosition.x, mPosition.y);
	    	canvas.scale( (float)mScale, (float)mScale);
	    	int sc2 = canvas.save();
	    	canvas.rotate((float)mRotation);
	    	canvas.scale((flipHorizontal ? -1 : 1), (flipVertical ? -1 : 1));
	//            dr.draw(canvas);
	    	canvas.drawBitmap(content, -getWidth() / 2, -getHeight() / 2, paint);
	        canvas.restoreToCount(sc2);
        	Rect imgrect = new Rect(-getWidth() / 2, -getHeight() / 2, getWidth() / 2, getHeight() / 2);
	        if (mSelected && interactiveMode)
	        {
	        	paint.setARGB(128, 128, 128, 128);
	        	canvas.drawRect(imgrect, paint);
	        	Rect resizerect = new Rect ();
	        	resizerect.set(imgrect.right - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.bottom - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.right, imgrect.bottom);
	        	paint.setARGB(255, 0, 0, 0);
	        	paint.setStyle(Style.FILL);
/*	        	if (!resizeMode)
	        		paint.setStyle(Style.STROKE);*/
	        	paint.setStrokeWidth(2.0f);
	        	if (!locked)
	        		canvas.drawRect(resizerect, paint);
	        	int lines = 5;
        		int f = (int)(resizeBoxSize * (1.0/ mScale)) / (lines + 2);
        		for (int i = 0; i < lines; ++i) {
        			resizerect.set(imgrect.left, imgrect.top + 2 * i * f, imgrect.left + f * (lines + 2), imgrect.top + (2 * i + 1) * f);
        			canvas.drawRect(resizerect, paint);
        		}
/*        		resizerect.set(imgrect.left, imgrect.top, imgrect.left + f * 5, imgrect.top + f);
        		canvas.drawRect(resizerect, paint);
	        	resizerect.set(imgrect.left, imgrect.top + 2 * f, imgrect.left + f * 5, imgrect.top + 3 * f);
        		canvas.drawRect(resizerect, paint);
	        	resizerect.set(imgrect.left, imgrect.top + 4 * f, imgrect.left + f * 5, imgrect.top + 5 * f);*/
        		canvas.drawRect(resizerect, paint);
	        }
	        if (locked && padlock != null && !padlock.isRecycled() && interactiveMode) {
	        	Rect dst = new Rect ();
	        	dst.set(imgrect.right - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.top, imgrect.right, imgrect.top + (int)(resizeBoxSize * (1.0/ mScale)));
	        	Rect src = new Rect (0, 0, padlock.getWidth(), padlock.getHeight());
	        	canvas.drawBitmap(padlock, src, dst, paint);
	        }
    	}
    	catch (Exception e) {
    		Log.d ("RAGE", e.toString());
    	}
        canvas.restoreToCount(sc);
    }
    
    public boolean pointIn(int x, int y){
        int wp2 = (int)(((float)getWidth() / 2.0) * mScale);
        int hp2 = (int)((getHeight() / 2.0) * mScale);
        return (x >= mPosition.x - wp2) && (x <= mPosition.x + wp2) &&
        	(y >= mPosition.y - hp2) && (y <= mPosition.y + hp2); 
    }

    public boolean pointInResize(int x, int y){
        int wp2 = (int)(((float)getWidth() / 2.0) * mScale);
        int hp2 = (int)((getHeight() / 2.0) * mScale);
        return (x >= mPosition.x + wp2 - resizeBoxSize) && (x <= mPosition.x + wp2) &&
        	(y >= mPosition.y + hp2 - resizeBoxSize) && (y <= mPosition.y + hp2);
    }

    public boolean pointInMenu(int x, int y){
        int wp2 = (int)(((float)getWidth() / 2.0) * mScale);
        int hp2 = (int)((getHeight() / 2.0) * mScale);
        return (x >= mPosition.x - wp2) && (x <= mPosition.x - wp2 + resizeBoxSize) &&
        	(y >= mPosition.y - hp2) && (y <= mPosition.y - hp2 + resizeBoxSize);
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
		if (getWidth() * Scale >= resizeBoxSize / 2 && getHeight() * Scale >= resizeBoxSize / 2)
			this.mScale = Scale;
	}

	public boolean isSelected() {
		if (mSelected){
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
    
