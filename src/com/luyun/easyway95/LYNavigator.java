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
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;
import com.luyun.easyway95.weibo.WBEntry;
import com.luyun.easyway95.wxapi.WXEntryActivity;
import com.luyun.easyway95.MapUtils.GeoPointHelper;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import android.widget.SearchView;
import android.widget.Toast;

/*
 * ���ǳ������Ҫ����ʵ�֣��������BaiduMap�ĸ���API
 */
public class LYNavigator extends MapActivity {
	//BMapManager mBMapMan = null; 
	final static String TAG = "LYNavigator";
	public MapUtils mMapUtils = null;
	
	private ZMQService mzService;
	private TTSService mtService;
    private boolean mIsBound;
    private boolean mIsTTSBound;
    public MapHelper mMapHelper;
    private GeoPoint mHomeAddr;
    private GeoPoint mOfficeAddr;
    private TrafficPoint mTrafficPoint;
    private ProgressDialog popupDlg;
    private SearchView mEndpoint;
    
    MapView mMapView;
    Easyway95App app;
    private boolean updateViewTimerCreated = false;
    private Timer mTimer;
    private LYDlgDismissTimerTask mDlgTimerTask;
//    private LYResetTimerTask mResetTimerTask;
    
    //��ʾ���������������Ի���ʱ����
    private long mlLastPrompt = 0;

	MyLocationOverlay mLocationOverlay = null;	//��λͼ��
	RouteOverlay mRouteOverlay = null; //Driving route overlay
	LocationListener mLocationListener = null;//createʱע���listener��Destroyʱ��ҪRemove

    private static boolean mbSynthetizeOngoing = false;
    
