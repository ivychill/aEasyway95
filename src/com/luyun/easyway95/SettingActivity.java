package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPoiInfo;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.PoiOverlay;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;


public class SettingActivity extends MapActivity {
	private static final String TAG = "SettingActivity";
	
	Easyway95App app;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
	
	Button mBtnSearchHome = null;	// ������ť
	Button mBtnSearchOffice = null;	// ������ť
	Button mSuggestionSearch = null;  //suggestion����
	ListView mSuggestionList = null;
	public static String mStrSuggestions[] = {};
	private String mSearchKey;
	private String mSearchPlace;
	
	MapView mMapView = null;	// ��ͼView
	MKSearch mSearch = null;	// ����ģ�飬Ҳ��ȥ����ͼģ�����ʹ��
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
        setContentView(R.layout.poisearch);
        
		app = (Easyway95App)this.getApplication();
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        // ���ʹ�õ�ͼSDK�����ʼ����ͼActivity
        super.initMapActivity(app.mBMapMan);
        
        mMapView = (MapView)findViewById(R.id.poi_search_view);
        mMapView.setBuiltInZoomControls(true);
        //���������Ŷ���������Ҳ��ʾoverlay,Ĭ��Ϊ������
        mMapView.setDrawOverlayWhenZooming(true);
        
        // ��ʼ������ģ�飬ע���¼�����
        mSearch = new MKSearch();
        mSearch.init(app.mBMapMan, new MKSearchListener(){
			public void onGetPoiResult(MKPoiResult res, int type, int error) {
				// ����ſɲο�MKEvent�еĶ���
				if (error != 0 || res == null) {
					Toast.makeText(SettingActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
					return;
				}

			    // ����ͼ�ƶ�����һ��POI���ĵ�
			    if (res.getCurrentNumPois() > 0) {
				    // ��poi�����ʾ����ͼ��
					PoiOverlay poiOverlay = new PoiOverlay(SettingActivity.this, mMapView);
					//poiOverlay.setData(res.getAllPoi());
					ArrayList<MKPoiInfo> poiResults = new ArrayList(1);
					poiResults.add(res.getPoi(0));
					poiOverlay.setData(poiResults);
				    mMapView.getOverlays().clear();
				    mMapView.getOverlays().add(poiOverlay);
				    mMapView.invalidate();
			    	mMapView.getController().animateTo(res.getPoi(0).pt);
			    	
			    	//��������ظ�SettingActivity
			    	//2012.09.25ֱ����poisearch�д�������������ʽ�������Ϣ����ע�͵�
			        //Message msg = new Message();
			        //Bundle bdl = new Bundle();
			        //UserProfile up = new UserProfile();
			        //MKPoiInfoHelper mpi = up.new MKPoiInfoHelper(res.getPoi(0));
			        //MKPoiInfoHelper mpi = new UserProfile().setHomeAddr(res.getPoi(0));// 
			        //mpi.setSearchPlace(mSearchPlace);
			        //bdl.putSerializable(Constants.POI_SEARCH_RESULT, mpi);
			        //msg.setData(bdl);
			        // The PendingIntent to launch our activity
			        //PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
			        //        new Intent(getApplicationContext(), MainActivity.class), 0);
			        //((MainActivity)getApplicationContext()).handler.sendMessage(msg);
			        //app.getSettingActivity().handler.sendMessage(msg);
			    	
			        MKPoiInfoHelper mpi = mUserProfile.new MKPoiInfoHelper(res.getPoi(0));
			        if (mSearchPlace.equals("home")) {
			        	mUserProfile.setHomeAddr(mpi);
			        } else {
			        	mUserProfile.setOfficeAddr(mpi);
			        }
	            	mUserProfile.commitPreferences(mSP);			    	
	        		resetTextView();
			    } else if (res.getCityListNum() > 0) {
			    	String strInfo = "��";
			    	for (int i = 0; i < res.getCityListNum(); i++) {
			    		strInfo += res.getCityListInfo(i).city;
			    		strInfo += ",";
			    	}
			    	strInfo += "�ҵ����";
					Toast.makeText(SettingActivity.this, strInfo, Toast.LENGTH_LONG).show();
			    }
			}
			public void onGetDrivingRouteResult(MKDrivingRouteResult res,
					int error) {
			}
			public void onGetTransitRouteResult(MKTransitRouteResult res,
					int error) {
			}
			public void onGetWalkingRouteResult(MKWalkingRouteResult res,
					int error) {
			}
			public void onGetAddrResult(MKAddrInfo res, int error) {
			}
			public void onGetBusDetailResult(MKBusLineResult result, int iError) {
			}
			@Override
			public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
				// TODO Auto-generated method stub
			}
			
        });
        
