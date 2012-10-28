package com.luyun.easyway95;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.view.WindowManager;


public class LYSetting extends PreferenceActivity {
	private static final String TAG = "LYSetting";
	
	Easyway95App app;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
	
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
        
		app = (Easyway95App)this.getApplication();
        
        //query SharedPreferences
		mSP = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mUserProfile = new UserProfile(mSP);
        addPreferencesFromResource(R.xml.preferences);
	}
	

	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}

}
