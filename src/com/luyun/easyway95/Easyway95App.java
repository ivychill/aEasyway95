package com.luyun.easyway95;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKEvent;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.MKPoiResult;

import android.app.Application;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class Easyway95App extends Application {
	private LYNavigator mainActivity;
	
	static Easyway95App mApp;
	public MKPoiResult mMKPoiResult;
	private Location lastLocation=null;
	private long lastChecked = 0;
	private boolean trafficLayerOn = false;
	
	//Map�����࣬�ṩ���롢·����ϵ��㷨֧��
	private MapUtils mMapUtils = null;
	
	//�ٶ�MapAPI�Ĺ�����
	BMapManager mBMapMan = null;
	
	// ��ȨKey
	// TODO: ����������Key,
	// �����ַ��http://dev.baidu.com/wiki/static/imap/key/
	String mStrKey = "513CBE299AB953DDFAEBC4A608F1F6557C30D685";
	boolean m_bKeyRight = true;	// ��ȨKey��ȷ����֤ͨ��
	
	// �����¼���������������ͨ�������������Ȩ��֤�����
	static class MyGeneralListener implements MKGeneralListener {
		@Override
		public void onGetNetworkState(int iError) {
			Log.d("MyGeneralListener", "onGetNetworkState error is "+ iError);
			Toast.makeText(Easyway95App.mApp.getApplicationContext(), "���������������",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onGetPermissionState(int iError) {
			Log.d("MyGeneralListener", "onGetPermissionState error is "+ iError);
			if (iError ==  MKEvent.ERROR_PERMISSION_DENIED) {
				// ��ȨKey����
				Toast.makeText(Easyway95App.mApp.getApplicationContext(), 
						"����Easyway95App.java�ļ�������ȷ����ȨKey��",
						Toast.LENGTH_LONG).show();
				Easyway95App.mApp.m_bKeyRight = false;
			}
		}
	}

	@Override
    public void onCreate() {
		Log.d("Easyway95App", "onCreate");
		mApp = this;
		mMapUtils = new MapUtils();
		
		mBMapMan = new BMapManager(this);
		mBMapMan.init(this.mStrKey, new MyGeneralListener());
		mBMapMan.getLocationManager().setNotifyInternal(10, 5);
//		if (mBMapMan != null) {
//			mBMapMan.destroy();
//			mBMapMan = null;
//		}
		
		super.onCreate();
	}

	@Override
	//��������app���˳�֮ǰ����mapadpi��destroy()�����������ظ���ʼ��������ʱ������
	public void onTerminate() {
		// TODO Auto-generated method stub
		if (mBMapMan != null) {
			mBMapMan.destroy();
			mBMapMan = null;
		}
		super.onTerminate();
	}

	
	public void setMainActivity(LYNavigator act) {
		mainActivity = act;
	}
	
	public LYNavigator getMainActivity() {
		return mainActivity;
	}
	
	public MapUtils getMapUtils() {
		return mMapUtils;
	}
	
	boolean isTinyMove(Location location) {
		if (location == null) return true;
		
		long timenow = System.currentTimeMillis();
		if (lastLocation == null || mMapUtils.getDistance(location, lastLocation)>Constants.MIN_CHK_DISTANCE || timenow-lastChecked >300*1000) {
			lastLocation = location;
			lastChecked = timenow;
			return false;
		}
		
		return true;
	}
	
	boolean getTrafficLayerFlag() {
		return trafficLayerOn;
	}
	
	void setTrafficLayerFlag(boolean flag) {
		trafficLayerOn = flag;
	}
}
