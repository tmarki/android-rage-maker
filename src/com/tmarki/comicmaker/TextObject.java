package com.tmarki.comicmaker;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.style.TypefaceSpan;


public class TextObject {
	enum FontType { MONOSPACE, SERIF, SANS }; 
	private int x = 0;
	private int y = 0;
	private int textSize = 20;
	private int color = Color.BLACK;
	private FontType typeface = FontType.MONOSPACE;
	private String text;
	private boolean bold = false;
	private boolean italic = false;
	public TextObject (int xx, int yy, int ts, int col, FontType tf, String ss) {
		x = xx;
		y = yy;
		text = ss;
		textSize = ts;
		color = col;
		typeface = tf;
	}
	public int getTextSize() {
		return textSize;
	}
	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}
	public int getColor() {
		return color;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public FontType getTypeface() {
		return typeface;
	}
	public void setTypeface(FontType typeface) {
		this.typeface = typeface;
	}
	public boolean isBold() {
		return bold;
	}
	public void setBold(boolean bold) {
		this.bold = bold;
	}
	public boolean isItalic() {
		return italic;
	}
	public void setItalic(boolean italic) {
		this.italic = italic;
	}
	public TextObject (int xx, int yy, int ts, int col, FontType tf, String ss, boolean bld, boolean itlic) {
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
	public Typeface getTypefaceObj () {
		return getTypefaceObj(typeface, bold, italic);
	}
	static public Typeface getTypefaceObj (FontType ft, boolean bol, boolean ita) {
		Typeface tmptf = Typeface.DEFAULT;
		if (ft == FontType.MONOSPACE)
			tmptf = Typeface.MONOSPACE;
		if (ft == FontType.SANS)
			tmptf = Typeface.SANS_SERIF;
		else if (ft == FontType.SERIF)
			tmptf = Typeface.SERIF;
		if (bol && !ita)
			tmptf = Typeface.create(tmptf, Typeface.BOLD);
		if (ita && !bol)
			tmptf = Typeface.create(tmptf, Typeface.ITALIC);
		if (ita && bol)
			tmptf = Typeface.create(tmptf, Typeface.BOLD_ITALIC);
		return tmptf;
	}
	public void draw (Canvas canvas) {
		Paint p = new Paint();
    	p.setColor(color);
		p.setStyle(Paint.Style.FILL);
		p.setTextSize(textSize);
		canvas.save();
		p.setAntiAlias(true);
		p.setTypeface(getTypefaceObj());
		canvas.translate(x, y);
		int i = 0;
		for (String s : text.split("\n"))
		{
			canvas.drawText(s, 0, i * textSize, p);
			i++;
		}
		canvas.restore();
	}
}

