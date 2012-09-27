package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.LocationListener;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.MyLocationOverlay;
import com.baidu.mapapi.RouteOverlay;
import com.google.protobuf.InvalidProtocolBufferException;
import com.luyun.easyway95.MKRouteHelper.GeoPointHelper;
import com.luyun.easyway95.MKRouteHelper.RoadTrafficHelper;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

public class MainActivity extends MapActivity {
	//BMapManager mBMapMan = null; 
	final static String TAG = "MainActivity";
	private ZMQService mzService;
	private TTSService mtService;
    private boolean mIsBound;
    public MapHelper mMapHelper;
    private GeoPoint mHomeAddr;
    private GeoPoint mOfficeAddr;
    private TrafficPoint mTrafficPoint;
    private ProgressDialog popupDlg;
    MapView mMapView;
    Easyway95App app;
    private boolean updateViewTimerCreated = false;

	MyLocationOverlay mLocationOverlay = null;	//定位图层
	RouteOverlay mRouteOverlay = null; //Driving route overlay
	LocationListener mLocationListener = null;//create时注册此listener，Destroy时需要Remove

    private static boolean mbSynthetizeOngoing = false;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	mzService = ((ZMQService.LocalBinder)service).getService();
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
    
    void bindZMQService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.d(TAG, "in bindZMQService");
    	bindService(new Intent(MainActivity.this, 
                ZMQService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void unbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 		
		
		//初始化家庭和办公室地址，在resume里也要做一次
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr().getPt();
		mOfficeAddr = up.getOfficeAddr().getPt();
		mMapHelper = new MapHelper(this);

		setContentView(R.layout.activity_main);
         
		app = (Easyway95App)this.getApplication();
		//注册mainActivity
		app.setMainActivity(this);
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        super.initMapActivity(app.mBMapMan);
        
		mMapView = (MapView) findViewById(R.id.bmapsView);
        //mMapView.setBuiltInZoomControls(true);  //设置启用内置的缩放控件
        //设置在缩放动画过程中也显示overlay,默认为不绘制
        //mMapView.setDrawOverlayWhenZooming(true);
         
        MapController mMapController = mMapView.getController();  // 得到mMapView的控制权,可以用它控制和驱动平移和缩放
        //GeoPoint point = new GeoPoint((int) (22.551541 * 1E6),
        //        (int) (113.94750 * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
        GeoPoint point = null;
        Calendar rightNow = Calendar.getInstance();
        
        //if (now.getHours() <= 12) {
        if (rightNow.get(Calendar.HOUR_OF_DAY) <= 12) {
        	point = mHomeAddr;
        } else {
        	point = mOfficeAddr;
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
					String strLog = String.format("您当前的位置:\r\n" +
							"纬度:%f\r\n" +
							"经度:%f",
							location.getLongitude(), location.getLatitude());
					Log.d(TAG, strLog);
					mMapHelper.onLocationChanged(location);
					resetMapView();
				}
			}
        };

        //启动ZMQService、线程
        bindZMQService();
        