    private ServiceConnection mConnectionZMQ = new ServiceConnection() {
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

//		getWindow().setFlags(
//			    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//			    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

		//��ʼ����ͥ�Ͱ칫�ҵ�ַ����resume��ҲҪ��һ��
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr().getPt();
		mOfficeAddr = up.getOfficeAddr().getPt();
		mMapHelper = new MapHelper(this);

		setContentView(R.layout.ly_navigator);
         
		app = (Easyway95App)this.getApplication();
		//ע��mainActivity
		app.setMainActivity(this);
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        super.initMapActivity(app.mBMapMan);
        mMapUtils = app.getMapUtils();
        
		mMapView = (MapView) findViewById(R.id.bmapsView);
        //mMapView.setBuiltInZoomControls(true);  //�����������õ����ſؼ�
        //���������Ŷ���������Ҳ��ʾoverlay,Ĭ��Ϊ������
        //mMapView.setDrawOverlayWhenZooming(true);
         
        MapController mMapController = mMapView.getController();  // �õ�mMapView�Ŀ���Ȩ,�����������ƺ�����ƽ�ƺ�����
        //GeoPoint point = new GeoPoint((int) (22.551541 * 1E6),
        //        (int) (113.94750 * 1E6));  //�ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢�� (�� * 1E6)
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
        	        (int) (113.94750 * 1E6));  //�ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢�� (�� * 1E6)
        }
        mMapController.setCenter(point);  //���õ�ͼ���ĵ�
        mMapController.setZoom(15);    //���õ�ͼzoom����
		// ��Ӷ�λͼ��
        mLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMapView.getOverlays().add(mLocationOverlay);
        
        // ע�ᶨλ�¼�
        mLocationListener = new LocationListener(){
			@Override
			public void onLocationChanged(Location location) {
				if(location != null && !(app.isTinyMove(location))){
	        		mMapView.getController().animateTo(GeoPointHelper.buildGeoPoint(location));
//					String strLog = String.format("����ǰ��λ��:\r\n" +
//							"γ��:%f\r\n" +
//							"����:%f",
//							location.getLongitude(), location.getLatitude());
//					Log.d(TAG, strLog);
					mMapHelper.onLocationChanged(location);
					resetMapView();
				}
			}
        };

        //����ZMQService���߳�
        bindZMQService();
        //����TTSService���Ƕ����߳�
        bindTTSService();
        
        /* 
         * �����ǲ��Դ��룬����ſ�����Ҫ��layout.activity_main������Ӧ����Դ��
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
        		TTSThread t = new TTSThread("����һ��У�����������������ձ���"); 
        		t.start();
        	}
        });

        Button btnXunfei2 = (Button)findViewById(R.id.button5);
        btnXunfei2.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start TTS service
        		TTSThread t = new TTSThread("�ҳ��ǣ���Һ�!���������ѧ����Ҫ��ӡ�ƵĽ��Ұ屨���������屨�ϣ���ʱ��ļҳ�����������ѧ�󵽽��Ұ�æ��лл���֧�֣�"); 
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
        		Drawable marker = getResources().getDrawable(R.drawable.icon95);  //�õ���Ҫ���ڵ�ͼ�ϵ���Դ
        		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
        				.getIntrinsicHeight());   //Ϊmaker����λ�úͱ߽�
        		ArrayList<GeoPoint> listPoints = new ArrayList();
        		listPoints.add(startPoint);
        		listPoints.add(endPoint);
        		LineOverlay lines = new LineOverlay(marker, MainActivity.this, listPoints);
        		mMapView.getOverlays().add(lines); //���ItemizedOverlayʵ����mMapView
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
        		startActivity(new Intent(LYNavigator.this, ShowTraffics.class));
        	}
        });
        
//        mEndpoint = new SearchView(this);
    }
    
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    
//	public boolean onTouchEvent(MotionEvent event) {
//    	mEndpoint.setVisibility(View.VISIBLE);
//    	return super.onTouchEvent(event);
//    }

    public GeoPoint getCurrentLocation() {
    	return mMapHelper.mCurrentPoint;
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
        mLocationOverlay.disableCompass(); // �ر�ָ����
	    app.mBMapMan.stop();
	    super.onPause();
	}
	@Override
	protected void onResume() {
		//��ʼ����ͥ�Ͱ칫�ҵ�ַ����resume��ҲҪ��һ��
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		//Log.d(TAG, up.toString());
		mHomeAddr = up.getHomeAddr().getPt();
		mOfficeAddr = up.getOfficeAddr().getPt();
		
		Easyway95App app = (Easyway95App)this.getApplication();
		// ע��Listener
        app.mBMapMan.getLocationManager().requestLocationUpdates(mLocationListener);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableCompass(); // ��ָ����
	    app.mBMapMan.start();
	    
	    //ע��һ����ʱ��
	    if (mTimer == null) {
	    	mTimer = new Timer();
	    }
	    resetMapView();
	    //setResetTimerTask();
	    super.onResume();
	}    
    
//	private void setResetTimerTask() {
//	    if (mResetTimerTask != null) {
//	    	mResetTimerTask.cancel();
//	    } 
//    	mResetTimerTask = new LYResetTimerTask();
//	    mTimer.schedule(mResetTimerTask, Constants.RESET_MAP_INTERVAL);
//	}
	
	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
//			if (msg.what == Constants.SYNTHESIZE_DONE) {
//				//mMapController.animateTo(mLocationOverlay.getMyLocation());
//				mbSynthetizeOngoing = false;
//				return;
//			}
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
            	//Log.i(TAG, "get message from server.");
            	LYMsgOnAir msgOnAir  = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.parseFrom(msg.getData().getByteArray(Constants.TRAFFIC_UPDATE));
            	Log.i(TAG, msgOnAir.toString());
             	mMapHelper.onMsg(msgOnAir);
            	LYNavigator.this.resetMapView();
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
    };
    
    public void text2Speech(String msg) {
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
    	return mOfficeAddr;
    }
    
    public GeoPoint getHomeAddr() {
    	return mHomeAddr;
    }
    
    public void resetMapView() {
    	resetMapViewByRoute();
    	updateNextTrafficPoint();
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
		mMapView.invalidate();  //ˢ�µ�ͼ
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
    		
        	Drawable marker = getResources().getDrawable(R.drawable.slow_speed);  //�õ���Ҫ���ڵ�ͼ�ϵ���Դ
    		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
    				.getIntrinsicHeight());   //Ϊmaker����λ�úͱ߽�
    		LineOverlay lines = new LineOverlay(marker, LYNavigator.this, matchedPoints);
    		mMapView.getOverlays().add(lines); //���ItemizedOverlayʵ����mMapView
        }
		mMapView.invalidate();  //ˢ�µ�ͼ
		//popupTrafficDialg("�Ӷ���·���±������������ģ�����"); //����ģ̬����
    }
    
    private void updateNextTrafficPoint() {
    	Log.d(TAG, "in updateNextTrafficPoint");
    	mTrafficPoint = mMapHelper.getNextTrafficPoint();
    }
      
    public void promptTraffic() {
		long timenow = System.currentTimeMillis();
    	if (timenow-mlLastPrompt<30*1000) {
    		return;
    	}    	
		mlLastPrompt = timenow;
    	popupTrafficDialog(mTrafficPoint);
    }
    
    public void popupTrafficDialog(TrafficPoint tp) {
        Log.d(TAG, "in popupTrafficDialog");
    	String msg = Constants.NO_TRAFFIC_AHEAD;
		mTrafficPoint = new TrafficPoint(tp);
    	if (tp != null && tp.getRoad() != null) {
    		Log.d(TAG, "next traffic point="+tp.toString());
        	String trafficJam = Constants.TRAFFIC_JAM_LVL_MIDDLE;
        	if (mTrafficPoint.getSpeed()<Constants.TRAFFIC_JAM_LVL_HIGH_SPD) {
        		trafficJam = Constants.TRAFFIC_JAM_LVL_HIGH;
        	} else if (mTrafficPoint.getSpeed()>=Constants.TRAFFIC_JAM_LVL_MIDDLE_SPD) {
        		trafficJam = Constants.TRAFFIC_JAM_LVL_LOW;
        	}
        	String newRoad = tp.getRoad()+trafficJam;
        	mTrafficPoint.setRoad(newRoad);
    		double linearDistance = mMapHelper.getLinearDistanceFromHere(tp.getPoint());
    		String distMsg = mMapHelper.formatDistanceMsg(linearDistance);
    		msg = tp.getDesc()+"\n"+distMsg;
    		mTrafficPoint.setDesc(msg);
    		msg = mTrafficPoint.getRoad()+msg;
    	}

    	Log.d(TAG, "msg to be showed:"+msg);
		//TTSThread t = new TTSThread(msg); 
		//t.start();
    	text2Speech(msg);
		
    	showDialog(Constants.TRAFFIC_POPUP);
		//����һ��20���timer
    	if (mDlgTimerTask != null) {
    		mDlgTimerTask = new LYDlgDismissTimerTask();
    	}
    	mDlgTimerTask = new LYDlgDismissTimerTask();
		mTimer.schedule(mDlgTimerTask, Constants.DLG_LAST_DURATION);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Constants.TRAFFIC_POPUP: {
                popupDlg = new ProgressDialog(this);
                if (mTrafficPoint != null) {
                	Log.d(TAG, mTrafficPoint.toString());
                }
                Log.d(TAG, "onCreateDialog");
                String title = "ǰ����ӵ��";
                String msg = "";
                if (mTrafficPoint != null && mTrafficPoint.getRoad() != null) {
                	Log.d(TAG, "no traffic ahead!");
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
    
    @Override
    protected void onPrepareDialog(int id, Dialog dlg) {
    	super.onPrepareDialog(id, dlg);
        switch (id) {
            case Constants.TRAFFIC_POPUP: {
                //popupDlg = new ProgressDialog(this);
                if (mTrafficPoint != null) {
                	Log.d(TAG, mTrafficPoint.toString());
                }
                Log.d(TAG, "onPrepareDialog");
                String title = "ǰ����ӵ��";
                String msg = "";
                if (mTrafficPoint != null && mTrafficPoint.getRoad() != null) {
                	Log.d(TAG, "traffic ahead!");
                	title = mTrafficPoint.getRoad();
                	msg = mTrafficPoint.getDesc();
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
    
//    private class LYResetTimerTask extends TimerTask {
//    	@Override
//    	public void run() {
//    		handler.sendMessage(Message.obtain(handler, Constants.RESET_MAP_TIME_OUT));
//    	}
//    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ly_navigator, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.more_traffic:
//        		startActivity(new Intent(LYNavigator.this, ShowTraffics.class));
//                return true;
//
            case R.id.go_home:
        		mMapHelper.requestDrivingRoutes(mOfficeAddr, mHomeAddr);
                return true;

            // For "Groups": Toggle visibility of grouped menu items with
            //               nongrouped menu items
            case R.id.go_office:
        		mMapHelper.requestDrivingRoutes(mHomeAddr, mOfficeAddr);
                return true;
                
            case R.id.profile_setting:
                // The reply item is part of the email group
        		startActivity(new Intent(LYNavigator.this, SettingActivity.class));
        		return true;
        		
            case R.id.weibo:
            	ShareToWeibo();
        		return true;
        		
            case R.id.weixin:
            	ShareToWeixin();
        		return true;
        		
            case R.id.quit:
            	finish();	
                
            // Generic catch all for all the other menu resources
            default:
                // something error happened
                break;
        }
        
        return false;
    }
    
    public void ShareToWeibo() {
		WBEntry wbEntry = new WBEntry(LYNavigator.this);
	    wbEntry.authorize();
    }
    
    public void ShareToWeixin() {
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
}
