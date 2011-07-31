package com.tmarki.comicmaker;


import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.example.blahblah.R;
import com.tmarki.comicmaker.TextObject.FontType;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.hardware.Camera.PreviewCallback;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
//import android.view.MotionEvent.PointerCoords;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.text.format.Time;
import android.util.Log;


public class ComicEditor extends View {
	
	public enum TouchModes { HAND, LINE, PENCIL, TEXT };
	
	private TouchModes mTouchMode = TouchModes.HAND;
	
	public class ComicState {
	    public ComicState() {
		}
	    public ComicState(ComicState os) {
	    	for (ImageObject io : os.mDrawables) {
	    		mDrawables.add(new ImageObject(io));
	    	}
	    	for (TextObject io : os.mTextDrawables) {
	    		mTextDrawables.add(new TextObject(io));
	    	}
	    	mLinePoints = new LinkedList<LinkedList<Point>>(os.mLinePoints);
	    	mLinePaints = new LinkedList<Paint>(os.mLinePaints);
/*	    	mTextDrawables = new Vector<TextObject>(os.mTextDrawables);*/
	    	mCurrentLinePoints = new LinkedList<Point>(os.mCurrentLinePoints);
	    	if (os.mCurrentText != null) {
	    		mCurrentText = new TextObject(os.mCurrentText);
	    	}
	    	mPanelCount = os.mPanelCount;
	    	currentColor = os.currentColor;
		}
	    public Vector<ImageObject> mDrawables = new Vector<ImageObject>();
	    public Vector<TextObject> mTextDrawables = new Vector<TextObject>();
	    public LinkedList<LinkedList<Point>> mLinePoints = new LinkedList<LinkedList<Point>>();
	    public LinkedList<Paint> mLinePaints = new LinkedList<Paint>();
	    public LinkedList<Point> mCurrentLinePoints = new LinkedList<Point>();
	    public TextObject mCurrentText = null;
	    public int mPanelCount = 4;
	    public int currentColor = Color.BLACK;
	};
	
	private ComicState currentState = new ComicState ();
	
	private Vector<ComicState> previousStates = new Vector<ComicState>();
	
    private Point mCanvasOffset = new Point (0, 0);
    private Rect mCanvasLimits = new Rect (0, 0, 640, 500);
    private float mCanvasScale = 1.0f;
    private int currentStrokeWidth = 3;
	private Point mPreviousPos = new Point (0, 0); // single touch events
	private boolean resizeObjectMode = false;
    private float mStartDistance = 0.0f;
    private float mStartScale = 0.0f;
    private float mStartRot = 0.0f;
    private int mStartTextSize = 0;
    private float mPrevRot = 0.0f;
    private boolean mMovedSinceDown = false;
    private boolean mModeMenu = false;
    private FontType defaultFontType = FontType.values()[0];
    private boolean defaultBold = false;
    private boolean defaultItalic = false;
    private int defaultFontSize = 20;
    private boolean drawGrid = true;
    private boolean wasMultiTouch = false;
	private Time lastInvalidate = new Time ();
	
	public float getCanvasScale () {
		return mCanvasScale;
	}

	public void setCanvasScale (float cs) {
		mCanvasScale = cs;
	}

	public Vector<ComicState> getHistory () {
		return previousStates;
	}
	
	public ComicState getStateCopy () {
		return new ComicState (currentState);
	}
	
	public void pushHistory (ComicState cs) {
		previousStates.add(cs);
	}

    public void setPanelCount (int pc) {
    	if (pc != currentState.mPanelCount) {
    		pushState();
    		currentState.mPanelCount = pc;
        	mCanvasLimits.bottom = (((pc - 1) / 2) + 1) * 250;
    	}
    }
    
    public void removeImageObject (ImageObject io) {
    	pushState ();
        for (ImageObject ad : currentState.mDrawables) {
        	if (ad == io) {
        		currentState.mDrawables.remove(io);
        		break;
        	}
        		
        }

    }
    
    public int getPanelCount () {
    	return currentState.mPanelCount;
    }
    
