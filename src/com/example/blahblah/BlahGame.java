package com.example.blahblah;

import android.app.Activity;
import android.os.Bundle;

import com.example.blahblah.BlahView;



public class BlahGame extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new BlahView(this));
    }
    

}