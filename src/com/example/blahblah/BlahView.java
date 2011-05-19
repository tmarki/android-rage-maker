package com.example.blahblah;


import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.util.Log;


public class BlahView extends View {
	
	public enum TouchModes { HAND, PENCIL, TEXT };
	
	private TouchModes mTouchMode = TouchModes.HAND;
	
	class ComicState {
	    public ComicState() {
		}
	    public ComicState(ComicState os) {
	    	for (ImageObject io : os.mDrawables) {
	    		mDrawables.add(new ImageObject(io));
	    	}
	    	for (TextObject io : os.mTextDrawables) {
	    		mTextDrawables.add(new TextObject(io));
	    	}
	    	mLinePoints = new Vector<Vector<Point>>(os.mLinePoints);
	    	mLinePaints = new Vector<Paint>(os.mLinePaints);
/*	    	mTextDrawables = new Vector<TextObject>(os.mTextDrawables);*/
	    	mCurrentLinePoints = new Vector<Point>(os.mCurrentLinePoints);
	    	if (os.mCurrentText != null) {
	    		mCurrentText = new TextObject(os.mCurrentText);
	    	}
	    	mPanelCount = os.mPanelCount;
	    	currentColor = os.currentColor;
		}
		private Vector<ImageObject> mDrawables = new Vector<ImageObject>();
	    private Vector<TextObject> mTextDrawables = new Vector<TextObject>();
	    private Vector<Vector<Point>> mLinePoints = new Vector<Vector<Point>>();
	    private Vector<Paint> mLinePaints = new Vector<Paint>();
	    private Vector<Point> mCurrentLinePoints = new Vector<Point>();
	    private TextObject mCurrentText = null;
	    private int mPanelCount = 5;
	    private int currentColor = Color.BLACK;
	};
	
	private ComicState currentState = new ComicState ();
	
	private Vector<ComicState> previousStates = new Vector<ComicState>();
	
    private Point mCanvasOffset = new Point (0, 0);
    private Rect mCanvasLimits = new Rect (0, 0, 640, 750);
    private float mCanvasScale = 1.0f;
    private int currentStrokeWidth = 3;
	private Point mPreviousPos = new Point (0, 0); // single touch events
    private float mStartDistance = 0.0f;
    private float mStartScale = 0.0f;
    private float mStartRot = 0.0f;
    private float mPrevRot = 0.0f;
    private boolean mMovedSinceDown = false;
    private boolean mModeMenu = false;
    
    private int mModeIconSize = 100;

    
    public int getCurrentColor() {
		return currentState.currentColor;
	}

