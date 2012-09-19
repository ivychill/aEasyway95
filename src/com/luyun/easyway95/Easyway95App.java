package com.luyun.easyway95;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKEvent;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.MKPoiResult;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class Easyway95App extends Application {
	private MainActivity mainActivity;
	private SettingActivity settingActivity;
	
	static Easyway95App mApp;
	public MKPoiResult mMKPoiResult;
	
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

	
	public void setMainActivity(MainActivity act) {
		mainActivity = act;
	}
	public MainActivity getMainActivity() {
		return mainActivity;
	}
	public void setSettingActivity(SettingActivity act) {
		settingActivity = act;
	}
	public SettingActivity getSettingActivity() {
		return settingActivity;
	}
}