    public boolean isDefaultBold() {
		return defaultBold;
	}

	public void setDefaultBold(boolean defaultBold) {
		this.defaultBold = defaultBold;
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.setBold(defaultBold);
	}

	public boolean isDefaultItalic() {
		return defaultItalic;
	}

	public void setDefaultItalic(boolean defaultItalic) {
		this.defaultItalic = defaultItalic;
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.setItalic(defaultItalic);
	}

	public Point getmCanvasOffset() {
		return mCanvasOffset;
	}

	public void setmCanvasOffset(Point mCanvasOffset) {
		this.mCanvasOffset = mCanvasOffset;
	}

	private int mModeIconSize = 100;

    
    public int getCurrentColor() {
		return currentState.currentColor;
	}

	public void setCurrentColor(int currentColor) {
		this.currentState.currentColor = currentColor;
		if (this.currentState.mCurrentText != null) {
			this.currentState.mCurrentText.setColor(currentColor);
			invalidate();
		}
	}
	
	public void setCurrentFont (FontType ft) {
		defaultFontType = ft;
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.setTypeface(ft);
	}
	
	public void resetObjects () {
		pushState ();
		currentState.mDrawables.clear ();
		currentState.mLinePaints.clear();
		currentState.mLinePoints.clear();
		currentState.mCurrentLinePoints.clear ();
		currentState.mTextDrawables.clear ();
		currentState.mCurrentText = null;
	}
	
    public int getCurrentStrokeWidth() {
		return currentStrokeWidth;
	}

	public void setCurrentStrokeWidth(int currentStrokeWidth) {
		this.currentStrokeWidth = currentStrokeWidth;
	}
	
	public int getDefaultFontSize() {
		return defaultFontSize;
	}

	public void setDefaultFontSize(int defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.setTextSize(defaultFontSize);
	}

	public ComicEditor(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

    }
    
    public ImageObject getSelected(){
        for (ImageObject ad : currentState.mDrawables) {
        	if (ad.isSelected())
        		return ad;
        }
        return null;
    }
    
    public void resetClick (){
    	mMovedSinceDown = true;
    }
    
    public Vector<ImageObject> getImageObjects (){
    	return currentState.mDrawables;

    }

    public Vector<TextObject> getTextObjects (){ 
    	Vector<TextObject> ret = new Vector<TextObject>(currentState.mTextDrawables); 
		if (currentState.mCurrentText != null)
		{
			ret.add(new TextObject (currentState.mCurrentText));
		}
    	return ret;
    }
    
    public LinkedList<LinkedList<Point>> getPoints () {
    	LinkedList<LinkedList<Point>> ret = new LinkedList<LinkedList<Point>> (currentState.mLinePoints);
		if (currentState.mCurrentLinePoints.size () > 0) {
			ret.add(new LinkedList<Point>(currentState.mCurrentLinePoints));
		}
    	return ret;
    }

    public LinkedList<Paint> getPaints () {
    	LinkedList<Paint> ret = new LinkedList<Paint>(currentState.mLinePaints);
		if (currentState.mCurrentLinePoints.size () > 0) {
			Paint p = new Paint ();
			p.setStrokeWidth(currentStrokeWidth);
			p.setColor(currentState.currentColor);
			ret.add(p); 
		}    	
    	return ret;
    }
    
    public void addLine (LinkedList<Point> points, Paint paint){
		if (currentState.mCurrentLinePoints.size () > 0) {
			currentState.mLinePoints.add(new LinkedList<Point>(currentState.mCurrentLinePoints));
			Paint p = new Paint ();
			p.setStrokeWidth(currentStrokeWidth);
			p.setColor(currentState.currentColor);
			currentState.mLinePaints.add(p);
		}
    	currentState.mCurrentLinePoints = new LinkedList<Point>(points); 
    	currentStrokeWidth = (int)paint.getStrokeWidth();
    	currentState.currentColor = paint.getColor();
    }

    public void pureAddLine (LinkedList<Point> points, Paint paint){
		currentState.mLinePoints.add(points);
		currentState.mLinePaints.add(paint);
    }

