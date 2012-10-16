package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.ItemizedOverlay;
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
import com.baidu.mapapi.Overlay;
import com.baidu.mapapi.OverlayItem;
import com.baidu.mapapi.PoiOverlay;
import com.baidu.mapapi.Projection;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;


public class SettingActivity extends MapActivity {
	private static final String TAG = "SettingActivity";
	
	Easyway95App app;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
	
	private AddrType mAddrProcessing = AddrType.HOME_ADDR;
	private CheckBox mHomeSelected;
	private CheckBox mOfficeSelected;
	
	ImageButton mBtnSearch = null;	// 搜索按钮
	Button mSuggestionSearch = null;  //suggestion搜索
	ListView mSuggestionList = null;
	public static String mStrSuggestions[] = {};
	private String mSearchKey;
	
	private GeoPoint mPointTapped;
	
	MapView mMapView = null;	// 地图View
	MKSearch mSearch = null;	// 搜索模块，也可去掉地图模块独立使用
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
        setContentView(R.layout.setting);
        
		app = (Easyway95App)this.getApplication();
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        // 如果使用地图SDK，请初始化地图Activity
        super.initMapActivity(app.mBMapMan);
        
        mMapView = (MapView)findViewById(R.id.poi_search_view);
        mMapView.setBuiltInZoomControls(true);
        //设置在缩放动画过程中也显示overlay,默认为不绘制
        mMapView.setDrawOverlayWhenZooming(true);
        LongTap lt = new LongTap();
        mMapView.getOverlays().add(lt);
        mMapView.invalidate();
		
