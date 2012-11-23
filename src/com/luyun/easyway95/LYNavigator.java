package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.LocationListener;
import com.baidu.mapapi.MKPoiInfo;
import com.baidu.mapapi.MKOLSearchRecord;
import com.baidu.mapapi.MKOLUpdateElement;
import com.baidu.mapapi.MKOfflineMap;
import com.baidu.mapapi.MKOfflineMapListener;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.MyLocationOverlay;
import com.baidu.mapapi.RouteOverlay;
import com.google.protobuf.InvalidProtocolBufferException;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;
import com.luyun.easyway95.weibo.WBEntry;
import com.luyun.easyway95.wxapi.WXEntryActivity;
import com.luyun.easyway95.MapUtils.GeoPointHelper;
import com.luyun.easyway95.PromptTrafficMsg.TrafficMsg;
import com.luyun.easyway95.TTSService.MsgReceiver;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 这是程序的主要功能实现，负责调用BaiduMap的各种API
 */
public class LYNavigator extends MapActivity implements MKOfflineMapListener{
	//BMapManager mBMapMan = null; 
	final static String TAG = "LYNavigator";
    private static final int MENU_SEARCH = 1;
	public MapUtils mMapUtils = null;
	
	private ZMQService mzService;
	private TTSService mtService;
    private boolean mIsBound;
    private boolean mIsTTSBound;
    public MapHelper mMapHelper;
    private MKPoiInfoHelper mHomeAddr;
    private MKPoiInfoHelper mOfficeAddr;
    private MKPoiInfoHelper mLastDestination;
    private ProgressDialog popupDlg;
    private boolean mSubscript=false;
    
    private MapView mMapView;
    private TextView mHeading;
    private ImageButton mSearch;
	private MKOfflineMap mOffline = null;
    Easyway95App app;
    private boolean updateViewTimerCreated = false;
    private Timer mTimer;
    private LYDlgDismissTimerTask mDlgTimerTask;
    private LYPromptWatchDogTask mPromptWatchDogTask;
    
	private MsgReceiver mReceiver;
	private ConnectivityChangeReceiver mConnectivityChangeReceiver;
    
    //提示播放声音、弹出对话框时间间隔
    private long mlLastPrompt = 0;
    private PromptTrafficMsg mPromptTrafficMsg = null;
    private TrafficMsg mMsgToBeShown = null;

	MyLocationOverlay mLocationOverlay = null;	//定位图层
	RouteOverlay mRouteOverlay = null; //Driving route overlay
	LocationListener mLocationListener = null;//create时注册此listener，Destroy时需要Remove

    private static boolean mbSynthetizeOngoing = false;
    private boolean isRunning = true;
    
    //设备ID
	private String mDeviceID;
	//系统版本
	private int majorRelease;
	private int minorRelease;
    
