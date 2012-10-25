package com.luyun.easyway95;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/*
 * 程序的第一个入口，实现：
 * 0、检查Android版本，如果OS>4.0，则设置hardwareAccelerated为false
 * 1、检查网络状态
 * 2、显示功能介绍的动画图片：保存使用状态在SharedPreferences里（跟Profile无关），检查自己的版本，如果是本版本第一次使用，则显示这些功能介绍动画
 * 		否则，直接滑动到最后一屏，显示网络连接状态
 * 3、启动LYNavigator
 * 4、注销自己
 */
public class LYEasyway extends Activity implements OnTouchListener, OnGestureListener{
	final static String TAG = "LYEasyway";
	private GestureDetector mGestureDetector; 
	ImageView mImageView;
	int mImageResource[] = {R.drawable.instro_1, R.drawable.instro_2, R.drawable.instro_3, R.drawable.instro_4};
	int mImageCursor = 0;
	String mRelease;
	boolean showGuide = false;
	boolean willSetNetwork = false;

    private ProgressDialog internetConnectionDlg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGestureDetector  = new GestureDetector(this,(android.view.GestureDetector.OnGestureListener) this);
	    
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		//查询版本
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mRelease = sp.getString("ReleaseVersion", null);
		if (mRelease == null || !mRelease.equals(Constants.RELEASE_VERSION)) {
			SharedPreferences.Editor ed = sp.edit();
			ed.putString("ReleaseVersion", Constants.RELEASE_VERSION);
			ed.commit();
			showGuide = true;
		}
		
		setContentView(R.layout.ly_easyway);
	    mImageView = (ImageView) findViewById(R.id.introduction);  
	    mImageView.setLongClickable(true);
	    mImageView.setOnTouchListener(this);
	    

        Button btnStart = (Button)findViewById(R.id.start_use);
        btnStart.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//判断网络连接是否正常 
        		//显示连接网络，如果网络连接未成功，则一直不进行下一步
        		boolean isNetworked = isConnectedToInternet();

        		if (isNetworked == false) {
    	            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//进入无线网络配置界面
    			    return;
//        			new AlertDialog.Builder(LYEasyway.this) 
//        			    .setTitle("请打开网络连接")
//        			    .setMessage("你当前没有网络连接。要接收实时路况，需要打开网络连接。")
//        			    .setPositiveButton("设置", 
//        			    	new DialogInterface.OnClickListener() {
//        			        public void onClick(DialogInterface dialog, int which) {
//        			        	willSetNetwork = true;
//        			        }
//        			     })
//        			    .setNegativeButton("取消", null)
//        			    .show();
        		}
        		
//        		if (willSetNetwork)
//        		{
//    	            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//进入无线网络配置界面
//    			    return;
//        		}
        		//start LYNavigator activity
        		startActivity(new Intent(LYEasyway.this, LYNavigator.class));
        		LYEasyway.this.finish();
        	}
        });
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		boolean isNetworked = isConnectedToInternet();
		if (isNetworked == true && showGuide == false) {
			//start LYNavigator activity
			startActivity(new Intent(LYEasyway.this, LYNavigator.class));
    		LYEasyway.this.finish();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return mGestureDetector.onTouchEvent(event);
//		Log.d(TAG, "onTouch");
//		return false;
	}

    public boolean onTouchEvent(MotionEvent me){ 
    	//this.gTap.onTouchEvent(me);
    	return super.onTouchEvent(me); 
    }

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "onDown");
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		// 参数解释：  
		// e1：第1个ACTION_DOWN MotionEvent   
		// e2：最后一个ACTION_MOVE MotionEvent   
		// velocityX：X轴上的移动速度，像素/秒   
		// velocityY：Y轴上的移动速度，像素/秒   
		// 触发条件 ：   
		// X轴的坐标位移大于FLING_MIN_DISTANCE，且移动速度大于FLING_MIN_VELOCITY个像素/秒   
		Log.d(TAG, "onFling ");
		if (e1.getX() - e2.getX() > 100      
		             && Math.abs(velocityX) > 200) {      
		  
		    // Fling left   
		    mImageCursor =  mImageCursor + 1;
		    if (mImageCursor > 3) mImageCursor = 3;
		     mImageView.setImageResource(mImageResource[mImageCursor]);
		} else if (e2.getX() - e1.getX() > 100      
		             && Math.abs(velocityX) > 200) {      
		     // Fling right   
		    mImageCursor =  mImageCursor - 1;
		    if (mImageCursor < 0) mImageCursor = 0;
		     mImageView.setImageResource(mImageResource[mImageCursor]);
		}      		
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onLongPress");
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "onScroll");
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onShowPress");
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSingleTapUp");
		return false;
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
	
}
