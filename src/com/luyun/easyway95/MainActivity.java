package com.luyun.easyway95;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.LocationListener;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.MyLocationOverlay;
import com.google.protobuf.InvalidProtocolBufferException;
import com.luyun.easyway95.shared.TSSProtos;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class MainActivity extends MapActivity {
	//BMapManager mBMapMan = null; 
	final static String TAG = "MainActivity";
	private ZMQService mzService;
	private TTSService mtService;
    private boolean mIsBound;
    private MapHelper mMapHelper;
    MapView mMapView;

	MyLocationOverlay mLocationOverlay = null;	//定位图层
	LocationListener mLocationListener = null;//create时注册此listener，Destroy时需要Remove
	//TrafficSubscriber mTrafficSubscriber;

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
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.d(TAG, "in doBindService");
    	bindService(new Intent(MainActivity.this, 
                ZMQService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void doUnbindService() {
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
		
		((Easyway95App)getApplication()).setMainActivity(this);
		mMapHelper = new MapHelper(this);

		setContentView(R.layout.activity_main);
        //mBMapMan = new BMapManager(getApplication());
        //mBMapMan.init("513CBE299AB953DDFAEBC4A608F1F6557C30D685", null);
        //super.initMapActivity(mBMapMan);
         
		Easyway95App app = (Easyway95App)this.getApplication();
		if (app.mBMapMan == null) {
			app.mBMapMan = new BMapManager(getApplication());
			app.mBMapMan.init(app.mStrKey, new Easyway95App.MyGeneralListener());
		}
		app.mBMapMan.start();
        super.initMapActivity(app.mBMapMan);
        
		mMapView = (MapView) findViewById(R.id.bmapsView);
        //mMapView.setBuiltInZoomControls(true);  //设置启用内置的缩放控件
        //设置在缩放动画过程中也显示overlay,默认为不绘制
        mMapView.setDrawOverlayWhenZooming(true);
         
        MapController mMapController = mMapView.getController();  // 得到mMapView的控制权,可以用它控制和驱动平移和缩放
        GeoPoint point = new GeoPoint((int) (22.551541 * 1E6),
                (int) (113.94750 * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
        mMapController.setCenter(point);  //设置地图中心点
        mMapController.setZoom(13);    //设置地图zoom级别
		// 添加定位图层
        mLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMapView.getOverlays().add(mLocationOverlay);
        
        // 注册定位事件
        mLocationListener = new LocationListener(){

			@Override
			public void onLocationChanged(Location location) {
				if(location != null){
					String strLog = String.format("您当前的位置:\r\n" +
							"纬度:%f\r\n" +
							"经度:%f",
							location.getLongitude(), location.getLatitude());
					Log.d(TAG, strLog);
					mMapHelper.onLocationChanged(location);
				}
			}
        };

        //startService(new Intent(MainActivity.this,
        //		ZMQService.class));
        doBindService();
        
        Button btn = (Button)findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		com.luyun.easyway95.shared.TSSProtos.Coordinate cor1 = com.luyun.easyway95.shared.TSSProtos.Coordinate.newBuilder()
        															.setLat(31.325152)
        															.setLng(120.558957)
        															.build();
        		com.luyun.easyway95.shared.TSSProtos.Coordinate cor2 = com.luyun.easyway95.shared.TSSProtos.Coordinate.newBuilder()
																	.setLat(31.325000)
																	.setLng(120.559000)
																	.build();
        		com.luyun.easyway95.shared.TSSProtos.Segment seg1 = com.luyun.easyway95.shared.TSSProtos.Segment.newBuilder()
        															.setRoad("宝石西路")
        															.setStart(cor1)
        															.setEnd(cor2)
        															.build();
        		com.luyun.easyway95.shared.TSSProtos.Segment seg2 = com.luyun.easyway95.shared.TSSProtos.Segment.newBuilder()
																	.setRoad("迎宾南路")
        															.setStart(cor1)
        															.setEnd(cor2)
																	.build();
        		com.luyun.easyway95.shared.TSSProtos.Route trt = com.luyun.easyway95.shared.TSSProtos.Route.newBuilder()
        															.setIdentity(1)
        															.addSegments(seg1)
        															.addSegments(seg2)
        															.build();
        		
        		com.luyun.easyway95.shared.TSSProtos.TrafficSub tsub = com.luyun.easyway95.shared.TSSProtos.TrafficSub.newBuilder()
        															.setCity("深圳")
        															.setOprType(com.luyun.easyway95.shared.TSSProtos.OprType.SUB_CREATE)
        															.setPubType(com.luyun.easyway95.shared.TSSProtos.PubType.PUB_ONCE)
        															.setRoute(trt)
        															.build();
        		com.luyun.easyway95.shared.TSSProtos.Package pkg = com.luyun.easyway95.shared.TSSProtos.Package.newBuilder()
        															.setVersion(1)
        															.setMsgDir(com.luyun.easyway95.shared.TSSProtos.MsgDir.CLIENT2TSS)
        															.setMsgType(com.luyun.easyway95.shared.TSSProtos.MsgType.TRAFFIC_SUB)
        															.setMsgId(1000)
        															.setTimestamp(System.currentTimeMillis()/1000)
        															.setTrafficSub(tsub)
        															.build();
        		Log.i(TAG, pkg.toString());
        		System.out.println(pkg.toString());
        		byte[] data = pkg.toByteArray();
        		mzService.sendMsgToSvr(data);
        	}
        });
        
        Button btnLogin = (Button)findViewById(R.id.button2);
        btnLogin.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		startActivity(new Intent(MainActivity.this, LoginActivity.class));
        	}
        });

        /*Button btnSetting = (Button)findViewById(R.id.button3);
        btnSetting.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		startActivity(new Intent(MainActivity.this, SettingActivity.class));
        	}
        });*/

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
        		TTSThread t = new TTSThread("家长们：大家好!今天下午放学后，需要把把印制的教室板报文字贴到板报上，有时间的家长请今天下午放学后到教室帮忙，谢谢大家支持！"); 
        		t.start();
        	}
        });
        
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
        
        ImageButton btnReset = (ImageButton)findViewById(R.id.imageButton1);
        btnReset.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		Log.d(TAG, "click resetting button.("+getCurrLocation().toString()+")");
        		GeoPoint currPosition = new GeoPoint((int) (getCurrLocation().getLatitude() * 1E6), (int) (getCurrLocation().getLongitude() * 1E6));
        		mMapView.getController().animateTo(currPosition);
        	}
        });
        ImageButton btnSetting = (ImageButton)findViewById(R.id.imageButton3);
        btnSetting.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//reset map view (animate to current location)
        		startActivity(new Intent(MainActivity.this, SettingActivity.class));
        	}
        });
    }

    public Location getCurrLocation() {
    	return mMapHelper.mCurrentLocation;
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
	    doUnbindService();
	}
	@Override
	protected void onPause() {
		mLocationOverlay.disableMyLocation();
        mLocationOverlay.disableCompass(); // 关闭指南针
		Easyway95App app = (Easyway95App)this.getApplication();
		app.mBMapMan.getLocationManager().removeUpdates(mLocationListener);
	    if (app.mBMapMan != null) {
	        app.mBMapMan.stop();
	    }
	    super.onPause();
	}
	@Override
	protected void onResume() {
		Easyway95App app = (Easyway95App)this.getApplication();
		// 注册Listener
        app.mBMapMan.getLocationManager().requestLocationUpdates(mLocationListener);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableCompass(); // 打开指南针
	    if (app.mBMapMan != null) {
	        app.mBMapMan.start();
	    }
	    super.onResume();
	}    
    
	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == Constants.SYNTHESIZE_DONE) {
				//mMapController.animateTo(mLocationOverlay.getMyLocation());
				mbSynthetizeOngoing = false;
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
            	com.luyun.easyway95.shared.TSSProtos.Package pkg  = com.luyun.easyway95.shared.TSSProtos.Package.parseFrom(msg.getData().getByteArray(Constants.TRAFFIC_UPDATE));
            	Log.i(TAG, pkg.toString());
            	mMapHelper.onMsg(pkg);
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
	        int waitingTimes = 0; //waiting upto 15 seconds
	        while (mbSynthetizeOngoing && waitingTimes < 30) {
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
}