    private ServiceConnection mConnectionZMQ = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	mzService = ((ZMQService.LocalBinder)service).getService();
        	mMapHelper.checkIn();
            //mzService.registerHandler(handler);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	mzService = null;
        }
    };
    
    private ServiceConnection mConnectionTTS = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	mtService = ((TTSService.LocalBinder)service).getService();
            //mzService.registerHandler(handler);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	mtService = null;
        }
    };
    
    void bindZMQService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.d(TAG, "in bindZMQService");
    	bindService(new Intent(LYNavigator.this, 
                ZMQService.class), mConnectionZMQ, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void bindTTSService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.d(TAG, "in bindTTSService"); 
    	bindService(new Intent(LYNavigator.this, 
                TTSService.class), mConnectionTTS, Context.BIND_AUTO_CREATE);
        mIsTTSBound = true;
    }
    
    void unbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnectionZMQ);
            mIsBound = false;
        }
        if (mIsTTSBound) {
            unbindService(mConnectionTTS);
            mIsTTSBound = false;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
        
		//获取DeviceID
		TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mDeviceID = tm.getDeviceId();
        //获取版本信息
        try {
        	PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        	String versionName = info.versionName;
        	Pattern p = Pattern.compile("(\\d)(\\.)(\\d)");
    		Matcher m = p.matcher(versionName);
    		if (m.find()) {
    			majorRelease = Integer.parseInt(m.group(1));
    			minorRelease = Integer.parseInt(m.group(3));
    			Log.d(TAG, String.format("major=%d, minor=%d", majorRelease, minorRelease));
    		}
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        //2012.11.07 蔡庆丰增加，对网络连接的监控
        mConnectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(mConnectivityChangeReceiver,
        	      new IntentFilter(
        	            ConnectivityManager.CONNECTIVITY_ACTION));

		//初始化家庭和办公室地址，在resume里也要做一次
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr();
		mOfficeAddr = up.getOfficeAddr();
		mLastDestination = up.getLastDestination();
		mMapHelper = new MapHelper(this);
		mSubscript = up.getSubscript();
		
		setContentView(R.layout.ly_navigator);
         
		app = (Easyway95App)this.getApplication();
		//注册mainActivity
		app.setMainActivity(this);
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        super.initMapActivity(app.mBMapMan);
        mMapUtils = app.getMapUtils();

		mMapView = (MapView)findViewById(R.id.bmapsView);
        //mMapView.setBuiltInZoomControls(true);  //设置启用内置的缩放控件
        //设置在缩放动画过程中也显示overlay,默认为不绘制
        //mMapView.setDrawOverlayWhenZooming(true);
		//启动时关闭流量图层
		toggleTrafficLayer(false);
		app.setTrafficLayerFlag(false);
         
        MapController mMapController = mMapView.getController();  // 得到mMapView的控制权,可以用它控制和驱动平移和缩放
        //支持离线地图, 20121105 因Baidu API离线地图功能有问题。如果放开可以把preference加到preferences.xml中
//
//        mOffline = new MKOfflineMap();
//        mOffline.init(app.mBMapMan, this);
//        ArrayList<MKOLUpdateElement> info = mOffline.getAllUpdateInfo();
//        if (info != null) {
//        	Log.d(TAG, String.format("has %d city info", info.size()));
//        	if (info.get(0).status == MKOLUpdateElement.FINISHED) {
//        		
//        	}
//        }
//        ArrayList<MKOLSearchRecord> records = mOffline.getHotCityList();
//        if (records != null) {
//        	Log.d(TAG, String.format("has %d hot city", records.size()));
//        }
//        records = mOffline.getOfflineCityList();
//        if (records != null) {
//        	Log.d(TAG, String.format("has %d offline city", records.size()));
//        }
//        int num = mOffline.scan();
//        Log.d(TAG, String.format("installed offline map %d", num));
//        int progress = downloadProgress();
//        if (progress < 100)
//        	downloadMap();
        
        
        //GeoPoint point = new GeoPoint((int) (22.551541 * 1E6),
        //        (int) (113.94750 * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
        GeoPoint point = null;
        Calendar rightNow = Calendar.getInstance();
        
        //if (now.getHours() <= 12) {
        if (rightNow.get(Calendar.HOUR_OF_DAY) <= 12) {
        	point = mHomeAddr.getPt();
        } else {
        	point = mOfficeAddr.getPt();
        }
        if (point == null) {
        	point = new GeoPoint((int) (22.551541 * 1E6),
        	        (int) (113.94750 * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
        }
        mMapController.setCenter(point);  //设置地图中心点
        mMapController.setZoom(15);    //设置地图zoom级别
		// 添加定位图层
        mLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMapView.getOverlays().add(mLocationOverlay);
        
        // 注册定位事件
        mLocationListener = new LocationListener(){
			@Override
			public void onLocationChanged(Location location) {
				if(location != null && !(app.isTinyMove(location))){
	        		mMapView.getController().animateTo(GeoPointHelper.buildGeoPoint(location));
					mMapHelper.onLocationChanged(location);
					resetMapView();
				}
			}
        };

        //启动ZMQService、线程
        bindZMQService();
        //启动TTSService，非独立线程
        bindTTSService();

        ImageButton btnReset = (ImageButton)findViewById(R.id.resetbtn);
        btnReset.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		Log.d(TAG, "click resetting button.("+getCurrentLocation().toString()+")");
        		//GeoPoint currPosition = new GeoPoint((int) (getCurrLocation().getLatitude() * 1E6), (int) (getCurrLocation().getLongitude() * 1E6));
        		mMapView.getController().animateTo(getCurrentLocation());
				resetMapView();
        	}
        });
        
        mHeading = (TextView)findViewById(R.id.heading);
        mHeading.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
    			final Intent searchIntent = new Intent(LYNavigator.this, SearchActivity.class);
    			startActivityForResult(searchIntent, Constants.ENDPOINT_REQUEST_CODE);
        	}
        });
        
        mSearch = (ImageButton)findViewById(R.id.search);
        mSearch.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
    			final Intent searchIntent = new Intent(LYNavigator.this, SearchActivity.class);
    			startActivityForResult(searchIntent, Constants.ENDPOINT_REQUEST_CODE);
        	}
        });
        
        ImageButton btnTraffics = (ImageButton)findViewById(R.id.trafficbtn);
        btnTraffics.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		//Log.d(TAG, "click resetting button.("+getCurrLocation().toString()+")");
        		//GeoPoint currPosition = new GeoPoint((int) (getCurrLocation().getLatitude() * 1E6), (int) (getCurrLocation().getLongitude() * 1E6));
        		startActivity(new Intent(LYNavigator.this, ShowTraffics.class));
        	}
        });
        
        mPromptTrafficMsg = new PromptTrafficMsg();
        handleIntent(getIntent());
    }
    
	@Override
	public void onNewIntent(Intent intent) {
		Log.d (TAG, "enter onNewIntent");
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		// Get the intent, verify the action and get the query
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.d(TAG, "query: " + query);
			// manually launch the real search activity
			final Intent searchIntent = new Intent(LYNavigator.this, SearchActivity.class);
			// add query to the Intent Extras
			searchIntent.putExtra(SearchManager.QUERY, query);
			startActivityForResult(searchIntent, Constants.ENDPOINT_REQUEST_CODE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.ENDPOINT_REQUEST_CODE) { 
				Bundle bundle = intent.getExtras();
				MKPoiInfoHelper poiInfo = (MKPoiInfoHelper)bundle.getSerializable(Constants.POI_RETURN_KEY);
				Log.d(TAG, "poi: " + poiInfo.toString());
				mMapHelper.requestDrivingRoutes(mMapHelper.getCurrentPoint(), poiInfo);
				mHeading.setText("至" + poiInfo.getName() + "的路况");
				mMapView.getController().animateTo(poiInfo.getPt());
//				mHeading.setTextSize(16);
			} else {
				Log.d(TAG, "unknown requestCode: " + requestCode);
			}
		} else {
			Log.d(TAG, "unknown requestCode: " + requestCode + " or resultCode: " + resultCode);
		}
	}
    
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    
    public GeoPoint getCurrentLocation() {
    	return mMapHelper.getCurrentPoint();
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	@Override
	protected void onDestroy() {
	    super.onDestroy();
		//Easyway95App app = (Easyway95App)this.getApplication();
	    //if (app.mBMapMan != null) {
	    //    app.mBMapMan.destroy();
	    //    app.mBMapMan = null;
	    //}
	    unbindService();
	    unregisterReceiver(mConnectivityChangeReceiver);
	}
	@Override
	protected void onPause() {
		isRunning = false;
		Easyway95App app = (Easyway95App)this.getApplication();
		app.mBMapMan.getLocationManager().removeUpdates(mLocationListener);
		mLocationOverlay.disableMyLocation();
        mLocationOverlay.disableCompass(); // 关闭指南针
	    app.mBMapMan.stop();
	    super.onPause();
	}
	@Override
	protected void onResume() {
		isRunning = true;
		//初始化家庭和办公室地址，在resume里也要做一次
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr();
		mOfficeAddr = up.getOfficeAddr();
		
		Easyway95App app = (Easyway95App)this.getApplication();
		// 注册Listener
        app.mBMapMan.getLocationManager().requestLocationUpdates(mLocationListener);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableCompass(); // 打开指南针
	    app.mBMapMan.start();
	    
	    //注册一个定时器
	    if (mTimer == null) {
	    	mTimer = new Timer();
	    }
	    resetMapView();
	    //setResetTimerTask();
	    
	    super.onResume();
	}    
    
	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			//refactored by chenfeng 2012-11-23
			switch (msg.what) {
			case Constants.DLG_TIME_OUT:
				if (popupDlg != null) {
					popupDlg.dismiss();
				}
				break;
			case Constants.PROMPT_WATCH_DOG:
				promptTraffic();
				break;
			case Constants.TRAFFIC_UPDATE_CMD:
	            try {
	            	LYMsgOnAir msgOnAir  = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.parseFrom(msg.getData().getByteArray(Constants.TRAFFIC_UPDATE));
	            	Log.d(TAG, "got traffic update" + msgOnAir.toString());
	             	mMapHelper.onMsg(msgOnAir);
	            	LYNavigator.this.resetMapView();
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				break;
			default:
				Log.d(TAG, "unknow message, what: " + msg.what);
				break;
			}
		}
    };
    
    public void text2Speech(String msg) {
    	boolean voiceOn = PreferenceManager.getDefaultSharedPreferences(app).getBoolean("voice_preference", true);
    	if (!voiceOn) {
    		return;
    	}
        Log.d(TAG, "in text2Speech");
        Bundle bundle = new Bundle();  
        bundle.putString("ttsmsg", msg);  
        Intent i = new Intent("ttsmsg");  
        i.putExtras(bundle);  
		sendBroadcast(i);  
    }
    
    public void sendMsgToSvr(byte[] data) {
        mzService.sendMsgToSvr(data);	
    }
    
    public GeoPoint getOfficeAddr() {
    	return mOfficeAddr.getPt();
    }
    
    public GeoPoint getHomeAddr() {
    	return mHomeAddr.getPt();
    }
    
    public boolean getSubscript(){
    	return mSubscript;
    }
    public void resetMapView() {
    	resetMapViewByRoute();
    	//刷新周边路况提示
//		TrafficMsg trafficMsg = mPromptTrafficMsg.new TrafficMsg("周边", "无路况", "");
//		mPromptTrafficMsg.pushMsg(trafficMsg);
    	
    	//刷新前方路况提示
    	updateNextTrafficPoint();
    	//更新TrafficView
    	updateTrafficView();
		
		promptTraffic();
    }
    
    private void resetMapViewByRoute() {
		mMapView.getOverlays().clear();
		mMapView.getOverlays().add(mLocationOverlay);
		
    	MKRouteHelper drivingRoute = mMapHelper.getDrivingRoutes();
    	if (drivingRoute != null) {
        	MKRoute route = drivingRoute.getRawRoute();
        	mRouteOverlay = new RouteOverlay(this, mMapView);
    	    mRouteOverlay.setData(route);
    		mMapView.getOverlays().add(mRouteOverlay);
    	}
		mMapView.invalidate();  //刷新地图
    }
    
    private void updateTrafficView() {
    	MKRouteHelper drivingRoutes = mMapHelper.getDrivingRoutes();
        Log.d(TAG, "fetching data from driving routes!");
        if (drivingRoutes == null) {
        	Log.d(TAG, "no data before request driving routes!");
        	return;
        }
        Log.d(TAG, "Driving routes not null. Fetching data from driving routes!");
        Map<String, DrivingRoadWithTraffic> roadTraffics = drivingRoutes.getRoadsWithTraffic();
        Iterator it = roadTraffics.entrySet().iterator();
        
        while (it.hasNext()) {
        	Map.Entry<String, DrivingRoadWithTraffic> entry = (Entry<String, DrivingRoadWithTraffic>) it.next();
        	DrivingRoadWithTraffic rt = (DrivingRoadWithTraffic) entry.getValue();
        	ArrayList<GeoPoint> matchedPoints = rt.getMatchedPointsByList();
        	if (matchedPoints == null || matchedPoints.size() == 0) continue;
    		
        	Drawable marker = getResources().getDrawable(R.drawable.slow_speed);  //得到需要标在地图上的资源
    		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
    				.getIntrinsicHeight());   //为maker定义位置和边界
    		LineOverlay lines = new LineOverlay(marker, LYNavigator.this, matchedPoints);
    		mMapView.getOverlays().add(lines); //添加ItemizedOverlay实例到mMapView
        }
		mMapView.invalidate();  //刷新地图
		//popupTrafficDialg("从东晓路到德贝银饰批发中心，西向"); //弹出模态窗口
    }
    
    /*
     * 本函数应该返回途径最新的三条路况
     */
    private void updateNextTrafficPoint() {
    	Log.d(TAG, "in updateNextTrafficPoint");
    	ArrayList<TrafficPoint> trafficPointsAhead = mMapHelper.getAllTrafficPointsAhead();
    	if (trafficPointsAhead == null || trafficPointsAhead.size() == 0)
    		return;
    	Log.d(TAG, "in updateNextTrafficPoint"+trafficPointsAhead.toString());
    	for (int i=trafficPointsAhead.size()-1; i>-1; i--) {
    		genTrafficMsg(trafficPointsAhead.get(i));
    	}
    }
      
    public void promptTraffic() {
    	//首先保证每两次提示之间隔至少有30秒
		long timenow = System.currentTimeMillis();
    	if (timenow-mlLastPrompt<30*1000) {
    		return;
    	}    	
    	//设置定时器，保证1分钟后会执行一次
    	mPromptWatchDogTask = new LYPromptWatchDogTask();
		mTimer.schedule(mPromptWatchDogTask, Constants.PROMPT_WATCH_DOG_INTERVAL);
    	
		mlLastPrompt = timenow;
		TrafficMsg trafficMsg = mPromptTrafficMsg.popMsg();
		if (trafficMsg == null) {
			return;
		}
		
		popupTrafficDialog(trafficMsg);
		text2Speech(trafficMsg.toString());
    }
    
    public TrafficMsg genTrafficMsg(TrafficPoint tp) {
        Log.d(TAG, "in genTrafficMsg");
        //不播放“前方无拥堵”，20121105，蔡庆丰修改
        if (tp == null) return null;
        
		TrafficMsg trafficMsg = mPromptTrafficMsg.new TrafficMsg(Constants.ROAD_AHEAD, Constants.NO_TRAFFIC, null);
    	
    	if (tp != null && tp.getRoad() != null) {
    		Log.d(TAG, "next traffic point="+tp.toString());
        	String trafficJam = Constants.TRAFFIC_JAM_LVL_MIDDLE;
        	if (tp.getSpeed()<Constants.TRAFFIC_JAM_LVL_HIGH_SPD) {
        		trafficJam = Constants.TRAFFIC_JAM_LVL_HIGH;
        	} else if (tp.getSpeed()>=Constants.TRAFFIC_JAM_LVL_MIDDLE_SPD) {
        		trafficJam = Constants.TRAFFIC_JAM_LVL_LOW;
        	}
        	//mTrafficPoint.setRoad(road);
    		double linearDistance = mMapHelper.getLinearDistanceFromHere(tp.getPoint());
    		String distMsg = mMapHelper.formatDistanceMsg(linearDistance);
    		String msg = tp.getDesc()+"\n"+distMsg;
        	trafficMsg.setRoad(tp.getRoad());
        	trafficMsg.setLevel(trafficJam);
        	trafficMsg.setTraffic(msg);
    	}

		mPromptTrafficMsg.pushMsg(trafficMsg);
		return trafficMsg;
    }
    
    public void popupTrafficDialog(TrafficMsg tmsg) {
        Log.d(TAG, "in popupTrafficDialog");
        if (!isRunning) return;
        mMsgToBeShown = tmsg;
        
    	showDialog(Constants.TRAFFIC_POPUP);
		//创建一个12秒的timer
    	mDlgTimerTask = new LYDlgDismissTimerTask();
		mTimer.schedule(mDlgTimerTask, Constants.DLG_LAST_DURATION);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Constants.TRAFFIC_POPUP: {
                popupDlg = new ProgressDialog(this);
                Log.d(TAG, "onCreateDialog");
                String title = "前方无拥堵";
                String msg = "";
                popupDlg.setTitle(title);
                popupDlg.setMessage(msg);
                popupDlg.setIndeterminate(true);
                popupDlg.setCancelable(true);
                return popupDlg;
            }
        }
        return null;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dlg) {
    	super.onPrepareDialog(id, dlg);
        switch (id) {
            case Constants.TRAFFIC_POPUP: {
                Log.d(TAG, "onPrepareDialog");
                String title = "前方无拥堵";
                String msg = "";
               if (mMsgToBeShown != null) {
                	title = mMsgToBeShown.getPaintedRoad();
                    msg = mMsgToBeShown.getTraffic();
                }
                popupDlg.setTitle(title);
                popupDlg.setMessage(msg);
                popupDlg.setIndeterminate(true);
                popupDlg.setCancelable(true);
                break;
            }
        }
    }
    
    private class LYDlgDismissTimerTask extends TimerTask {
    	@Override
    	public void run() {
    		handler.sendMessage(Message.obtain(handler, Constants.DLG_TIME_OUT));
    	}
    }
    
    private class LYPromptWatchDogTask extends TimerTask {
    	@Override
    	public void run() {
    		handler.sendMessage(Message.obtain(handler, Constants.PROMPT_WATCH_DOG));
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ly_navigator, menu);
        return true; 
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	ProgressDialog promptDlg;
        switch (item.getItemId()) {
            case R.id.search:
//        		startActivity(new Intent(LYNavigator.this, SearchActivity.class));
//    	        onSearchRequested();
    			final Intent searchIntent = new Intent(LYNavigator.this, SearchActivity.class);
    			startActivityForResult(searchIntent, Constants.ENDPOINT_REQUEST_CODE);
                return true;

            case R.id.go_home:
//            	promptDlg = new ProgressDialog(this);
//                promptDlg.setTitle("路况获取中...");
//                promptDlg.setMessage("");
//                promptDlg.setIndeterminate(true);
//                promptDlg.setCancelable(true);
        		mMapHelper.requestDrivingRoutes(mMapHelper.getCurrentPoint(), mHomeAddr);
    			mHeading.setText("至" + mHomeAddr.getName() + "的路况");
    			mHeading.setTextSize(16);
//        		promptDlg.dismiss();
                return true;

            // For "Groups": Toggle visibility of grouped menu items with
            //               nongrouped menu items
            case R.id.go_office:
//            	promptDlg = new ProgressDialog(this);
//                promptDlg.setTitle("路况获取中...");
//                promptDlg.setMessage("");
//                promptDlg.setIndeterminate(true);
//                promptDlg.setCancelable(true);
        		mMapHelper.requestDrivingRoutes(mMapHelper.getCurrentPoint(), mOfficeAddr);
    			mHeading.setText("至" + mOfficeAddr.getName() + "的路况");
    			mHeading.setTextSize(16);
//        		promptDlg.dismiss();
                return true;
                
            case R.id.profile_setting:
                // The reply item is part of the email group
        		startActivity(new Intent(LYNavigator.this, LYSetting.class));
        		return true;
        		
            case R.id.weibo:
            	shareToWeibo();
        		return true;
        		
            case R.id.weixin:
            	shareToWeixin();
        		return true;
        		
            case R.id.quit:
            	finish();	
                
            // Generic catch all for all the other menu resources
            default:
                // something error happened
                break;
        }
        
        return super.onOptionsItemSelected(item);
//        return false;
    }
    
    public void shareToWeibo() {
		WBEntry wbEntry = new WBEntry(LYNavigator.this);
	    wbEntry.authorize();
    }
    
    public void shareToWeixin() {
		IWXAPI wxApi = WXAPIFactory.createWXAPI(this, Constants.WEIXIN_APP_ID, true);
		wxApi.registerApp(Constants.WEIXIN_APP_ID);
        WXTextObject textObj = new WXTextObject();
        textObj.text = Constants.SHARE_MESSAGE;
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = Constants.SHARE_MESSAGE;
        
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        if (wxApi.getWXAppSupportAPI() >= 0x21020001) {
        	req.scene = SendMessageToWX.Req.WXSceneTimeline;
            Log.d(TAG, "support friend circle");
        }
        else {
        	req.scene = SendMessageToWX.Req.WXSceneSession;
        }
        
        wxApi.sendReq(req);
    }

	@Override
	public void onGetOfflineMapState(int type, int state) {
		// TODO Auto-generated method stub
		switch (type) {
		case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
			{
				Log.d(TAG, String.format("cityid:%d update", state));
				if (state != Constants.SHENZHEN_CITY_ID) {
					Log.d(TAG, String.format("cityid:%d update", state));
					removeMap(state);
				}
				//MKOLUpdateElement update = mOffline.getUpdateInfo(state);
				//mText.setText(String.format("%s : %d%%", update.cityName, update.ratio));
			}
			break;
		case MKOfflineMap.TYPE_NEW_OFFLINE:
			Log.d(TAG, String.format("add offlinemap num:%d", state));
			break;
		case MKOfflineMap.TYPE_VER_UPDATE:
			Log.d(TAG, String.format("new offlinemap ver"));
			break;
		}
		 
	}
	
	public int downloadProgress() {
		MKOLUpdateElement element = mOffline.getUpdateInfo(Constants.SHENZHEN_CITY_ID);
		if (element != null) {
			return element.ratio;
		}
		return -1;
	}
	
	public double getMapSize() {
		MKOLUpdateElement element = mOffline.getUpdateInfo(Constants.SHENZHEN_CITY_ID);
		if (element != null) {
			return (double)(element.size/1E6);
		}
		return 0.0;
	}
	
	public void downloadMap() {
		//Log.d(TAG, "in downloadMap()");
		boolean result = mOffline.start(Constants.SHENZHEN_CITY_ID);
		if (result) {
			Log.d(TAG, String.format("started downloading map %d", Constants.SHENZHEN_CITY_ID));
		} else {
			Log.d(TAG, "failed to download map");
		}
	}
	
	public void pauseMap() {
		//Log.d(TAG, "in pauseMap()");
		boolean result = mOffline.pause(Constants.SHENZHEN_CITY_ID);
		if (result) {
			Log.d(TAG, String.format("pauseMap %d", Constants.SHENZHEN_CITY_ID));
		} else {
			Log.d(TAG, "failed to pauseMap");
		}
	}
	
	public void removeMap(int state) {
		//Log.d(TAG, "in removeMap()");
		boolean result = mOffline.remove(state);
		if (result) {
			Log.d(TAG, String.format("removeMap %d", state));
		} else {
			Log.d(TAG, "failed to removeMap");
		}
	}
	
	public void removeMap() {
		//Log.d(TAG, "in removeMap()");
		removeMap(Constants.SHENZHEN_CITY_ID);
	}
	
	public String getDeviceID() {
		Log.d(TAG, "device id: " + mDeviceID);
		return mDeviceID;
	}
	
	public void toggleTrafficLayer(boolean to_set) {
		mMapView.setTraffic(to_set);
	}
    
	public void setLastDestination(MKPoiInfoHelper mpi) {
		//初始化家庭和办公室地址，在resume里也要做一次
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		up.setAndCommitLastDestination(sp, mpi);
	}
	
	public GeoPoint getLastDestination() {
    	return mLastDestination.getPt();
	}
	
	public int getMajorRelease() {
		return majorRelease;
	}
	
	public int getMinorRelease() {
		return minorRelease;
	}
	
	/*
	 * 如果force，则强制升级，否则提示用户有新版本，但当前版本还可继续运行，点升级可去升级，否则关闭对话框继续运行
	 * 1、弹出对话框
	 * 2、点击下载，自动升级
	 * 
	 */
	public void onSoftwareUpgrade(int major, int minor, String url, String desc, boolean force) {
		final String upgradeUrl = url;
		String msg = String.format("您的当前版本: %d.%d, 最新版本: %d.%d\n%s\n", majorRelease, minorRelease, major, minor, desc);

		AlertDialog.Builder alert = new AlertDialog.Builder(LYNavigator.this)
		.setTitle("升级提示")
		.setMessage(msg)
		.setPositiveButton("立即升级", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				/* User clicked OK so do some stuff */
//				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.luyun.easyway95"));
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl));
				Log.d(TAG, "download url: " + upgradeUrl);
				startActivity(intent);
