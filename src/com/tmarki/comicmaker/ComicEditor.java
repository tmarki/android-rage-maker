package com.tmarki.comicmaker;


import java.util.LinkedList;
import java.util.Vector;

import com.tmarki.comicmaker.R;
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
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.text.format.Time;


public class ComicEditor extends View {
	
	public enum TouchModes { HAND, LINE, PENCIL, TEXT };
	private String[] TMNames = { "Manipulate mode", "Line mode", "Draw mode", "Type mode" }; 
	
	private TouchModes mTouchMode = TouchModes.HAND;
	
	public interface ZoomChangeListener {
		public void ZoomChanged (float newScale);
	}
	
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
	    	linePoints = new Vector<float[]>();
	    	for (int i = 0; i < os.linePoints.size(); ++i) {
	    		float tmp[] = new float[os.linePoints.get(i).length];
	    		System.arraycopy(os.linePoints.get (i), 0, tmp, 0, os.linePoints.get (i).length);
	    		linePoints.add (tmp);
	    	}
	    		
	    	mLinePaints = new LinkedList<Paint>(os.mLinePaints);
	    	mTextDrawables = new Vector<TextObject>(os.mTextDrawables);
	    	if (os.currentLinePoints != null) {
	    		float src[] = os.currentLinePoints;
	    		currentLinePoints = new float[os.currentLinePoints.length];
	    		System.arraycopy(src, 0, currentLinePoints, 0, os.currentLinePoints.length);
	    	}
	    	if (os.mCurrentText != null) {
	    		mCurrentText = new TextObject(os.mCurrentText);
	    	}
	    	mPanelCount = os.mPanelCount;
	    	currentColor = os.currentColor;
		}
	    public Vector<ImageObject> mDrawables = new Vector<ImageObject>();
	    public Vector<TextObject> mTextDrawables = new Vector<TextObject>();
	    public Vector<float[]> linePoints = new Vector<float[]>();
	    public LinkedList<Paint> mLinePaints = new LinkedList<Paint>();
	    public float[] currentLinePoints = null;
	    public TextObject mCurrentText = null;
	    public int mPanelCount = 4;
	    public int currentColor = Color.BLACK;
	};
	 
	ListAdapter modeAdapter = new ArrayAdapter<String>(
	                getContext(), R.layout.mode_select_row, TMNames) {
	               
	        ViewHolder holder;
	 
	        class ViewHolder {
	                ImageView icon;
	                TextView title;
	        }
	 
	        public View getView(int position, View convertView,
	                        ViewGroup parent) {
	                final LayoutInflater inflater = (LayoutInflater) getContext()
	                                .getSystemService(
	                                                Context.LAYOUT_INFLATER_SERVICE);
	 
	                if (convertView == null) {
	                        convertView = inflater.inflate(
	                                        R.layout.mode_select_row, null);
	 
	                        holder = new ViewHolder();
	                        holder.icon = (ImageView) convertView
	                                        .findViewById(R.id.icon);
	                        holder.title = (TextView) convertView
	                                        .findViewById(R.id.title);
	                        convertView.setTag(holder);
	                } else {
	                        // view already defined, retrieve view holder
	                        holder = (ViewHolder) convertView.getTag();
	                }              
	 
	                Drawable tile = null; //this is an image from the drawables folder

	                if (position == 0)
	                	tile = getResources().getDrawable(R.drawable.hand);
	                else if (position == 1)
	                	tile = getResources().getDrawable(R.drawable.line);
	                else if (position == 2)
	                	tile = getResources().getDrawable(R.drawable.pencil);
	                else if (position == 3)
	                	tile = getResources().getDrawable(R.drawable.text);
	               
	                holder.title.setText(TMNames[position]);
	                if (tile != null)
	                	holder.icon.setImageDrawable(tile);
	 
	                return convertView;
	        }
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
    private FontType defaultFontType = FontType.values()[0];
    private boolean defaultBold = false;
    private boolean defaultItalic = false;
    private int defaultFontSize = 20;
    private boolean drawGrid = true;
    private boolean wasMultiTouch = false;
	private Time lastInvalidate = new Time ();
	private Bitmap linesLayer = null;
	private ZoomChangeListener zoomChangeListener = null;
	
	public ComicEditor(Context context, ZoomChangeListener zcl) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        zoomChangeListener = zcl;

    }
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
		currentState.linePoints.clear ();
		currentState.currentLinePoints = null;
		linesLayer = null;
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
    
    public Vector<float[]> getPoints () {
    	return new Vector<float[]> (currentState.linePoints);
    }
    
    public LinkedList<Paint> getPaints () {
    	return new LinkedList<Paint>(currentState.mLinePaints);
    }
    
    public void addLine (float[] points, Paint paint){
    	if (currentState.currentLinePoints != null) {
    		currentState.linePoints.add(currentState.currentLinePoints);
			Paint p = new Paint ();
			p.setStrokeWidth(currentStrokeWidth);
			p.setColor(currentState.currentColor);
			currentState.mLinePaints.add(p);
    	}
		currentState.currentLinePoints = new float[points.length];
		System.arraycopy(points, 0, currentState.currentLinePoints, 0, points.length);
    	currentStrokeWidth = (int)paint.getStrokeWidth();
    	currentState.currentColor = paint.getColor();
    }

    public void pureAddLine (float[] points, Paint paint){
		linesLayer = null;
    	currentState.linePoints.add (points);
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
    }

    public void pureAddTextObject (TextObject to) {
		currentState.mTextDrawables.add(to);
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
    	if (linesLayer == null) {
    		Canvas canv = canvas;
    		try {
    			linesLayer = Bitmap.createBitmap(mCanvasLimits.right, mCanvasLimits.bottom, Bitmap.Config.ARGB_8888);
    			canv = new Canvas (linesLayer);
        		canv.drawARGB(0, 0, 0, 0);
    		}
    		catch (Exception e) {
    			
    		}
	    	for (int i = 0; i < currentState.linePoints.size(); ++i) {
	    		float[] lp = currentState.linePoints.get (i);
	    		Paint p = currentState.mLinePaints.get (i);
	    		canv.drawLines(lp, p);
	    		for (int j = 0; j < lp.length; j += 4) {
	    			canv.drawCircle(lp[j], lp[j + 1], p.getStrokeWidth() / 2.0f, p);
	    		}
    			canv.drawCircle(lp[lp.length - 2], lp[lp.length - 1], p.getStrokeWidth() / 2.0f, p);
	    	}
    	}
		Paint p = new Paint ();
		p.setColor(this.currentState.currentColor);
		p.setStrokeWidth(currentStrokeWidth);
    	canvas.drawBitmap(linesLayer, 0, 0, p);
    	if (currentState.currentLinePoints == null)  return;
    	canvas.drawLines(currentState.currentLinePoints, p);
		for (int j = 0; j < currentState.currentLinePoints.length; j += 4) {
			canvas.drawCircle(currentState.currentLinePoints[j], currentState.currentLinePoints[j + 1], currentStrokeWidth / 2, p);
		}
		canvas.drawCircle(currentState.currentLinePoints[currentState.currentLinePoints.length - 2], currentState.currentLinePoints[currentState.currentLinePoints.length - 1], p.getStrokeWidth() / 2.0f, p);
    }
    
    private void drawText (Canvas canvas) {
		for (TextObject to : currentState.mTextDrawables) {
			if (to != null) {
				to.draw(canvas);
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
        	dr = getResources().getDrawable(R.drawable.text);
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
        linesLayer = null;
        drawLines (canvas);
        linesLayer = null;
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
		super.onSizeChanged(w, h, oldw, oldh);

	}
	
	private void showModeSelect () {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.mode_select_title));
        builder.setAdapter(modeAdapter,
                        new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                int item) {
//                                        Toast.makeText(getContext(), "You selected: " + item,Toast.LENGTH_LONG).show();
                                	if (item == 0)
                                		mTouchMode = TouchModes.HAND;
                                	else if (item == 1)
                                		mTouchMode = TouchModes.LINE;
                                	else if (item == 2)
                                		mTouchMode = TouchModes.PENCIL;
                                	else if (item == 3)
                                		mTouchMode = TouchModes.TEXT;
                                    dialog.dismiss();
                                    invalidate();
                                }
                        });
        AlertDialog alert = builder.create();
        alert.show();


	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() == 1) {
			mStartDistance = 0.0f;
			if (event.getX() > getWidth() - mModeIconSize
					 && event.getX() < getWidth()
					 && event.getY() > getHeight() - mModeIconSize
					 && event.getY() < getHeight()) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					showModeSelect();
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
	private float mStarta = 0;
	private void handleMultiTouchManipulateEvent (MotionEvent event) {
		wasMultiTouch = true;
		float x1 = event.getX(0);
		float x2 = event.getX(1);
		float y1 = event.getY(0);
		float y2 = event.getY(1);
		float a = (x2 - x1);
		float b =  (y2 - y1);
		float diff = (float)Math.sqrt((a * a + b * b));
		float q = (b / a);
		float rot = (float)Math.toDegrees(Math.atan (q));
		if (mStartDistance < 0.1f) {
			mStartDistance = diff;
			boolean found = false;
	        for (ImageObject io : currentState.mDrawables) {
	        	if (io.isSelected())
	        	{
	        		mStartScale = io.getScale();
	        		mStartRot = io.getRotation();
	        		mPrevRot = rot;
	        		mStarta = a;
	        		found = true;
	        		Log.d ("RAGE", "START MULTITOUCH");
	        		break;
	        	}
	        }
	        if (!found) {
	        	mStartScale = mCanvasScale;
	        }
		}
		else {
			float scale = diff / mStartDistance;
			boolean found = false;
	        for (ImageObject io : currentState.mDrawables) {
	        	float newscale = mStartScale * scale;
	        	float rotdiff = rot - mPrevRot;
	        	if (io.isSelected() && newscale < 10.0f && newscale > 0.1f)
	        	{
	        		float newrot = Math.round((mStartRot + rotdiff) / 1.0f);
	        		if (((a < 0 && mStarta > 0) || (a > 0 && mStarta < 0)) && Math.abs(io.getRotation() - newrot) > 10)
	        			newrot += 180;
	        		if (Math.abs ((newscale - io.getScale()) * 10.0) > Math.abs(newrot - io.getRotation()))
	    	        	io.setScale(newscale);
	        		else
	        			io.setRotation(newrot % 360);
	        		found = true;
	        		break;
	        	}
	        }
	        if (!found) {
	        	float newscale = mStartScale * scale;
	        	if (newscale < 5.0f && newscale > 0.2f) {
	        		mCanvasScale = newscale;
		        	linesLayer = null;
		        	if (zoomChangeListener != null)
		        		zoomChangeListener.ZoomChanged(newscale);
	        	}
	        }
		}
		super.cancelLongPress();
		
	}

	private void handleMultiTouchTextEvent (MotionEvent event) {
		if (currentState.mCurrentText == null)
			return;
		wasMultiTouch = true;
		float x1 = event.getX(0);
		float x2 = event.getX(1);
		float y1 = event.getY(0);
		float y2 = event.getY(1);
		float diff = (float)Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
		if (mStartDistance < 0.1f) {
			mStartDistance = diff;
			mStartScale = 0.0f;
			mStartTextSize = currentState.mCurrentText.getTextSize();
		}
		else {
			float scale = diff / mStartDistance;
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
				if (diffX > 2 || diffY > 2)
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
		        if (!found)  {
		        	linesLayer = null;
		        	if (((mCanvasOffset.x + diffX) < mCanvasLimits.left && diffX > 0)
		        			|| (-(mCanvasOffset.x + diffX) + getWidth () / mCanvasScale <= mCanvasLimits.right) && diffX < 0) 
		        		mCanvasOffset.x += diffX;
		        	if (((mCanvasOffset.y + diffY) < mCanvasLimits.top && diffY > 0)
			        		|| (-(mCanvasOffset.y + diffY) + getHeight () / mCanvasScale <= mCanvasLimits.bottom) && diffY < 0)
		        		mCanvasOffset.y += diffY;
		        	} 
				cancelLongPress();
			}
			else {
				cancelLongPress();
				ImageObject sel = getSelected();
				if (sel != null) {
					int direction = 1;
					double diffSize = event.getX () - mPreviousPos.x;
					if (Math.abs(diffSize) < Math.abs(event.getY () - mPreviousPos.y))
						diffSize = event.getY () - mPreviousPos.y;
					double imgDiag = (Math.sqrt((double)((sel.getIntrinsicWidth()) * (sel.getIntrinsicWidth()) + (sel.getIntrinsicHeight()) * (sel.getIntrinsicHeight()))) / 2.0) * sel.getScale();
					double newScale = ((imgDiag + (double)direction * diffSize) / imgDiag) * sel.getScale ();
					sel.setScale((float)newScale);
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
			pushState ();
			currentState.currentLinePoints = null;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			
			if (currentState.currentLinePoints != null) {
				Paint p = new Paint ();
				p.setColor(this.currentState.currentColor);
				p.setStrokeWidth(currentStrokeWidth);
				float tmp[] = new float[currentState.currentLinePoints.length];
				System.arraycopy(currentState.currentLinePoints, 0, tmp, 0, tmp.length);
				currentState.linePoints.add(tmp);
				currentState.mLinePaints.add (p);
				currentState.currentLinePoints = null;
			}
			linesLayer = null;
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
			int x = (int)(event.getX() / mCanvasScale) - mCanvasOffset.x;
			int y = (int)(event.getY() / mCanvasScale) - mCanvasOffset.y;
			if (mTouchMode == TouchModes.LINE && currentState.currentLinePoints != null && currentState.currentLinePoints.length > 0) {
				currentState.currentLinePoints[currentState.currentLinePoints.length - 2] = x;
				currentState.currentLinePoints[currentState.currentLinePoints.length - 1] = y;
			}
			else {
				float tmp[] = null;
				int len = 0;
				if (currentState.currentLinePoints != null) {
					len = currentState.currentLinePoints.length;
					tmp = new float[len + 4];
					System.arraycopy(currentState.currentLinePoints, 0, tmp, 0, len);
					tmp[len] = tmp[len - 2];
					tmp[len + 1] = tmp[len - 1];
				}
				else {
					tmp = new float[4];
					tmp[0] = mPreviousPos.x / mCanvasScale - mCanvasOffset.x;
					tmp[1] = mPreviousPos.y / mCanvasScale - mCanvasOffset.y;
				}
				tmp[len + 2] = x;
				tmp[len + 3] = y;
				currentState.currentLinePoints = tmp;
			}
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
		linesLayer = null;
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