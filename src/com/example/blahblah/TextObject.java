package com.example.blahblah;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.style.TypefaceSpan;


public class TextObject {
	private int x = 0;
	private int y = 0;
	private int textSize = 20;
	private int color = Color.BLACK;
	private Typeface typeface = Typeface.MONOSPACE;
	private String text;
	private boolean bold = false;
	private boolean italic = false;
	public TextObject (int xx, int yy, int ts, int col, Typeface tf, String ss) {
		x = xx;
		y = yy;
		text = ss;
		textSize = ts;
		color = col;
		typeface = tf;
	}
	public TextObject (int xx, int yy, int ts, int col, Typeface tf, String ss, boolean bld, boolean itlic) {
		x = xx;
		y = yy;
		text = ss;
		textSize = ts;
		color = col;
		typeface = tf;
		bold = bld;
		italic = itlic;
	}
	public TextObject(TextObject other) {
		x = other.x;
		y = other.y;
		text = other.text;
		textSize = other.textSize;
		color = other.color;
		typeface = other.typeface;
		bold = other.bold;
		italic = other.italic;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public void draw (Canvas canvas) {
		Paint p = new Paint();
    	p.setColor(color);
		p.setStyle(Paint.Style.FILL);
		p.setTextSize(textSize);
		canvas.save();
		p.setAntiAlias(true);
		Typeface tmptf = typeface;
		if (bold && !italic)
			tmptf = Typeface.create(typeface, Typeface.BOLD);
		if (italic && !bold)
			tmptf = Typeface.create(typeface, Typeface.ITALIC);
		if (italic && bold)
			tmptf = Typeface.create(typeface, Typeface.BOLD_ITALIC);
		p.setTypeface(tmptf);
		canvas.translate(x, y);
		canvas.drawText(text, 0, 0, p);
		canvas.restore();
	}
}

