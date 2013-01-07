package com.luyun.easyway95;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.waps.AppConnect;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		mLPHome = (ListPreference)findPreference("homeaddr_preference");  
    	mLPOffice = (ListPreference)findPreference("officeaddr_preference"); 
    	
		mLPHome.setSummary(mUserProfile.getHomeAddr().getName());
		mLPOffice.setSummary(mUserProfile.getOfficeAddr().getName());
		
        //获取版本信息
		String versionName = null;
        try {
        	PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        	versionName = info.versionName;
        } catch (Exception e) {
        	e.printStackTrace();
        }
		Preference release_preference = findPreference("release_preference"); 
		release_preference.setSummary(versionName);
		 
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
		final CheckBoxPreference pref = (CheckBoxPreference)this.findPreference("traffic_layer_preference");
		if (app.getTrafficLayerFlag()) {
			pref.setChecked(true);
		} else {
			pref.setChecked(false);			
		}
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {  
				@Override
				public boolean onPreferenceClick(Preference preference
						) {
					// TODO Auto-generated method stub
					boolean checked = pref.isChecked();
					app.setTrafficLayerFlag(checked);
					app.getMainActivity().toggleTrafficLayer(checked);
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

				Intent cronintent = new Intent(this, LYCronService.class);
				String key = "subscription";

			} else {
				Log.d(TAG, "unknown requestCode: " + requestCode);
			}
			CheckBoxPreference pref = (CheckBoxPreference) this
					.findPreference("subscription_preference");
			if (pref.isChecked()) {
				app.getMainActivity().mcService.onAddressUpdate();
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
    	Intent intent = new Intent(LYSetting.this, LoginActivity.class);
		String strKey = "url";
	    if (preference.getKey().equals("user_preference")) {
	    	Log.d(TAG, "click user_prefereces");
	    	intent.putExtra(strKey, Constants.USERS_PROFILE_URL);
    		startActivity(intent);
	    } else if (preference.getKey().equals("map_mgr_preference")) {
	    	Log.d(TAG, "click map_mgr_preference");
	    } else if (preference.getKey().equals("homepage_preference")) {
	    	Log.d(TAG, "click homepage_preference");
	    } else if (preference.getKey().equals("promotion_preference")) {
	    	intent.putExtra(strKey, Constants.PROMOTION_URL);
    		startActivity(intent);
	    } else if (preference.getKey().equals("question_more")) {
	    	intent.putExtra(strKey, Constants.FAQ_URL);
    		startActivity(intent);
	    } else if (preference.getKey().equals("ad_preference")) {
			AppConnect.getInstance(this).showOffers(this);
	    }
	    else if(preference.getKey().equals("subscription_preference")){
	    	final CheckBoxPreference pref = (CheckBoxPreference)this.findPreference("subscription_preference");
	    	Log.d(TAG, "is checked : " + pref.isChecked());
    		app.getMainActivity().mcService.onSubscription(pref.isChecked());
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
