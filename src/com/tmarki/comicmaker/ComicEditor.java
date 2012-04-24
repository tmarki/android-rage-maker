package com.tmarki.comicmaker;


import java.util.LinkedList;
import java.util.Vector;

import com.flurry.android.FlurryAgent;
import com.tmarki.comicmaker.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.DrawFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.Toast;
import android.net.Uri;
import android.text.format.Time;


public class ComicEditor extends View {
	
	public class ComicState {
	    public ComicState() {
		}
	    @SuppressWarnings("unchecked")
		public ComicState(ComicState os) {
	    	mDrawables = (Vector<ImageObject>)os.mDrawables.clone();
	    	linePoints = new Vector<float[]>();
	    	for (int i = 0; i < os.linePoints.size(); ++i) {
	    		float tmp[] = new float[os.linePoints.get(i).length];
	    		System.arraycopy(os.linePoints.get (i), 0, tmp, 0, os.linePoints.get (i).length);
	    		linePoints.add (tmp);
	    	}
	    		
	    	mLinePaints = new LinkedList<Paint>(os.mLinePaints);
	    	if (os.currentLinePoints != null) {
	    		float src[] = os.currentLinePoints;
	    		currentLinePoints = new float[os.currentLinePoints.length];
	    		System.arraycopy(src, 0, currentLinePoints, 0, os.currentLinePoints.length);
	    	}
	    	mPanelCount = os.mPanelCount;
	    	currentColor = os.currentColor;
		}
	    public Vector<ImageObject> mDrawables = new Vector<ImageObject>();
	    public Vector<float[]> linePoints = new Vector<float[]>();
	    public LinkedList<Paint> mLinePaints = new LinkedList<Paint>();
	    public float[] currentLinePoints = null;
	    public int mPanelCount = 4;
	    public int currentColor = Color.BLACK;
	};
	
	public enum TouchModes { HAND, LINE, PENCIL, TEXT, ERASER };
	public PorterDuffXfermode transparentXfer = new PorterDuffXfermode(Mode.SRC);
	private String[] TMNames = { "Manipulate mode", "Line mode", "Draw mode", "Type mode", "Eraser" };
	

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
	                else if (position == 4)
	                	tile = getResources().getDrawable(R.drawable.eraser);
	               
	                holder.title.setText(TMNames[position]);
	                if (tile != null)
	                	holder.icon.setImageDrawable(tile);
	 