        /* 
         * 以下是测试代码，如果放开，需要在layout.activity_main配置相应的资源。
        btnLogin = (Button)findViewById(R.id.button2);
        btnLogin.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		startActivity(new Intent(MainActivity.this, LoginActivity.class));
        	}
        });

        Button btnSetting = (Button)findViewById(R.id.button3);
        btnSetting.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		startActivity(new Intent(MainActivity.this, SettingActivity.class));
        	}
        });

        Button btnXunfei1 = (Button)findViewById(R.id.button4);
        btnXunfei1.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start TTS service
        		TTSThread t = new TTSThread("江西一高校新生霸气姓名“操日本”"); 
        		t.start();
        	}
        });

        Button btnXunfei2 = (Button)findViewById(R.id.button5);
        btnXunfei2.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start TTS service
        		TTSThread t = new TTSThread("家长们：大家好!今天下午放学后，需要把印制的教室板报文字贴到板报上，有时间的家长请今天下午放学后到教室帮忙，谢谢大家支持！"); 
        		t.start();
        	}
        });
        
        */
        /*
        Button btnDrivingReq = (Button)findViewById(R.id.button6);
        btnDrivingReq.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start request driving routes
        		GeoPoint startPoint = new GeoPoint((int) (22.661993 * 1E6), (int) (114.063844 * 1E6));
        		GeoPoint endPoint = new GeoPoint((int) (22.575831 * 1E6), (int) (113.908052 * 1E6));
        		mMapHelper.requestDrivingRoutes(startPoint, endPoint);
        	}
        });
        
        Button btnAroundReq = (Button)findViewById(R.id.button7);
        btnAroundReq.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start request driving routes
        		GeoPoint startPoint = new GeoPoint((int) (22.661993 * 1E6), (int) (114.063844 * 1E6));
        		//GeoPoint endPoint = new GeoPoint((int) (22.575831 * 1E6), (int) (113.908052 * 1E6));
        		mMapHelper.requestRoadsAround(startPoint);
        	}
        });

        Button btnLineTest = (Button)findViewById(R.id.linetest);
        btnLineTest.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start request driving routes
        		GeoPoint startPoint = new GeoPoint((int) (22.661993 * 1E6), (int) (114.063844 * 1E6));
        		GeoPoint endPoint = new GeoPoint((int) (22.575831 * 1E6), (int) (113.908052 * 1E6));
        		Drawable marker = getResources().getDrawable(R.drawable.icon95);  //得到需要标在地图上的资源
        		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
        				.getIntrinsicHeight());   //为maker定义位置和边界
        		ArrayList<GeoPoint> listPoints = new ArrayList();
        		listPoints.add(startPoint);
        		listPoints.add(endPoint);
        		LineOverlay lines = new LineOverlay(marker, MainActivity.this, listPoints);
        		mMapView.getOverlays().add(lines); //添加ItemizedOverlay实例到mMapView
        	}
        });
        */

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
        ImageButton btnTraffics = (ImageButton)findViewById(R.id.trafficbtn);
        btnTraffics.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		//Log.d(TAG, "click resetting button.("+getCurrLocation().toString()+")");
        		//GeoPoint currPosition = new GeoPoint((int) (getCurrLocation().getLatitude() * 1E6), (int) (getCurrLocation().getLongitude() * 1E6));
        		startActivity(new Intent(MainActivity.this, ShowTraffics.class));
        	}
        });
        ImageButton btnSetting = (ImageButton)findViewById(R.id.setting);
        btnSetting.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		startActivity(new Intent(MainActivity.this, SettingActivity.class));
        	}
        });
        ImageButton btnGohome = (ImageButton)findViewById(R.id.gohome);
        btnGohome.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		mMapHelper.requestDrivingRoutes(mOfficeAddr, mHomeAddr);
        	}
        });
        ImageButton btnGooffice = (ImageButton)findViewById(R.id.gooffice);
        btnGooffice.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		mMapHelper.requestDrivingRoutes(mHomeAddr, mOfficeAddr);
        	}
        });
    }

    public GeoPoint getCurrentLocation() {
    	return mMapHelper.mCurrentPoint;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
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
	}
	@Override
	protected void onPause() {
		Easyway95App app = (Easyway95App)this.getApplication();
		app.mBMapMan.getLocationManager().removeUpdates(mLocationListener);
		mLocationOverlay.disableMyLocation();
        mLocationOverlay.disableCompass(); // 关闭指南针
	    app.mBMapMan.stop();
	    super.onPause();
	}
	@Override
	protected void onResume() {
		//初始化家庭和办公室地址，在resume里也要做一次
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr().getPt();
		mOfficeAddr = up.getOfficeAddr().getPt();
		
		Easyway95App app = (Easyway95App)this.getApplication();
		// 注册Listener
        app.mBMapMan.getLocationManager().requestLocationUpdates(mLocationListener);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableCompass(); // 打开指南针
	    app.mBMapMan.start();
	    super.onResume();
	}    
    
	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == Constants.SYNTHESIZE_DONE) {
				//mMapController.animateTo(mLocationOverlay.getMyLocation());
				mbSynthetizeOngoing = false;
				return;
			}
			if (msg.what == Constants.DLG_TIME_OUT) {
				//mMapController.animateTo(mLocationOverlay.getMyLocation());
				if (popupDlg != null) {
					popupDlg.dismiss();
				}
				return;
			}
			if (msg.what == Constants.RESET_MAP_TIME_OUT) {
				updateViewTimerCreated = false;
				resetMapView();
				return;
			}
			/* //first two from location update
			else if (msg.what == Constants.REOCODER_RESULT) {
				//addMarker(mTrackeeLngX, mTrackeeLatY);
				return;
			}
			else if (msg.what == Constants.WAITING_TRACKEE_LOC) {
				//Toast a message and block shake event detection
                Toast.makeText(getApplicationContext(), "waiting for trackee location update..", Toast.LENGTH_SHORT).show();
                //mSensorManager.unregisterListener(mSensorListener);
			}
			else if (msg.what == Constants.GOT_TRACKEE_LOC) {
				//Toast a message and enable shake event detection
                Toast.makeText(getApplicationContext(), "got trackee location ..", Toast.LENGTH_SHORT).show();
                //mSensorManager.registerListener(mSensorListener,
                //        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                //        SensorManager.SENSOR_DELAY_UI);
			}
			*/
			switch (msg.what) {
			case Constants.TRAFFIC_UPDATE_CMD:
				Log.d(TAG, "got traffic update");
				break;
			default:
					break;
			}
            try {
            	Log.i(TAG, "get message from server.");
            	LYMsgOnAir msgOnAir  = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.parseFrom(msg.getData().getByteArray(Constants.TRAFFIC_UPDATE));
            	Log.i(TAG, msgOnAir.toString());
            	mMapHelper.onMsg(msgOnAir);
            	MainActivity.this.resetMapView();
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
    };
    
    public class TTSThread extends Thread {
		private String msTTS;
    	public TTSThread(String s) {
			msTTS = s;
		}
    	
    	@Override
		public void run() {
	        Log.d(TAG, "In TTSThead::running"); 
	        int waitingTimes = 0; //waiting upto 5 seconds
	        while (mbSynthetizeOngoing && waitingTimes < 10) {
	        	waitingTimes ++;
	        	try {
	        		Thread.sleep(500);
	        	} catch (Exception e) {
	        		
	        	}
	        }
	        mbSynthetizeOngoing = true;
	        Intent i = new Intent(MainActivity.this, TTSService.class);
    		i.putExtra("text", msTTS);
    		startService(i);
		}
    }
    
    public void sendMsgToSvr(byte[] data) {
        mzService.sendMsgToSvr(data);	
    }
    
    public GeoPoint getOfficeAddr() {
    	return mOfficeAddr;
    }
    
    public GeoPoint getHomeAddr() {
    	return mHomeAddr;
    }
    
    public void resetMapView() {
    	resetMapViewByRoute();
    	updateTrafficView();
		
		if (updateViewTimerCreated) return;
		updateViewTimerCreated = true;
    	//创建一个60秒的timer，保证自动刷新，否则界面显示和数据不同步
		Timer timer = new Timer();
		LYResetTimerTask timerTask = new LYResetTimerTask();
		timer.schedule(timerTask, 60000);
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
        Map<String, RoadTrafficHelper> roadTraffics = drivingRoutes.getRoadsWithTraffic();
        Iterator it = roadTraffics.entrySet().iterator();
        
        while (it.hasNext()) {
        	Map.Entry<String, RoadTrafficHelper> entry = (Entry<String, RoadTrafficHelper>) it.next();
        	RoadTrafficHelper rt = (RoadTrafficHelper) entry.getValue();
        	ArrayList<GeoPoint> matchedPoints = rt.getMatchedPointsByList();
        	if (matchedPoints == null || matchedPoints.size() == 0) continue;
    		
        	Drawable marker = getResources().getDrawable(R.drawable.slow_speed);  //得到需要标在地图上的资源
    		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
    				.getIntrinsicHeight());   //为maker定义位置和边界
    		LineOverlay lines = new LineOverlay(marker, MainActivity.this, matchedPoints);
    		mMapView.getOverlays().add(lines); //添加ItemizedOverlay实例到mMapView
        }
		mMapView.invalidate();  //刷新地图
		//popupTrafficDialg("从东晓路到德贝银饰批发中心，西向"); //弹出模态窗口
    }
    
    public void popupTrafficDialg(String msg) {
    	showDialog(Constants.TRAFFIC_POPUP);
        //String msg = "从东晓路到德贝银饰批发中心，西向";
		TTSThread t = new TTSThread(msg); 
		t.start();
    }
    
    public void promptTraffic() {
    	
    }
    
    public void popupTrafficDialog(TrafficPoint tp) {
        Log.d(TAG, "in popupTrafficDialog");
    	String msg = Constants.NO_TRAFFIC_AHEAD;
    	if (tp != null) {
    		mTrafficPoint = tp;
    		msg = tp.getDesc();
    	}

    	showDialog(Constants.TRAFFIC_POPUP);
		TTSThread t = new TTSThread(msg); 
		t.start();
		
		//创建一个8秒的timer
		Timer timer = new Timer();
		LYTimerTask timerTask = new LYTimerTask();
		timer.schedule(timerTask, 8000);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Constants.TRAFFIC_POPUP: {
                popupDlg = new ProgressDialog(this);
                String title = "前方无拥堵";
                String msg = "";
                if (mTrafficPoint != null) {
                	title = mTrafficPoint.getRoad();
                	msg = mTrafficPoint.getDesc();
                }
                popupDlg.setTitle(title);
                popupDlg.setMessage(msg);
                popupDlg.setIndeterminate(true);
                popupDlg.setCancelable(true);
                return popupDlg;
            }
        }
        return null;
    }
    
    private class LYTimerTask extends TimerTask {
    	@Override
    	public void run() {
    		handler.sendMessage(Message.obtain(handler, Constants.DLG_TIME_OUT));
    	}
    }
    
    private class LYResetTimerTask extends TimerTask {
    	@Override
    	public void run() {
    		handler.sendMessage(Message.obtain(handler, Constants.RESET_MAP_TIME_OUT));
    	}
    }
}
