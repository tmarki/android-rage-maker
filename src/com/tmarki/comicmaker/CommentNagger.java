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
			goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=com.tmarki.comicmaker"));
			//goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.tmarki.comicmaker"));
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
