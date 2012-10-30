package com.luyun.easyway95;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


public class LYSetting extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener{
	private static final String TAG = "LYSetting";
	
	Easyway95App app;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
	
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
		//setContentView(R.layout.ly_preferences);
        
		app = (Easyway95App)this.getApplication();
        
        //query SharedPreferences
		mSP = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mUserProfile = new UserProfile(mSP);
		this.addPreferencesFromResource(R.xml.preferences);
//		String currentAction = PreferenceManager.getDefaultSharedPreferences(app).getString("map_mgr_preference", null);
//		Preference pref = this.findPreference("map_mgr_preference");
//		if (pref != null) {
//			if (currentAction != null && currentAction.equals("download")) {
//				pref.setSummary(String.format("正在下载深圳地图，大小%.2fmb，完成进度%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//			} else if (currentAction != null && currentAction.equals("pause")){
//				pref.setSummary(String.format("暂停下载深圳地图，大小%.2fmb，完成进度%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//			}
//			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
//				@Override
//				public boolean onPreferenceChange(Preference preference,
//						Object newValue) {
//					// TODO Auto-generated method stub
//					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
//					if (((String)newValue).equals("download")) {
//						preference.setSummary(String.format("正在下载深圳地图，大小%.2fmb，完成进度%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//						app.getMainActivity().downloadMap();
//					} else if (((String)newValue).equals("pause")) {
//						preference.setSummary(String.format("暂停下载深圳地图，大小%.2fmb，完成进度%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//						app.getMainActivity().pauseMap();
//					} else if (((String)newValue).equals("delete")) {
//						preference.setSummary(String.format("当前仅支持深圳地图，建议在WiFi网络下下载"));
//						app.getMainActivity().removeMap();
//					}
//					return true;
//				}   
//			});
//		}
		Preference pref = this.findPreference("homeaddr_preference");
		if (pref != null) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					// TODO Auto-generated method stub
					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
					if (((String)newValue).equals("set")) {
		        		startActivity(new Intent(LYSetting.this, SettingActivity.class));
					}
					return true;
				}   
			});
		}
		pref = this.findPreference("officeaddr_preference");
		if (pref != null) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					// TODO Auto-generated method stub
					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
					if (((String)newValue).equals("set")) {
		        		startActivity(new Intent(LYSetting.this, SettingActivity.class));
					}
					return true;
				}   
			});
		}
	}
	

	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {		
		// TODO Auto-generated method stub
		Log.d(TAG, "enter onPreferenceTreeClick");
	    if (preference.getKey().equals("user_preference")) {
	    	Log.d(TAG, "click user_prefereces");
    		startActivity(new Intent(LYSetting.this, LoginActivity.class));
	    } else if (preference.getKey().equals("map_mgr_preference")) {
	    	Log.d(TAG, "click map_mgr_preference");
	    } else if (preference.getKey().equals("homepage_preference")) {
	    	Log.d(TAG, "click homepage_preference");
	    }
		return true;
	}


	@Override
	public boolean onPreferenceChange(Preference preference, Object arg1) {
		// TODO Auto-generated method stub
		Log.d(TAG, "in onPreferenceChange");
		return true;
	}


	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		Log.d(TAG, "in onPreferenceClick");
		return false;
	}

}
