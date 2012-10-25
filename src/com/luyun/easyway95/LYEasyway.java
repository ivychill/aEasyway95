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
 * ����ĵ�һ����ڣ�ʵ�֣�
 * 0�����Android�汾�����OS>4.0��������hardwareAcceleratedΪfalse
 * 1���������״̬
 * 2����ʾ���ܽ��ܵĶ���ͼƬ������ʹ��״̬��SharedPreferences���Profile�޹أ�������Լ��İ汾������Ǳ��汾��һ��ʹ�ã�����ʾ��Щ���ܽ��ܶ���
 * 		����ֱ�ӻ��������һ������ʾ��������״̬
 * 3������LYNavigator
 * 4��ע���Լ�
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
		//��ѯ�汾
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
        		//�ж����������Ƿ����� 
        		//��ʾ�������磬�����������δ�ɹ�����һֱ��������һ��
        		boolean isNetworked = isConnectedToInternet();

        		if (isNetworked == false) {
    	            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//���������������ý���
    			    return;
//        			new AlertDialog.Builder(LYEasyway.this) 
//        			    .setTitle("�����������")
//        			    .setMessage("�㵱ǰû���������ӡ�Ҫ����ʵʱ·������Ҫ���������ӡ�")
//        			    .setPositiveButton("����", 
//        			    	new DialogInterface.OnClickListener() {
//        			        public void onClick(DialogInterface dialog, int which) {
//        			        	willSetNetwork = true;
//        			        }
//        			     })
//        			    .setNegativeButton("ȡ��", null)
//        			    .show();
        		}
        		
//        		if (willSetNetwork)
//        		{
//    	            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//���������������ý���
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
		// �������ͣ�  
		// e1����1��ACTION_DOWN MotionEvent   
		// e2�����һ��ACTION_MOVE MotionEvent   
		// velocityX��X���ϵ��ƶ��ٶȣ�����/��   
		// velocityY��Y���ϵ��ƶ��ٶȣ�����/��   
		// �������� ��   
		// X�������λ�ƴ���FLING_MIN_DISTANCE�����ƶ��ٶȴ���FLING_MIN_VELOCITY������/��   
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
        
        //���3G�����wifi���綼δ���ӣ��Ҳ��Ǵ�����������״̬ �����Network Setting���� ���û�������������
        if(mobile==State.CONNECTED||mobile==State.CONNECTING)
            return true;
        if(wifi==State.CONNECTED||wifi==State.CONNECTING)
            return true;
        return false;
	}
	
}