        mHomeSelected = (CheckBox)findViewById(R.id.homeselected);
        mHomeSelected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					mAddrProcessing = AddrType.HOME_ADDR;
					mOfficeSelected.setChecked(false);
					GeoPoint pt = mUserProfile.getHomeAddr().getPt();
					mMapView.getController().animateTo(pt);
				} else {
					mAddrProcessing = AddrType.OFFICE_ADDR;
					mOfficeSelected.setChecked(true);
					GeoPoint pt = mUserProfile.getOfficeAddr().getPt();
					mMapView.getController().animateTo(pt);
				}
			}
		});
		mOfficeSelected = (CheckBox)findViewById(R.id.officeselected);
		mOfficeSelected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					mAddrProcessing = AddrType.OFFICE_ADDR;
					mHomeSelected.setChecked(false);
					GeoPoint pt = mUserProfile.getOfficeAddr().getPt();
					mMapView.getController().animateTo(pt);
				} else {
					mAddrProcessing = AddrType.HOME_ADDR;
					mHomeSelected.setChecked(true);
					GeoPoint pt = mUserProfile.getHomeAddr().getPt();
					mMapView.getController().animateTo(pt);
				}
			}
		});
		
        // 初始化搜索模块，注册事件监听
        mSearch = new MKSearch();
        mSearch.init(app.mBMapMan, new MKSearchListener(){
			public void onGetPoiResult(MKPoiResult res, int type, int error) {
				// 错误号可参考MKEvent中的定义
				if (error != 0 || res == null) {
					Toast.makeText(SettingActivity.this, "抱歉，未找到结果", Toast.LENGTH_LONG).show();
					return;
				}

			    // 将地图移动到第一个POI中心点
			    if (res.getCurrentNumPois() > 0) {
				    // 将poi结果显示到地图上
					PoiOverlay poiOverlay = new PoiOverlay(SettingActivity.this, mMapView);
					//poiOverlay.setData(res.getAllPoi());
					ArrayList<MKPoiInfo> poiResults = new ArrayList(1);
					poiResults.add(res.getPoi(0));
					poiOverlay.setData(poiResults);
			    	
			    	//将结果传回给SettingActivity
			    	//2012.09.25直接在poisearch中处理搜索结果，故将传递消息功能注释掉
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
			        if (mAddrProcessing == AddrType.HOME_ADDR) {
			        	mUserProfile.setHomeAddr(mpi);
			        } else {
			        	mUserProfile.setOfficeAddr(mpi);
			        }
	            	mUserProfile.commitPreferences(mSP);			    	
	        		resetTextView();
				    mMapView.getOverlays().clear();
				    mMapView.getOverlays().add(poiOverlay);
			        LongTap lt = new LongTap();
			        mMapView.getOverlays().add(lt);
			        setMarkers();
				    mMapView.invalidate();
			    	mMapView.getController().animateTo(res.getPoi(0).pt);
			    } else if (res.getCityListNum() > 0) {
			    	String strInfo = "在";
			    	for (int i = 0; i < res.getCityListNum(); i++) {
			    		strInfo += res.getCityListInfo(i).city;
			    		strInfo += ",";
			    	}
			    	strInfo += "找到结果";
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
				if (error != 0) {
					String str = String.format("错误号：%d", error);
					Toast.makeText(SettingActivity.this, str, Toast.LENGTH_LONG).show();
					return;
				}

				mMapView.getController().animateTo(res.geoPt);
					
				String strInfo = String.format("纬度：%f 经度：%f\r\n, name: %s", res.geoPt.getLatitudeE6()/1e6, 
							res.geoPt.getLongitudeE6()/1e6, res.strAddr);

				Toast.makeText(SettingActivity.this, strInfo, Toast.LENGTH_LONG).show();
				Log.d(TAG, strInfo);
		        MKPoiInfoHelper mpi = mUserProfile.new MKPoiInfoHelper(res);
		        if (mAddrProcessing == AddrType.HOME_ADDR) {
		        	mUserProfile.setHomeAddr(mpi);
		        } else {
		        	mUserProfile.setOfficeAddr(mpi);
		        }
            	mUserProfile.commitPreferences(mSP);			    	
        		resetTextView();
				
				mMapView.getOverlays().clear();
		        LongTap lt = new LongTap();
		        mMapView.getOverlays().add(lt);
		        setMarkers();
		        mMapView.getController().animateTo(res.geoPt);
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
		setMarkers();
		mHomeSelected.setChecked(true);
		
        // 设定搜索按钮的响应
        OnClickListener clickListener = new OnClickListener(){
			public void onClick(View v) {
				SearchButtonProcess(v);
			}
        };
        mBtnSearch = (ImageButton)findViewById(R.id.search);
        mBtnSearch.setOnClickListener(clickListener); 
	}
	
	public void setMarkers() {
		GeoPoint ptHome = mUserProfile.getHomeAddr().getPt();
		mMapView.getController().animateTo(ptHome);
		Drawable markerHome = getResources().getDrawable(R.drawable.bubble_48);  //得到需要标在地图上的资源
		markerHome.setBounds(0, 0, markerHome.getIntrinsicWidth(), markerHome
				.getIntrinsicHeight());   //为maker定义位置和边界
		GeoPoint ptOffice = mUserProfile.getOfficeAddr().getPt();
		mMapView.getController().animateTo(ptOffice);
		Drawable markerOffice = getResources().getDrawable(R.drawable.bubble_48);  //得到需要标在地图上的资源
		markerHome.setBounds(0, 0, markerHome.getIntrinsicWidth(), markerOffice
				.getIntrinsicHeight());   //为maker定义位置和边界
		mMapView.getOverlays().add(new OverItemT(markerHome, SettingActivity.this, ptHome, null));
		mMapView.getOverlays().add(new OverItemT(markerOffice, SettingActivity.this, ptOffice, null));
	}
	
	void SearchButtonProcess(View v) {
		if (mBtnSearch.equals(v)) {
			EditText editSearchKey = (EditText)findViewById(R.id.searchkey);
			mSearch.poiSearchInCity("深圳", 
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
		
//		TextView txtHomeLatLng = (TextView)findViewById(R.id.home_lat_lng);
//		tmpString = mUserProfile.getHomeLatLng();
//		if (tmpString != null && tmpString.length() > 0)
//			txtHomeLatLng.setText(tmpString);
//		
		TextView txtOffice = (TextView)findViewById(R.id.officeaddr);
		tmpString = mUserProfile.getOfficeAddr().getName();
		if (tmpString != null && tmpString.length() > 0)
			txtOffice.setText(tmpString);

//		TextView txtOfficeLatLng = (TextView)findViewById(R.id.office_lat_lng);
//		tmpString = mUserProfile.getOfficeLatLng();
//		if (tmpString != null && tmpString.length() > 0)
//			txtOfficeLatLng.setText(tmpString);
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
	
	
	public class LongTap extends Overlay implements OnDoubleTapListener, OnGestureListener {
		private GestureDetector mGestureDetector = new GestureDetector(this); 
		private boolean isLongPress = false;
		private long time;

	    @Override  
	    public boolean onTouchEvent(MotionEvent arg0, MapView mapView) {  
	    	Log.d(TAG, "in onTouchEvent");
	        float x = arg0.getX();  
	        float y = arg0.getY();  
	        switch (arg0.getAction()) {  
	        case MotionEvent.ACTION_DOWN:  
	            time = System.currentTimeMillis();  
	            isLongPress = false;  
	            break;  
	  
	        case MotionEvent.ACTION_UP:  
	            if (System.currentTimeMillis() - time > 3000) {  
	                isLongPress = true;  
	            }  
	            break;  
	        default:  
	            break;  
	        }  
	        return mGestureDetector.onTouchEvent(arg0);  
	    }  
	    
		@Override
		public boolean onDoubleTap(MotionEvent arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDown(MotionEvent arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
				float arg3) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			Log.d(TAG, "in onLongPress,"+e.toString());
	        float x = e.getX();  
	        float y = e.getY();  
	        Projection pj = mMapView.getProjection();
        	mPointTapped = pj.fromPixels((int) x, (int) y);
        	
			//Log.d(TAG, "in onLongPress,"+pt.toString());
        	//调用一次反向地理解码，获取位置描述信息
        	mSearch.reverseGeocode(mPointTapped);
	        isLongPress = false;  
		}

		@Override
		public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
				float arg3) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent arg0) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	public enum AddrType {
		HOME_ADDR, 
		OFFICE_ADDR;
	}
	
	class OverItemT extends ItemizedOverlay<OverlayItem>{
		private List<OverlayItem> mGeoList = new ArrayList<OverlayItem>();

		public OverItemT(Drawable marker, Context context, GeoPoint pt, String title) {
			super(boundCenterBottom(marker));
			
			mGeoList.add(new OverlayItem(pt, title, null));

			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mGeoList.get(i);
		}

		@Override
		public int size() {
			return mGeoList.size();
		}

		@Override
		public boolean onSnapToItem(int i, int j, Point point, MapView mapview) {
			Log.e("ItemizedOverlayDemo","enter onSnapToItem()!");
			return false;
		}
	}
}
