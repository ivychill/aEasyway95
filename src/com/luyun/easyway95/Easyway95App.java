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
	
	//百度MapAPI的管理类
	BMapManager mBMapMan = null;
	
	// 授权Key
	// TODO: 请输入您的Key,
	// 申请地址：http://dev.baidu.com/wiki/static/imap/key/
	String mStrKey = "513CBE299AB953DDFAEBC4A608F1F6557C30D685";
	boolean m_bKeyRight = true;	// 授权Key正确，验证通过
	
	// 常用事件监听，用来处理通常的网络错误，授权验证错误等
	static class MyGeneralListener implements MKGeneralListener {
		@Override
		public void onGetNetworkState(int iError) {
			Log.d("MyGeneralListener", "onGetNetworkState error is "+ iError);
			Toast.makeText(Easyway95App.mApp.getApplicationContext(), "您的网络出错啦！",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onGetPermissionState(int iError) {
			Log.d("MyGeneralListener", "onGetPermissionState error is "+ iError);
			if (iError ==  MKEvent.ERROR_PERMISSION_DENIED) {
				// 授权Key错误：
				Toast.makeText(Easyway95App.mApp.getApplicationContext(), 
						"请在Easyway95App.java文件输入正确的授权Key！",
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
	//建议在您app的退出之前调用mapadpi的destroy()函数，避免重复初始化带来的时间消耗
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