        //login
        Button btnLogin = (Button)findViewById(R.id.login);
        btnLogin.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		startActivity(new Intent(SettingActivity.this, LoginActivity.class));
        	}
        });

        
        //query SharedPreferences
		mSP = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mUserProfile = new UserProfile(mSP);
		retrieveAuthToken();
		retrieveSessionToken();
		resetTextView();
		
        // �趨������ť����Ӧ
        OnClickListener clickListener = new OnClickListener(){
			public void onClick(View v) {
				SearchButtonProcess(v);
			}
        };
        mBtnSearchHome = (Button)findViewById(R.id.search_home);
        mBtnSearchHome.setOnClickListener(clickListener); 
        mBtnSearchOffice = (Button)findViewById(R.id.search_office);
        mBtnSearchOffice.setOnClickListener(clickListener); 
	}
	void SearchButtonProcess(View v) {
		if (mBtnSearchHome.equals(v)) {
			mSearchPlace = "home";
			EditText editSearchKey = (EditText)findViewById(R.id.searchkey);
			mSearch.poiSearchInCity("����", 
					editSearchKey.getText().toString());
		}
		if (mBtnSearchOffice.equals(v)) {
			mSearchPlace = "office";
			EditText editSearchKey = (EditText)findViewById(R.id.searchkey);
			mSearch.poiSearchInCity("����", 
					editSearchKey.getText().toString());
		}
	}

	@Override
	protected void onPause() {
		Easyway95App app = (Easyway95App)this.getApplication();
		app.mBMapMan.stop();
		super.onPause();
	}
	@Override
	protected void onResume() {
		Easyway95App app = (Easyway95App)this.getApplication();
		app.mBMapMan.start();
		super.onResume();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void resetTextView() {
		TextView txtUserName = (TextView)findViewById(R.id.username);
		String tmpString = mUserProfile.getUserName();
		if (tmpString != null && tmpString.length() > 0) {
			txtUserName.setText(tmpString);
		} else {
			mUserProfile.getProfileFromSvr();
		}
		
		TextView txtHome = (TextView)findViewById(R.id.homeaddr);
		tmpString = mUserProfile.getHomeAddr().getName();
		if (tmpString != null && tmpString.length() > 0)
			txtHome.setText(tmpString);
		
		TextView txtHomeLatLng = (TextView)findViewById(R.id.home_lat_lng);
		tmpString = mUserProfile.getHomeLatLng();
		if (tmpString != null && tmpString.length() > 0)
			txtHomeLatLng.setText(tmpString);
		
		TextView txtOffice = (TextView)findViewById(R.id.officeaddr);
		tmpString = mUserProfile.getOfficeAddr().getName();
		if (tmpString != null && tmpString.length() > 0)
			txtOffice.setText(tmpString);

		TextView txtOfficeLatLng = (TextView)findViewById(R.id.office_lat_lng);
		tmpString = mUserProfile.getOfficeLatLng();
		if (tmpString != null && tmpString.length() > 0)
			txtOfficeLatLng.setText(tmpString);
	}

	/** get the stored cookies */
	private void retrieveAuthToken() {
		Log.d(TAG, "in retrieveAuthToken");
		CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
	    CookieManager cookieManager = CookieManager.getInstance();
	    String cookie = cookieManager.getCookie(Constants.USERS_PROFILE_URL);
	    if (cookie != null) {
	    	Log.d(TAG, cookie);
	    	Pattern reg = Pattern.compile("auth_token=(.+)(;*)");
	    	Matcher mch = reg.matcher(cookie);
	    	if (mch.find()) {
	    		String s0 = mch.group(0);
	    		String s1 = mch.group(1);
	    		mUserProfile.setAuthToken(s1);
	    		return;
	    	}
	    }
	}

	/** get the stored session tokens cookies */
	private void retrieveSessionToken() {
		CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
	    CookieManager cookieManager = CookieManager.getInstance();
	    String cookie = cookieManager.getCookie(Constants.USERS_PROFILE_URL);
	    if (cookie != null) {
	    	Log.d(TAG, cookie);
	    	Pattern reg = Pattern.compile("_roadclouding_session=(.+)(;*)");
	    	Matcher mch = reg.matcher(cookie);
	    	if (mch.find()) {
	    		String s0 = mch.group(0);
	    		String s1 = mch.group(1);
	    		mUserProfile.setSessionId(s1);
	    		return;
	    	}
	    }
	}
}