//				System.exit(0);
			}
		});
		
		if (!force) {
			alert.setNegativeButton("以后再说", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked Cancel */
				}
			});
		}

		alert.show();
	}
	
	public void ZMQreconnect() {
		if (mzService != null) {
			mzService.reconnect();
		}
	}
	
	/**
	 * @return boolean return true if the application can access the internet
	 */
	public boolean isConnectedToInternet() {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        //mobile 3G Data Network
        State mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        //wifi
        State wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        
        //如果3G网络和wifi网络都未连接，且不是处于正在连接状态 则进入Network Setting界面 由用户配置网络连接
        if(mobile==State.CONNECTED||mobile==State.CONNECTING)
            return true;
        if(wifi==State.CONNECTED||wifi==State.CONNECTING)
            return true;
        return false;
	}
	
	/*
	 * 监控网络连接发生变化
	 */
	public class ConnectivityChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d(TAG, "ConnectivityChangeReceiver");
			if (isConnectedToInternet()) {
				if (mzService == null) { //ZMQService not ready yet
					return;
				}
				Log.d(TAG, "reconnect");
				ZMQreconnect();
				mMapHelper.checkIn();
			}
		}
	}
	
	public void onSubscription(Boolean sub){
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		up.setAndCommitSubscript(sp, sub);
		mSubscript = sub;

		mMapHelper.subRoute(mOfficeAddr.getPt(), mHomeAddr.getPt(), false, mSubscript);
		
		//delay job
		TimerTask task = new TimerTask(){  
		    public void run(){  
				mMapHelper.subRoute(mHomeAddr.getPt(), mOfficeAddr.getPt(), true, mSubscript);
		    }  
		};
		
		Timer timer = new Timer();
		java.util.Date delay = new Date();
		if(delay.getSeconds() < 55){
			delay.setSeconds(delay.getSeconds() + 5);
		}
		else{
			delay.setMinutes(delay.getMinutes() + 1);
			delay.setSeconds(0);
		}
		
		timer.schedule(task, delay); 
	}
}

