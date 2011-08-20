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
        Log.d ("RAGE", "Initialized ImageObject at" + mPosition.toString());
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
    
    public void draw(Canvas canvas) {
    	int sc = canvas.save();
    	try {
	    	canvas.translate(mPosition.x, mPosition.y);
	    	canvas.scale( (float)mScale, (float)mScale);
	    	int sc2 = canvas.save();
	    	canvas.rotate((float)mRotation);
	    	canvas.scale((flipHorizontal ? -1 : 1), (flipVertical ? -1 : 1));
	//            dr.draw(canvas);
	    	canvas.drawBitmap(content, -getWidth() / 2, -getHeight() / 2, new Paint ());
	        canvas.restoreToCount(sc2);
	        if (mSelected && interactiveMode)
	        {
	        	Paint paint = new Paint ();
	        	paint.setARGB(128, 128, 128, 128);
	        	Rect imgrect = new Rect(-getWidth() / 2, -getHeight() / 2, getWidth() / 2, getHeight() / 2);
	        	canvas.drawRect(imgrect, paint);
	        	Rect resizerect = new Rect ();
	        	resizerect.set(imgrect.right - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.bottom - (int)(resizeBoxSize * (1.0/ mScale)), imgrect.right, imgrect.bottom);
	        	paint.setARGB(255, 0, 0, 0);
	        	if (!resizeMode)
	        		paint.setStyle(Style.STROKE);
	        	paint.setStrokeWidth(2.0f);
	        	canvas.drawRect(resizerect, paint);
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
		if (getWidth() * Scale >= resizeBoxSize && getHeight() * Scale >= resizeBoxSize)
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
    