    public void resetHistory () {
    	previousStates.clear();
    }

    public void addImageObject (Drawable dr, int x, int y, float rot, float scale, int drawableId) {
    	addImageObject(dr, x, y, rot, scale, drawableId, "", "", "");
    }

    public void addImageObject (Drawable dr, int x, int y, float rot, float scale, int drawableId, String pack, String folder, String file) {
		pushState ();
    	currentState.mDrawables.add(new ImageObject(dr, x, y, rot, scale, drawableId, pack, folder, file));
    	invalidate ();
    }
    
    public void pureAddImageObject (ImageObject io) {
    	currentState.mDrawables.add(io);
    }

    public void addTextObject (int x, int y, int textSize, int color, TextObject.FontType ft, String text, boolean bold, boolean italic) {
		pushState ();
		if (currentState.mCurrentText != null)
		{
			currentState.mTextDrawables.add(new TextObject (currentState.mCurrentText));
		}
		currentState.mCurrentText = new TextObject(x, y, textSize, color, ft, text, bold, italic);
//    	currentState.mTextDrawables.add(new TextObject(x, y, textSize, color, ft, text, bold, italic));
    }

    public void pureAddTextObject (TextObject to) {
		currentState.mTextDrawables.add(to);
//		currentState.mCurrentText = to;
    }

