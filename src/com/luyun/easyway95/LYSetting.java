package com.luyun.easyway95;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


public class LYSetting extends PreferenceActivity
	implements OnPreferenceChangeListener, OnPreferenceClickListener, OnSharedPreferenceChangeListener {
	
	private static final String TAG = "LYSetting";
	
	Easyway95App app;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
	private ListPreference mLPHome;
	private ListPreference mLPOffice;
	
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
//				pref.setSummary(String.format("�����������ڵ�ͼ����С%.2fmb����ɽ���%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//			} else if (currentAction != null && currentAction.equals("pause")){
//				pref.setSummary(String.format("��ͣ�������ڵ�ͼ����С%.2fmb����ɽ���%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//			}
//			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
//				@Override
//				public boolean onPreferenceChange(Preference preference,
//						Object newValue) {
//					// TODO Auto-generated method stub
//					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
//					if (((String)newValue).equals("download")) {
//						preference.setSummary(String.format("�����������ڵ�ͼ����С%.2fmb����ɽ���%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//						app.getMainActivity().downloadMap();
//					} else if (((String)newValue).equals("pause")) {
//						preference.setSummary(String.format("��ͣ�������ڵ�ͼ����С%.2fmb����ɽ���%d%%", app.getMainActivity().getMapSize(), app.getMainActivity().downloadProgress()));
//						app.getMainActivity().pauseMap();
//					} else if (((String)newValue).equals("delete")) {
//						preference.setSummary(String.format("��ǰ��֧�����ڵ�ͼ��������WiFi����������"));
//						app.getMainActivity().removeMap();
//					}
//					return true;
//				}   
//			});
//		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		mLPHome = (ListPreference)findPreference("homeaddr_preference");  
    	mLPOffice = (ListPreference)findPreference("officeaddr_preference"); 
    	
		mLPHome.setSummary(mUserProfile.getHomeAddr().getName());
		mLPOffice.setSummary(mUserProfile.getOfficeAddr().getName());
		 
//		Preference pref = this.findPreference("homeaddr_preference");
		if (mLPHome != null) {
			mLPHome.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					// TODO Auto-generated method stub
					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
					if (((String)newValue).equals("set")) {
		    			final Intent searchIntent = new Intent(LYSetting.this, SearchActivity.class);
		    			startActivityForResult(searchIntent, Constants.HOME_REQUEST_CODE);
					}
					return true;
				}   
			});
		}
//		pref = this.findPreference("officeaddr_preference");
		if (mLPOffice != null) {
			mLPOffice.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					// TODO Auto-generated method stub
					Log.d(TAG, "in setOnPreferenceChangeListener!"+newValue.toString());
					if (((String)newValue).equals("set")) {
		    			final Intent searchIntent = new Intent(LYSetting.this, SearchActivity.class);
		    			startActivityForResult(searchIntent, Constants.OFFICE_REQUEST_CODE);
					}
					return true;
				}   
			});
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.HOME_REQUEST_CODE) { 
				Bundle bundle = intent.getExtras(); 
				MKPoiInfoHelper poiInfo = (MKPoiInfoHelper)bundle.getSerializable(Constants.POI_RETURN_KEY);
				Log.d(TAG, "poi: " + poiInfo.toString());
				mUserProfile.setHomeAddr(poiInfo);
				mUserProfile.commitPreferences(mSP);
				mLPHome.setSummary(poiInfo.getName());
			} else if (requestCode == Constants.OFFICE_REQUEST_CODE) {
				Bundle bundle = intent.getExtras(); 
				MKPoiInfoHelper poiInfo = (MKPoiInfoHelper)bundle.getSerializable(Constants.POI_RETURN_KEY);
				Log.d(TAG, "poi: " + poiInfo.toString());
				mUserProfile.setOfficeAddr(poiInfo);
				mUserProfile.commitPreferences(mSP);
				mLPOffice.setSummary(poiInfo.getName());
			} else {
				Log.d(TAG, "unknown requestCode: " + requestCode);
			}
		} else {
			Log.d(TAG, "unknown resultCode: " + resultCode);
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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
//		Log.d(TAG, "in onSharedPreferenceChanged");
//        if(key.equals("homeaddr_preference")){  
//        	mLPHome.setSummary(mLPHome.getEntry());  
//        }  
//        if(key.equals("officeaddr_preference")){  
//        	mLPOffice.setSummary(mLPOffice.getEntry()); 
//        }
	}
}