	                return convertView;
	        }
	};	
	
	public interface ZoomChangeListener {
		public void ZoomChanged (float newScale);
	}

	
	private ComicState currentState = new ComicState ();
	
	private Vector<ComicState> previousStates = new Vector<ComicState>();
	
	private Vector<ComicState> poppedStates = new Vector<ComicState>();
	
    private Point mCanvasOffset = new Point (0, 0);
    private Rect mCanvasLimits = new Rect (0, 0, 640, 500);
    private float mCanvasScale = 1.0f;
    private int currentStrokeWidth = 3;
	private Point mPreviousPos = new Point (0, 0); // single touch events
	private boolean resizeObjectMode = false;
    private float mStartDistance = 0.0f;
    private float mStartScale = 0.0f;
    private float mStartRot = 0.0f;
    private float mPrevRot = 0.0f;
    private boolean mMovedSinceDown = false;
    private int defaultFontType = 0;
    private boolean defaultBold = false;
    private boolean defaultItalic = false;
    private int defaultFontSize = 48;
    private boolean drawGrid = true;
    private boolean wasMultiTouch = false;
	private Time lastInvalidate = new Time ();
	private Bitmap linesLayer = null;
	private Bitmap padlock = null;
	private ZoomChangeListener zoomChangeListener = null;
	static public final double ROTATION_STEP = 2.0;
	static public final  double ZOOM_STEP = 0.01;
	static public final float CANVAS_SCALE_MIN = 0.25f;
	static public final float CANVAS_SCALE_MAX = 3.0f;
	static public final int UNDO_STACK_SIZE = 10;
	
	private TouchModes mTouchMode = TouchModes.HAND;
	
	public ComicEditor(Context context, ZoomChangeListener zcl) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        zoomChangeListener = zcl;
        padlock = BitmapFactory.decodeResource(getResources(), R.drawable.padlock, new BitmapFactory.Options ());

    }
	
	public Rect getCanvasDimensions () {
		return mCanvasLimits;
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
	}

	public boolean isDefaultItalic() {
		return defaultItalic;
	}

	public void setDefaultItalic(boolean defaultItalic) {
		this.defaultItalic = defaultItalic;
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
	}
	
	public void setCurrentFont (int ft) {
		defaultFontType = ft;
	}
	
	public void resetObjects () {
		if (linesLayer != null)
			linesLayer.recycle ();
		linesLayer = null;
		pushState ();
		padlock.recycle();
		currentState.mDrawables.clear ();
		currentState.mLinePaints.clear();
		currentState.linePoints.clear ();
		currentState.currentLinePoints = null;
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
			p.setAntiAlias(true);
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
		if (linesLayer != null)
			linesLayer.recycle ();
		linesLayer = null;
    	currentState.linePoints.add (points);
		currentState.mLinePaints.add(paint);
    }

    public void resetHistory () {
    	previousStates.clear();
    }

    public void addImageObject (Bitmap dr, int x, int y, float rot, float scale, int drawableId) {
    	addImageObject(dr, x, y, rot, scale, drawableId, "", "", "");
    }
    
    private void addImageWithPrompt (final Bitmap dr, final int x, final int y, final float rot, final float scale, final int drawableId, final String pack, final String folder, final String file) {
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(getContext()).create();
		alertDialog.setTitle(R.string.select_layer_title);
	    alertDialog.setMessage(getResources ().getString (R.string.select_layer_message));
		alertDialog.setButton(getResources ().getString (R.string.to_front), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
		    	ImageObject z = addImageObjectDirect(dr, x, y, rot, scale, drawableId, pack, folder, file);
		    	z.setInBack(false);
			}
		});
		alertDialog.setButton2(getResources ().getString (R.string.to_back), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
		    	ImageObject z = addImageObjectDirect(dr, x, y, rot, scale, drawableId, pack, folder, file);
		    	z.setInBack(true);
			}
		});
		int newH = (int)(48.0 * ((double)dr.getHeight() / (double)dr.getWidth()));
		if (newH == 0)
			newH = 1;
		BitmapDrawable bd = new BitmapDrawable (Bitmap.createScaledBitmap(dr, 48, newH, true));
		alertDialog.setIcon(bd);
		alertDialog.show();
    	
    }

    private ImageObject addImageObjectDirect (Bitmap dr, int x, int y, float rot, float scale, int drawableId, String pack, String folder, String file) {
		pushState ();
		ImageObject io = new ImageObject(dr, x, y, rot, scale, drawableId, pack, folder, file);
		io.padlock = padlock;
		io.setPosition(new Point (x + io.getWidth() / 2, y + io.getHeight() / 2));
		for (ImageObject ioo : currentState.mDrawables) {
			ioo.setSelected(false);
		}
		io.setSelected(true);
    	currentState.mDrawables.add(io);
    	invalidate ();
    	return io;
    }

    public void addImageObject (Bitmap dr, int x, int y, float rot, float scale, int drawableId, String pack, String folder, String file) {
    	addImageWithPrompt (dr, x, y, rot, scale, drawableId, pack, folder, file);
    }

    public void pureAddImageObject (ImageObject io) {
    	currentState.mDrawables.add(io);
    }


    private void drawGridLines (Canvas canvas) {
    	if (currentState.mPanelCount < 2)
    		return;
        Paint paint = new Paint ();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3.0f);
        paint.setAntiAlias(true);
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
        		if (ad.padlock == null)
        			ad.padlock = padlock;
        		ad.draw(canvas);
        	}
        }
    }

    private void drawLines (Canvas canvas) {
    	drawLines (canvas, true);
    }

	Paint paint = new Paint ();
	Paint erasePaint = new Paint ();
    private void drawLines (Canvas canvas, boolean useLineLayer) {
    	if (linesLayer == null) {
    		Canvas canv = canvas;
    		if (useLineLayer) {
	    		try {
	    			linesLayer = Bitmap.createBitmap(mCanvasLimits.right, mCanvasLimits.bottom, Bitmap.Config.ARGB_8888);
	    			canv = new Canvas (linesLayer);
	        		canv.drawARGB(0, 0, 0, 0);
	    		}
	    		catch (Exception e) {
	    			
	    		}
	    		catch (OutOfMemoryError e) {
	    			Toast.makeText(getContext(), "Out of memory error!",Toast.LENGTH_SHORT).show();
		    		if (linesLayer != null)
		    			linesLayer.recycle ();
	    			linesLayer = null;
	    		}
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
		if (mTouchMode == TouchModes.ERASER && currentState.currentLinePoints != null && linesLayer != null) {
			Canvas canv = new Canvas (linesLayer);
			//p.setAntiAlias(true);
			erasePaint.setARGB(0, 255, 255, 255);
			erasePaint.setXfermode(transparentXfer);
			erasePaint.setStrokeWidth(currentStrokeWidth);
	    	canv.drawLines(currentState.currentLinePoints, erasePaint);
			/*for (int j = 0; j < currentState.currentLinePoints.length; j += 4) {
				canvas.drawCircle(currentState.currentLinePoints[j], currentState.currentLinePoints[j + 1], currentStrokeWidth / 2, p);
			}
			canv.drawCircle(currentState.currentLinePoints[currentState.currentLinePoints.length - 2], currentState.currentLinePoints[currentState.currentLinePoints.length - 1], p.getStrokeWidth() / 2.0f, p);*/
		}
		paint.setAntiAlias(true);
		if (linesLayer != null) {
			canvas.drawBitmap(linesLayer, 0, 0, paint);
		}
    	if (currentState.currentLinePoints == null)  return;
    	paint.setStrokeWidth(currentStrokeWidth);
		if (mTouchMode != TouchModes.ERASER) {
	    	paint.setColor(this.currentState.currentColor);
	    	paint.setStrokeWidth(currentStrokeWidth);
	    	canvas.drawLines(currentState.currentLinePoints, paint);
			for (int j = 0; j < currentState.currentLinePoints.length; j += 4) {
				canvas.drawCircle(currentState.currentLinePoints[j], currentState.currentLinePoints[j + 1], currentStrokeWidth / 2, paint);
			}
			canvas.drawCircle(currentState.currentLinePoints[currentState.currentLinePoints.length - 2], currentState.currentLinePoints[currentState.currentLinePoints.length - 1], paint.getStrokeWidth() / 2.0f, paint);
		}
    }
    
    private void drawModeIcon (Canvas canvas) {
    	Drawable dr;
    	try {
    		if (mTouchMode == TouchModes.HAND)
    			dr = getResources().getDrawable(R.drawable.hand);
    		else if (mTouchMode == TouchModes.PENCIL)
    			dr = getResources().getDrawable(R.drawable.pencil);
    		else if (mTouchMode == TouchModes.LINE)
    			dr = getResources().getDrawable(R.drawable.line);
    		else if (mTouchMode == TouchModes.TEXT)
    			dr = getResources().getDrawable(R.drawable.text);
    		else
    			dr = getResources().getDrawable(R.drawable.eraser);
    	}
    	catch (OutOfMemoryError e) {
    		FlurryAgent.logEvent("Out of memory for drawModeIcon");
    		return;
    	}
        mModeIconSize = (getWidth() > getHeight () ? getWidth () : getHeight ()) / 8; 
        if (getWidth() > getHeight())
        	canvas.translate(getWidth() - mModeIconSize, 0);
        else
        	canvas.translate(getWidth() - mModeIconSize, getHeight() - mModeIconSize);
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        canvas.scale((float)mModeIconSize / dr.getIntrinsicWidth(), (float)mModeIconSize / dr.getIntrinsicHeight());
        RectF r = new RectF (-5.0f, -5.0f, (float)dr.getIntrinsicWidth() + 10.0f, (float)dr.getIntrinsicHeight() + 10.0f);
        Paint p = new Paint ();
        p.setAntiAlias(true);
        p.setColor(currentState.currentColor);
        canvas.drawRoundRect(r, 4, 4, p);
        dr.draw(canvas);
    }
    
    public Bitmap getSaveBitmap () {
    	ImageObject.setInteractiveMode(false);
    	try {
    		Bitmap bmp = Bitmap.createBitmap(mCanvasLimits.right, mCanvasLimits.bottom, Bitmap.Config.ARGB_8888);
    		Canvas canvas = new Canvas (bmp);
    		canvas.drawColor(Color.WHITE);
    		if (drawGrid)
    			drawGridLines (canvas);
    		/*Bitmap qbmp = BitmapFactory.decodeResource(getResources(), R.drawable.qr);
    		canvas.drawBitmap (qbmp, canvas.getWidth() - qbmp.getWidth(), canvas.getHeight() - qbmp.getHeight(), p);
    		qbmp.recycle();*/
    		drawImages (canvas, true);
/*    		if (linesLayer != null)
    			linesLayer.recycle();
    		linesLayer = null;*/
    		drawLines (canvas, true);
    		linesLayer = null;
    		drawImages (canvas, false);
        	return bmp;
    	}
		catch (Exception e) {
			return null;
		}
		catch (Error e) {
			Toast.makeText(getContext(), "Error: " + e.toString(),Toast.LENGTH_SHORT).show();
			return null;
		}
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
                                	else if (item == 4)
                                		mTouchMode = TouchModes.ERASER;
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
			if (getWidth() < getHeight() && event.getX() > getWidth() - mModeIconSize
					 && event.getX() < getWidth()
					 && event.getY() > getHeight() - mModeIconSize
					 && event.getY() < getHeight()) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					showModeSelect();
				}
			}
			else if (getWidth() > getHeight() && event.getX() > getWidth() - mModeIconSize
					 && event.getX() < getWidth()
					 && event.getY() > 0
					 && event.getY() < mModeIconSize) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					showModeSelect();
				}
			}
			else {
				if (mTouchMode == TouchModes.HAND)
					handleSingleTouchManipulateEvent(event);
				else if (mTouchMode == TouchModes.PENCIL || mTouchMode == TouchModes.LINE || mTouchMode == TouchModes.ERASER)
					handleSingleTouchDrawEvent(event);
				else if (mTouchMode == TouchModes.TEXT)
					handleSingleTouchTextEvent(event);
				else
					cancelLongPress();
			}
		}
		else {
			handleMultiTouchManipulateEvent(event);
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
	        	if (io.isSelected() && !io.locked)
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
	        	if (io.isSelected() && newscale < 10.0f && newscale > 0.1f && !io.locked)
	        	{
	        		float newrot = Math.round((mStartRot + rotdiff) / 1.0f);
	        		if (((a < 0 && mStarta > 0) || (a > 0 && mStarta < 0)) && Math.abs(io.getRotation() - newrot) > ROTATION_STEP)
	        			newrot += 180;
	        		if (Math.abs ((newscale - io.getScale()) * ROTATION_STEP) > Math.abs(newrot - io.getRotation()))
	    	        	io.setScale(newscale);
	        		else
	        			io.setRotation(newrot % 360);
	        		found = true;
	        		break;
	        	}
	        }
	        if (!found) {
	        	float newscale = mStartScale * scale;
	        	if (newscale < CANVAS_SCALE_MAX && newscale > CANVAS_SCALE_MIN) {
	        		mCanvasScale = newscale;
	        		if (linesLayer != null)
	        			linesLayer.recycle ();
		        	linesLayer = null;
		        	if (zoomChangeListener != null)
		        		zoomChangeListener.ZoomChanged(newscale);
	        	}
	        }
		}
		super.cancelLongPress();
		
	}

	
	private void handleSingleTouchManipulateEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mMovedSinceDown = false;
			resizeObjectMode = false;
			ImageObject io = getSelected();
			if (io != null) {
				pushState ();
			}
			if (io != null && io.pointInResize ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
				resizeObjectMode = true;
			}
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && !mMovedSinceDown && !resizeObjectMode && !wasMultiTouch) {
			ImageObject tio = getSelected();
			if (tio != null && tio.pointInMenu ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
				showContextMenu();
			}
			else {
				resizeObjectMode = false;
				if (previousStates.size() > 0)
					previousStates.remove(previousStates.size() - 1);
				int selectedId = -1;
				for (int i = currentState.mDrawables.size() - 1; i >= 0; --i) {
					ImageObject io = currentState.mDrawables.elementAt(i);
					if (io.isInBack())
						continue;
					if (io.pointIn ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
		        		io.setSelected(!io.isSelected());
		        		currentState.mDrawables.removeElementAt(i);
		        		currentState.mDrawables.add(io);
						selectedId = currentState.mDrawables.size() - 1;
						break;
					}
				}
				if (selectedId < 0) {
					for (int i = currentState.mDrawables.size() - 1; i >= 0; --i) {
						ImageObject io = currentState.mDrawables.elementAt(i);
						if (!io.isInBack())
							continue;
						if (io.pointIn ((int) (event.getX() / mCanvasScale - mCanvasOffset.x), (int) (event.getY() / mCanvasScale - mCanvasOffset.y))){
			        		io.setSelected(!io.isSelected());
			        		currentState.mDrawables.removeElementAt(i);
			        		currentState.mDrawables.add(io);
							selectedId = currentState.mDrawables.size() - 1;
							break;
						}
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
		        	if (ad.isSelected() && !ad.locked) {
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
		    		if (linesLayer != null)
		    			linesLayer.recycle ();
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
				if (sel != null && !sel.locked) {
					int direction = 1;
					double diffSize = event.getX () - mPreviousPos.x;
					if (Math.abs(diffSize) < Math.abs(event.getY () - mPreviousPos.y))
						diffSize = event.getY () - mPreviousPos.y;
					double imgDiag = (Math.sqrt((double)((sel.getWidth()) * (sel.getWidth()) + (sel.getHeight()) * (sel.getHeight()))) / 2.0) * sel.getScale();
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
		int x = (int)(event.getX() / mCanvasScale) - mCanvasOffset.x;
		int y = (int)(event.getY() / mCanvasScale) - mCanvasOffset.y;
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pushState ();
			currentState.currentLinePoints = null;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			
			if (!wasMultiTouch)
				addLinePoint(x, y);
			if (currentState.currentLinePoints != null) {
				Paint p = new Paint ();
				p.setColor(this.currentState.currentColor);
				p.setStrokeWidth(currentStrokeWidth);
				p.setAntiAlias(true);
				if (mTouchMode == TouchModes.ERASER) {
					p.setXfermode(transparentXfer);
					p.setAlpha(0);
				}
				float tmp[] = new float[currentState.currentLinePoints.length];
				System.arraycopy(currentState.currentLinePoints, 0, tmp, 0, tmp.length);
				currentState.linePoints.add(tmp);
				currentState.mLinePaints.add (p);
				currentState.currentLinePoints = null;
			}
    		if (linesLayer != null)
    			linesLayer.recycle ();
			linesLayer = null;
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE){
			if (mTouchMode == TouchModes.LINE && currentState.currentLinePoints != null && currentState.currentLinePoints.length > 0) {
				currentState.currentLinePoints[currentState.currentLinePoints.length - 2] = x;
				currentState.currentLinePoints[currentState.currentLinePoints.length - 1] = y;
			}
			else {
				addLinePoint(x, y);
			}
		}
		super.cancelLongPress();
		mPreviousPos.x = (int)event.getX();
		mPreviousPos.y = (int)event.getY();
	}
	
	private void addLinePoint (int x, int y) {
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

	private void handleSingleTouchTextEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mMovedSinceDown = false;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP && mMovedSinceDown == false && wasMultiTouch == false) {
			AlertDialog.Builder alert = new AlertDialog.Builder(getContext ());

			alert.setTitle(R.string.enter_text);

			final EditText input = new EditText(getContext());
			alert.setView(input);

			alert.setPositiveButton(getResources ().getString (R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					if (value.length() > 0 && value.replace("\n", "").replace(" ", "").length() > 0) {
						pushState ();
						TextObject to = new TextObject((int)(mPreviousPos.x / mCanvasScale - mCanvasOffset.x), (int)(mPreviousPos.y / mCanvasScale - mCanvasOffset.y),
								defaultFontSize, currentState.currentColor, defaultFontType, value, defaultBold, defaultItalic);
						for (ImageObject io : currentState.mDrawables) {
							io.setSelected(false);
						}
						to.setSelected(true);
						to.setInBack(false);
						currentState.mDrawables.add(to);
						mTouchMode = TouchModes.HAND;
						invalidate();
					}
			  }
			});

			alert.setNegativeButton(getResources ().getString (R.string.cancel), new DialogInterface.OnClickListener() {
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
		poppedStates.clear();
		if (previousStates.size() > UNDO_STACK_SIZE)
			previousStates.removeElementAt(0);
		Log.d ("RAGE", "pushState");
	}
	
	public boolean popState () {
		int pos = previousStates.size () - 1;
		Log.d ("RAGE", "popState");
		if (linesLayer != null)
			linesLayer.recycle ();
		linesLayer = null;
		if (pos >= 0) {
			poppedStates.add(new ComicState (currentState));
			currentState = new ComicState(previousStates.get(pos));
			previousStates.removeElementAt(pos);
			if (poppedStates.size() > UNDO_STACK_SIZE)
				poppedStates.removeElementAt(0);
			invalidate();
			return true;
		}
		else {
//			resetObjects();
		}
		invalidate();
		return false;
	}
	
	public boolean unpopState () {
		int pos = poppedStates.size () - 1;
		Log.d ("RAGE", "unpopState");
		if (pos >= 0) {
			previousStates.add (new ComicState(currentState));
			currentState = new ComicState(poppedStates.get(pos));
			poppedStates.removeElementAt(pos);
    		if (linesLayer != null)
    			linesLayer.recycle ();
			linesLayer = null;
			invalidate();
			return true;
		}
		return false;
	}


	public boolean isDrawGrid() {
		return drawGrid;
	}


	public void setDrawGrid(boolean drawGrid) {
		this.drawGrid = drawGrid;
	}
	
	public void moveEvent (int diffX, int diffY) {
		ImageObject io = getSelected();
		if (io != null) {
			Point p = io.getPosition();
			p.x += diffX;
			p.y += diffY;
			io.setPosition(p);
		}
		else {
        	if (((mCanvasOffset.x + diffX) < mCanvasLimits.left && diffX > 0)
        			|| (-(mCanvasOffset.x + diffX) + getWidth () / mCanvasScale <= mCanvasLimits.right) && diffX < 0) 
        		mCanvasOffset.x += diffX;
        	if (((mCanvasOffset.y + diffY) < mCanvasLimits.top && diffY > 0)
	        		|| (-(mCanvasOffset.y + diffY) + getHeight () / mCanvasScale <= mCanvasLimits.bottom) && diffY < 0)
        		mCanvasOffset.y += diffY;
		}
		invalidate();
	}
	
	public void rotateEvent (float rot) {
		ImageObject io = getSelected();
		if (io != null) {
			float r = io.getRotation();
			io.setRotation(rot + r);
			invalidate();
		}
		
	}
	
	public void scaleEvent (float diff) {
		ImageObject io = getSelected();
		if (io != null) {
			float s = io.getScale();
			io.setScale(diff + s);
		}
		else {
        	float newscale = mCanvasScale + diff;
        	if (newscale < 5.0f && newscale > 0.2f) {
        		mCanvasScale = newscale;
	    		if (linesLayer != null)
	    			linesLayer.recycle ();
	        	linesLayer = null;
	        	if (zoomChangeListener != null)
	        		zoomChangeListener.ZoomChanged(newscale);
        	}
		}
		invalidate();
		
	}
   
	public boolean isRedoAvailable () {
		return poppedStates.size () > 0;
	}

}