    private void drawGridLines (Canvas canvas) {
    	if (currentState.mPanelCount < 2)
    		return;
        Paint paint = new Paint ();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3.0f);
        int bott = mCanvasLimits.bottom;
        int dy = mCanvasLimits.height() / (currentState.mPanelCount / 2);
        if (currentState.mPanelCount % 2 == 1) {
        	dy = mCanvasLimits.height() / (currentState.mPanelCount / 2 + 1);
        	bott -= dy;
        }
    	canvas.drawLine(mCanvasLimits.width () / 2, mCanvasLimits.top, mCanvasLimits.width () / 2, bott, paint);
        for (int i = 0; i < (currentState.mPanelCount / 2) + 1; ++i) {
    		canvas.drawLine(mCanvasLimits.left, mCanvasLimits.top + i * dy, mCanvasLimits.right, mCanvasLimits.top + i * dy, paint);
        }
		canvas.drawLine(mCanvasLimits.left, mCanvasLimits.top, mCanvasLimits.left, mCanvasLimits.bottom, paint);
		canvas.drawLine(mCanvasLimits.left, mCanvasLimits.bottom, mCanvasLimits.right, mCanvasLimits.bottom, paint);
		canvas.drawLine(mCanvasLimits.right, mCanvasLimits.bottom, mCanvasLimits.right, mCanvasLimits.top, paint);
    }
    
    private void drawImages (Canvas canvas, boolean back) {
    	ImageObject.setResizeMode(resizeObjectMode);
        for (ImageObject ad : currentState.mDrawables) {
        	if (ad != null && (ad.isInBack() == back)) {
        		ad.draw(canvas);
        	}
        }
    }
    
    private void drawLines (Canvas canvas) {
    	LinkedList<LinkedList<Point>> lp = currentState.mLinePoints; 
		for (int i = 0; i < lp.size(); ++i){
			if (lp.get(i).size() == 0) continue;
			Log.d ("RAGE", String.format ("Line count: %s", lp.size ()));
			for (int j = 0; j < lp.get (i).size() - 1; ++j){
				canvas.drawLine(lp.get(i).get (j).x, lp.get(i).get (j).y, lp.get(i).get (j + 1).x, lp.get(i).get (j + 1).y, currentState.mLinePaints.get (i));
				canvas.drawCircle(lp.get(i).get(j).x, lp.get(i).get(j).y, currentState.mLinePaints.get (i).getStrokeWidth() / 2, currentState.mLinePaints.get (i));
				if (j == lp.get (i).size() - 2)
					canvas.drawCircle(lp.get(i).get(j + 1).x, lp.get(i).get(j + 1).y, currentState.mLinePaints.get (i).getStrokeWidth() / 2, currentState.mLinePaints.get (i));
			}
		}
		if (currentState.mCurrentLinePoints.size() > 0) {
			Paint p = new Paint ();
			p.setColor(this.currentState.currentColor);
			p.setStrokeWidth(currentStrokeWidth);
			LinkedList<Point> clp = currentState.mCurrentLinePoints; 
			Log.d ("RAGE", String.format ("Line count: %s", clp.size ()));
			for (int i = 0; i < clp.size() - 1; ++i){
				canvas.drawLine(clp.get(i).x, clp.get(i).y, clp.get(i + 1).x, clp.get(i + 1).y, p);
				canvas.drawCircle(clp.get(i).x, clp.get(i).y, currentStrokeWidth / 2, p);
				if (i == clp.size() - 2)
					canvas.drawCircle(clp.get(i + 1).x, clp.get(i + 1).y, currentStrokeWidth / 2, p);
			}
		}
    }
    
    private void drawText (Canvas canvas) {
		for (TextObject to : currentState.mTextDrawables) {
			if (to != null) {
				to.draw(canvas);
				Log.d ("RAGE", "Drawing " + to.getText());
			}
		}
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.draw (canvas);
    }
    
    private void drawModeIcon (Canvas canvas) {
    	Drawable dr;
        if (mTouchMode == TouchModes.HAND)
        	dr = getResources().getDrawable(R.drawable.hand);
        else if (mTouchMode == TouchModes.PENCIL)
        	dr = getResources().getDrawable(R.drawable.pencil);
        else if (mTouchMode == TouchModes.LINE)
        	dr = getResources().getDrawable(R.drawable.line);
        else
        	dr = getResources().getDrawable(R.drawable.type);
        mModeIconSize = (getWidth() > getHeight () ? getWidth () : getHeight ()) / 8; 
        canvas.translate(getWidth() - mModeIconSize, getHeight() - mModeIconSize);
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        canvas.scale((float)mModeIconSize / dr.getIntrinsicWidth(), (float)mModeIconSize / dr.getIntrinsicHeight());
        RectF r = new RectF (-5.0f, -5.0f, (float)dr.getIntrinsicWidth() + 10.0f, (float)dr.getIntrinsicHeight() + 10.0f);
        Paint p = new Paint ();
        p.setColor(currentState.currentColor);
        canvas.drawRoundRect(r, 4, 4, p);
        dr.draw(canvas);
    }
    
    public Bitmap getSaveBitmap () {
    	ImageObject.setInteractiveMode(false);
    	Bitmap bmp = Bitmap.createBitmap(mCanvasLimits.right, mCanvasLimits.bottom, Bitmap.Config.ARGB_8888);
    	Canvas canvas = new Canvas (bmp);
        canvas.drawColor(Color.WHITE);
        if (drawGrid)
        	drawGridLines (canvas);
        drawImages (canvas, true);
        drawLines (canvas);
        drawText (canvas);
        drawImages (canvas, false);
    	return bmp;
    }

    @Override 
    protected void onDraw(Canvas canvas) {
    	ImageObject.setInteractiveMode(true);
    	int sc = canvas.save();
        canvas.drawColor(Color.BLACK);
        canvas.scale(mCanvasScale, mCanvasScale);
        canvas.translate(mCanvasOffset.x, mCanvasOffset.y);
        canvas.clipRect(mCanvasLimits);
        canvas.drawColor(Color.WHITE);
        
        if (drawGrid)
        	drawGridLines (canvas);
        drawImages (canvas, true);
        drawLines (canvas);
        drawText (canvas);
        drawImages (canvas, false);
        canvas.restoreToCount(sc);
        drawModeIcon (canvas);

        canvas.restoreToCount(sc);
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
		Log.d("RAGE", String.valueOf (event.getPointerCount()));
		if (event.getPointerCount() == 1) {
			mStartDistance = 0.0f;
			if (event.getX() > getWidth() - mModeIconSize
					 && event.getX() < getWidth()
					 && event.getY() > getHeight() - mModeIconSize
					 && event.getY() < getHeight()) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mModeMenu = true;
					showContextMenu();
				}
			}
			else {
				if (mTouchMode == TouchModes.HAND)
					handleSingleTouchManipulateEvent(event);
				else if (mTouchMode == TouchModes.PENCIL || mTouchMode == TouchModes.LINE)
					handleSingleTouchDrawEvent(event);
				else if (mTouchMode == TouchModes.TEXT)
					handleSingleTouchTextEvent(event);
				else
					cancelLongPress();
			}
		}
		else {
			if (mTouchMode == TouchModes.HAND)
				handleMultiTouchManipulateEvent(event);
			else if (mTouchMode == TouchModes.TEXT)
				handleMultiTouchTextEvent(event);
		}
		Time t = new Time ();
		t.setToNow();
		if (lastInvalidate != t) {
			invalidate();
			lastInvalidate = t;
		}
		super.onTouchEvent(event);
		return true;
	}
	
	private void handleMultiTouchManipulateEvent (MotionEvent event) {
		wasMultiTouch = true;
		float x1 = event.getX(0);
		float x2 = event.getX(1);
		float y1 = event.getY(0);
		float y2 = event.getY(1);
		float diff = (float)Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
		float a = (x1 - x2);
		float b =  (y1 - y2);
/*		if (y1 > y2)
			b = (y2 - y1);*/
		float q = a / b;
		float rot = (float)Math.toDegrees(Math.atan (q));
		Log.d("RAGE", "Diff " + String.valueOf(diff) + " Rot " + String.valueOf(rot));
		if (mStartDistance < 0.1f) {
			mStartDistance = diff;
			boolean found = false;
	        for (ImageObject io : currentState.mDrawables) {
	        	if (io.isSelected())
	        	{
	        		mStartScale = io.getScale();
	        		mStartRot = io.getRotation();
	        		mPrevRot = rot;
	        		found = true;
	        		break;
	        	}
	        }
	        if (!found) {
	        	mStartScale = mCanvasScale;
	        }
		}
		else {
			float scale = diff / mStartDistance;
			Log.d("RAGE", "Scale " + String.valueOf(scale) + " Start Scale " + String.valueOf(mStartScale));
			boolean found = false;
	        for (ImageObject io : currentState.mDrawables) {
	        	float newscale = mStartScale * scale;
	        	float rotdiff = rot - mPrevRot;
	        	if (io.isSelected() && newscale < 10.0f && newscale > 0.1f)
	        	{
	        		io.setScale(newscale);
	        		float newrot = Math.round((mStartRot - rotdiff) / 5.0f) * 5.0f;
	        		io.setRotation(newrot);
//	        		io.setRotation(mStartRot - rotdiff);
	        		found = true;
	        		break;
	        	}
	        }
	        if (!found) {
	        	float newscale = mStartScale * scale;
	        	if (newscale < 5.0f && newscale > 0.2f) {
	        		mCanvasScale = newscale;
	        	}
	        }
		}
		super.cancelLongPress();
		
	}

	private void handleMultiTouchTextEvent (MotionEvent event) {
		wasMultiTouch = true;
		float x1 = event.getX(0);
		float x2 = event.getX(1);
		float y1 = event.getY(0);
		float y2 = event.getY(1);
		float diff = (float)Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
		float a = (x1 - x2);
		float b =  (y1 - y2);
/*		if (y1 > y2)
			b = (y2 - y1);*/
		float q = a / b;
		float rot = (float)Math.toDegrees(Math.atan (q));
		Log.d("RAGE", "Diff " + String.valueOf(diff) + " Rot " + String.valueOf(rot));
		if (mStartDistance < 0.1f) {
			mStartDistance = diff;
			mStartScale = 0.0f;
			boolean found = false;
			mStartTextSize = currentState.mCurrentText.getTextSize();
		}
		else {
			float scale = diff / mStartDistance;
			Log.d("RAGE", "Scale " + String.valueOf(scale) + " Start Scale " + String.valueOf(mStartScale));
			currentState.mCurrentText.setTextSize((int)(mStartTextSize * (scale - mStartScale)));
		}
		super.cancelLongPress();
		
	}

	private void handleSingleTouchManipulateEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pushState ();
			mMovedSinceDown = false;
			resizeObjectMode = false;
			ImageObject io = getSelected();
			if (io != null && io.pointInResize ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
				resizeObjectMode = true;
			}
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && !mMovedSinceDown && !resizeObjectMode && !wasMultiTouch) {
			resizeObjectMode = false;
			if (previousStates.size() > 0)
				previousStates.remove(previousStates.size() - 1);
			int selectedId = -1;
			for (int i = currentState.mDrawables.size() - 1; i >= 0; --i) {
				ImageObject io = currentState.mDrawables.elementAt(i);
				if (io.pointIn ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
					Log.d ("RAGE", "Point in!" + io.toString());
	        		io.setSelected(!io.isSelected());
	        		currentState.mDrawables.removeElementAt(i);
	        		currentState.mDrawables.add(io);
					selectedId = currentState.mDrawables.size() - 1;
					break;
				}
			}
	        for (int i = 0; i < currentState.mDrawables.size(); ++i) {
				ImageObject io = currentState.mDrawables.elementAt(i);
	        	if (io.isSelected() && i != selectedId)
	        	{
	        		io.setSelected(!io.isSelected());
	        	}
	        }
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && mMovedSinceDown && !resizeObjectMode) {
			resizeObjectMode = false;
			boolean found = false;
	        for (ImageObject ad : currentState.mDrawables) {
	        	if (ad.isSelected()) {
	        		found = true;
	        	}
	        }
			if (!found && (previousStates.size() > 0))
				previousStates.remove(previousStates.size() - 1);
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE && !wasMultiTouch){
			if (!resizeObjectMode) {
				int diffX = (int)((event.getX() - mPreviousPos.x) / mCanvasScale);
				int diffY = (int)((event.getY() - mPreviousPos.y) / mCanvasScale);
				if (Math.abs(diffX) > 3 * mCanvasScale || Math.abs(diffY) > 3 * mCanvasScale) {
					super.cancelLongPress();
					mMovedSinceDown = true;
					boolean found = false;
			        for (ImageObject ad : currentState.mDrawables) {
			        	if (ad.isSelected()) {
			        		found = true;
			        		Point p = ad.getPosition();
			        		if (p.x + diffX >= mCanvasLimits.left
			        				&& p.x + diffX <= mCanvasLimits.right
			        				&& p.y + diffY >= mCanvasLimits.top
			        				&& p.y + diffY <= mCanvasLimits.bottom
			        				)
			        		ad.moveBy((int)(diffX), (int)(diffY));
			        	}
			        }
			        if (!found)
			        {
			        	if (((mCanvasOffset.x + diffX) < mCanvasLimits.left && diffX > 0)
			        			|| (-(mCanvasOffset.x + diffX) + getWidth () / mCanvasScale <= mCanvasLimits.right) && diffX < 0) 
			        		mCanvasOffset.x += diffX;
			        	if (((mCanvasOffset.y + diffY) < mCanvasLimits.top && diffY > 0)
				        		|| (-(mCanvasOffset.y + diffY) + getHeight () / mCanvasScale <= mCanvasLimits.bottom) && diffY < 0)
			        		mCanvasOffset.y += diffY;
			        } 
				}
			}
			else {
				cancelLongPress();
				ImageObject sel = getSelected();
				if (sel != null) {
					int direction = 1;
/*					if (event.getX () > mPreviousPos.x) {
						direction = 1;
					}*/
					double diffSize = event.getX () - mPreviousPos.x;
					double imgDiag = (Math.sqrt((double)((sel.getIntrinsicWidth()) * (sel.getIntrinsicWidth()) + (sel.getIntrinsicHeight()) * (sel.getIntrinsicHeight()))) / 2.0) * sel.getScale();
					sel.setScale((float)((imgDiag + (double)direction * diffSize) / imgDiag) * sel.getScale ());
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP && wasMultiTouch == true)
			wasMultiTouch = false;
		mPreviousPos.x = (int)event.getX();
		mPreviousPos.y = (int)event.getY();
	}

	private void handleSingleTouchDrawEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			currentState.mCurrentLinePoints.clear();
			pushState ();
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (currentState.mCurrentLinePoints.size () > 0) {
				currentState.mLinePoints.add(new LinkedList<Point>(currentState.mCurrentLinePoints));
			}
			Paint p = new Paint ();
			p.setColor(this.currentState.currentColor);
			p.setStrokeWidth(currentStrokeWidth);
			currentState.mLinePaints.add (new Paint (p));
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
			if (mTouchMode == TouchModes.LINE && currentState.mCurrentLinePoints.size() > 1)
				currentState.mCurrentLinePoints.removeLast();
			currentState.mCurrentLinePoints.add(new Point ((int)(event.getX() / mCanvasScale) - mCanvasOffset.x, (int)(event.getY() / mCanvasScale) - mCanvasOffset.y));
		}
		super.cancelLongPress();
		mPreviousPos.x = (int)event.getX();
		mPreviousPos.y = (int)event.getY();
	}

	private void handleSingleTouchTextEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mMovedSinceDown = false;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && mMovedSinceDown == false && wasMultiTouch == false) {
			AlertDialog.Builder alert = new AlertDialog.Builder(getContext ());

			alert.setTitle("Enter the text");
//			alert.setMessage("Message");

			// Set an EditText view to get user input 
			final EditText input = new EditText(getContext());
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					pushState ();
					if (currentState.mCurrentText != null)
					{
						currentState.mTextDrawables.add(new TextObject (currentState.mCurrentText));
					}
					currentState.mCurrentText = new TextObject((int)(mPreviousPos.x / mCanvasScale - mCanvasOffset.x), (int)(mPreviousPos.y / mCanvasScale - mCanvasOffset.y),
							defaultFontSize, currentState.currentColor, defaultFontType, value, defaultBold, defaultItalic);
					invalidate();
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			  }
			});

			alert.show();
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE && wasMultiTouch == false){
			int diffX = (int)((event.getX() - mPreviousPos.x) / mCanvasScale);
			int diffY = (int)((event.getY() - mPreviousPos.y) / mCanvasScale);
			if (Math.abs(diffX) > 2 / mCanvasScale || Math.abs(diffY) > 2 / mCanvasScale) {
				mMovedSinceDown = true;
				if (currentState.mCurrentText != null) {
					currentState.mCurrentText.setX(currentState.mCurrentText.getX() + diffX);
					currentState.mCurrentText.setY(currentState.mCurrentText.getY() + diffY);
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			wasMultiTouch = false;
		}
		super.cancelLongPress();
		mPreviousPos.x = (int)event.getX();
		mPreviousPos.y = (int)event.getY();
	}

	@Override
	protected void onCreateContextMenu(ContextMenu menu) {
		if (mModeMenu) {
			MenuInflater inflater = new MenuInflater(getContext());
			inflater.inflate(R.menu.mode_menu, menu);
			Log.d ("RAGE", "Mode menu");
			mModeMenu = false;
			cancelLongPress();
		}
	}

	public TouchModes getmTouchMode() {
		return mTouchMode;
	}

	public void setmTouchMode(TouchModes mTouchMode) {
		if (mTouchMode != null)
			this.mTouchMode = mTouchMode;
	}
	
	public void pushState () {
		previousStates.add (new ComicState(currentState));
	}
	
	public boolean popState () {
		int pos = previousStates.size () - 1; 
		if (pos >= 0) {
			currentState = new ComicState(previousStates.get(pos));
			previousStates.removeElementAt(pos);
			invalidate();
			return true;
		}
		else {
			resetObjects();
		}
		invalidate();
		return false;
	}


	public boolean isDrawGrid() {
		return drawGrid;
	}


	public void setDrawGrid(boolean drawGrid) {
		this.drawGrid = drawGrid;
	}
    
}