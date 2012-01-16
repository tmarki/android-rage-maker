package com.tmarki.comicmaker;

import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CommentNagger extends Dialog {

	private SharedPreferences sharedPref = null;
	private final String DISMISS_TAG = "naggingDismissed";
	
	private View.OnClickListener yesClick = new View.OnClickListener() {
		public void onClick(View v) {
			FlurryAgent.logEvent("Rate Yes");
			Intent goToMarket = null;
			goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.tmarki.comicmaker"));
			getContext().startActivity(goToMarket);			
			setDismissed();
		}
	}; 

	private View.OnClickListener laterClick = new View.OnClickListener() {
		public void onClick(View v) {
			FlurryAgent.logEvent("Rate Later");
			dismiss();
		}
	}; 

	private View.OnClickListener neverClick = new View.OnClickListener() {
		public void onClick(View v) {
			FlurryAgent.logEvent("Rate Never");
			setDismissed();
		}
	}; 

	public CommentNagger(Context context, SharedPreferences sp) {
		super(context);
		sharedPref = sp;
	}
	
	public boolean wasDismissed () {
		return sharedPref.getBoolean(DISMISS_TAG, false);
	}
	
	private void setDismissed () {
		SharedPreferences.Editor ed = sharedPref.edit();
		ed.putBoolean(DISMISS_TAG, true);
		ed.commit();
		dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nagger);
		setTitle(R.string.naggertitle);
		Button b = (Button)findViewById(R.id.button_yes);
		b.setOnClickListener(yesClick);
		b = (Button)findViewById(R.id.button_later);
		b.setOnClickListener(laterClick);
		b = (Button)findViewById(R.id.button_never);
		b.setOnClickListener(neverClick);
	}

}
