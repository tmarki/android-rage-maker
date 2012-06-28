package com.tmarki.comicmaker;

import java.util.LinkedList;
import java.util.Vector;

import android.graphics.Color;
import android.graphics.Paint;

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
    	drawGrid = os.drawGrid;
	}
    public Vector<ImageObject> mDrawables = new Vector<ImageObject>();
    public Vector<float[]> linePoints = new Vector<float[]>();
    public LinkedList<Paint> mLinePaints = new LinkedList<Paint>();
    public float[] currentLinePoints = null;
    public int mPanelCount = 4;
    public int currentColor = Color.BLACK;
    public boolean drawGrid = true;
}