	public void setCurrentColor(int currentColor) {
		this.currentState.currentColor = currentColor;
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

	public BlahView(Context context) {
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
    
    public void addImageObject (Drawable dr, int x, int y, float rot, float scale, int drawableId) {
		pushState ();
    	currentState.mDrawables.add(new ImageObject(dr, x, y, rot, scale, drawableId));
    	invalidate ();
    }

    public void addTextObject (int x, int y, int textSize, int color, Typeface tf, String text, boolean bold, boolean italic) {
		pushState ();
    	currentState.mTextDrawables.add(new TextObject(x, y, textSize, color, tf, text, bold, italic));
    }

    @Override 
    protected void onDraw(Canvas canvas) {
    	int sc = canvas.save();
        canvas.drawColor(Color.WHITE);
        canvas.scale(mCanvasScale, mCanvasScale);
        canvas.translate(mCanvasOffset.x, mCanvasOffset.y);

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
        for (ImageObject ad : currentState.mDrawables) {
        	if (ad != null && ad.isInBack()) {
        		ad.draw(canvas);
        	}
        }
        Vector<Vector<Point>> lp = currentState.mLinePoints; 
		for (int i = 0; i < lp.size(); ++i){
			if (lp.get(i).size() == 0) continue;
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
	        Vector<Point> clp = currentState.mCurrentLinePoints; 
			
			for (int i = 0; i < clp.size() - 1; ++i){
				canvas.drawLine(clp.get(i).x, clp.get(i).y, clp.get(i + 1).x, clp.get(i + 1).y, p);
				canvas.drawCircle(clp.get(i).x, clp.get(i).y, currentStrokeWidth / 2, p);
				if (i == clp.size() - 2)
					canvas.drawCircle(clp.get(i + 1).x, clp.get(i + 1).y, currentStrokeWidth / 2, p);
			}
		}
		for (TextObject to : currentState.mTextDrawables) {
			if (to != null) {
				to.draw(canvas);
				Log.d ("RAGE", "Drawing " + to.getText());
			}
		}
        for (ImageObject ad : currentState.mDrawables) {
        	if (ad != null && !ad.isInBack()) {
        		ad.draw(canvas);
        	}
        }
		if (currentState.mCurrentText != null)
			currentState.mCurrentText.draw (canvas);
        canvas.restoreToCount(sc);
        sc = canvas.save();
    	Drawable dr;
        if (mTouchMode == TouchModes.HAND)
        	dr = getResources().getDrawable(R.drawable.hand);
        else if (mTouchMode == TouchModes.PENCIL)
        	dr = getResources().getDrawable(R.drawable.pencil);
        else
        	dr = getResources().getDrawable(R.drawable.type);
        mModeIconSize = (getWidth() > getHeight () ? getWidth () : getHeight ()) / 8; 
        canvas.translate(getWidth() - mModeIconSize, getHeight() - mModeIconSize);
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        canvas.scale((float)mModeIconSize / dr.getIntrinsicWidth(), (float)mModeIconSize / dr.getIntrinsicHeight());
        dr.draw(canvas);
        canvas.restoreToCount(sc);
//        invalidate();
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
			if (event.getAction() == MotionEvent.ACTION_DOWN
					 && event.getX() > getWidth() - mModeIconSize
					 && event.getX() < getWidth()
					 && event.getY() > getHeight() - mModeIconSize
					 && event.getY() < getHeight()) {
				mModeMenu = true;
				showContextMenu();
			}
			else {
				if (mTouchMode == TouchModes.HAND)
					handleSingleTouchManipulateEvent(event);
				else if (mTouchMode == TouchModes.PENCIL)
					handleSingleTouchDrawEvent(event);
				else if (mTouchMode == TouchModes.TEXT)
					handleSingleTouchTextEvent(event);
				else
					cancelLongPress();
			}
		}
		else
			handleMultiTouchEvent(event);
		invalidate();
		super.onTouchEvent(event);
		return true;
	}
	
	private void handleMultiTouchEvent (MotionEvent event) {
		float x1 = event.getX(0);
		float x2 = event.getX(1);
		float y1 = event.getY(0);
		float y2 = event.getY(1);
		float diff = (float)Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
		float a = (x1 - x2);
		float b = (y1 - y2);
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
	        	if (io.isSelected() && newscale < 10.0f && newscale > 0.1f)
	        	{
	        		io.setScale(newscale);
	        		io.setRotation(mPrevRot - rot + mStartRot);
	        		found = true;
	        		break;
	        	}
	        }
	        if (!found) {
	        	float newscale = mStartScale * scale;
	        	if (newscale < 5.0f && newscale > 0.2f)
	        		mCanvasScale = newscale;
	        }
		}
		super.cancelLongPress();
		
	}
	
	private void handleSingleTouchManipulateEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pushState ();
			mMovedSinceDown = false;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && !mMovedSinceDown) {
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
		else if (event.getAction() == MotionEvent.ACTION_UP && mMovedSinceDown) {
			boolean found = false;
	        for (ImageObject ad : currentState.mDrawables) {
	        	if (ad.isSelected()) {
	        		found = true;
	        	}
	        }
			if (!found && (previousStates.size() > 0))
				previousStates.remove(previousStates.size() - 1);
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
			int diffX = (int)((event.getX() - mPreviousPos.x) / mCanvasScale);
			int diffY = (int)((event.getY() - mPreviousPos.y) / mCanvasScale);
			if (Math.abs(diffX) > 5 / mCanvasScale || Math.abs(diffY) > 5 / mCanvasScale) {
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
		mPreviousPos.x = (int)event.getX();
		mPreviousPos.y = (int)event.getY();
	}

	private void handleSingleTouchDrawEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			currentState.mCurrentLinePoints.clear();
			pushState ();
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			currentState.mLinePoints.add(new Vector<Point>(currentState.mCurrentLinePoints));
			Paint p = new Paint ();
			p.setColor(this.currentState.currentColor);
			p.setStrokeWidth(currentStrokeWidth);
			currentState.mLinePaints.add (new Paint (p));
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
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
		else if (event.getAction() == MotionEvent.ACTION_UP && mMovedSinceDown == false) {
			AlertDialog.Builder alert = new AlertDialog.Builder(getContext ());

			alert.setTitle("Type in *le text*");
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
							20, currentState.currentColor, Typeface.MONOSPACE, value, false, false);
					invalidate();
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			  }
			});

			alert.show();
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
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
    
}