package com.luyun.easyway95;

import com.google.protobuf.InvalidProtocolBufferException;
import com.iflytek.speech.SpeechError;
import com.iflytek.speech.SynthesizerPlayer;
import com.iflytek.speech.SynthesizerPlayerListener;
import com.luyun.easyway95.ZMQService.LocalBinder;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class TTSService extends Service implements SynthesizerPlayerListener{
	private static String TAG = "TTSService";
	private MsgReceiver mReceiver;
	
    private static boolean mbSynthetizeOngoing = false;
	public SynthesizerPlayer mSynthetizerPlayer;
	
    public class LocalBinder extends Binder {
        TTSService getService() {
            return TTSService.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "in TTSService::onCreate");
        IntentFilter filter = new IntentFilter("ttsmsg");  
        mReceiver = new MsgReceiver();  
        registerReceiver(mReceiver,filter);  
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "in TTSService::onStart");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	/*
	 * handleIntent这个函数用于处理在startService时传递的参数
	 */
//	@Override
//	protected void onHandleIntent(Intent intent) {
//		// TODO Auto-generated method stub
//		String text = intent.getStringExtra("text");
//		Log.d(TAG, "in onHandleIntent, text="+text);
//		synthetizeInSilence(text);
//	}
//	
	private void synthetizeInSilence(String text) {
		if (null == mSynthetizerPlayer) {
			mSynthetizerPlayer = SynthesizerPlayer.createSynthesizerPlayer(
					this, "appid=" + getString(R.string.app_id));
		}

		int speed = 50;
		mSynthetizerPlayer.setSpeed(speed);

		int volume = 80;
		mSynthetizerPlayer.setVolume(volume);

		mSynthetizerPlayer.playText(text, null, this);
	}
	
	@Override
	public void onBufferPercent(int percent,int beginPos,int endPos) {
		Log.d(TAG, "in onBufferPercent");
	}

	@Override
	public void onPlayBegin() {
		Log.d(TAG, "in onPlayBegin");
	}

	@Override
	public void onPlayPaused() {
		Log.d(TAG, "in onPlayPaused");
	}

	@Override
	public void onPlayPercent(int percent,int beginPos,int endPos) {
		//Log.d(TAG, "in onPlayPercent, percent="+percent+"beginPos="+beginPos+"endPos="+endPos);
	}

	@Override
	public void onPlayResumed() {
		Log.d(TAG, "in onPlayResumed");
	}

	@Override
	public void onEnd(SpeechError arg0) {
		Log.d(TAG, "in onEnd");
        mbSynthetizeOngoing = false;
		//MainActivity mainActivity = (MainActivity)((Easyway95App)getApplication()).getMainActivity();
		//((MainActivity)mainActivity).handler.sendMessage(Message.obtain(((MainActivity)mainActivity).handler, Constants.SYNTHESIZE_DONE));
	}
	/*
	 * 也可以用点到点的方式实现Activity和Service通信，但用Broadcast显得更简洁
	 */
//	public Handler handler = new Handler() {
//		public void handleMessage(Message msg) {
//			
//		}
//    };
    
    public class MsgReceiver extends BroadcastReceiver {  
        public boolean mbRunFlagReceiver = false;  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            // TODO Auto-generated method stub  
       		Log.d(TAG, "in MsgReceiver::onReceive");
       		if (mbSynthetizeOngoing) {
       			Log.d(TAG, "ignore this message, due to preivous speech is still ongoing! ");
       			return;
       		}
       	    Bundle bundle = intent.getExtras();  
    		String msg = bundle.getString("ttsmsg");
    		Log.d(TAG, msg);
    		//TTSThread t = new TTSThread(msg); 
    		//t.start();
    		synthetizeInSilence(msg);
        }  
  
    }  

    public class TTSThread extends Thread {
		private String msTTS;
    	public TTSThread(String s) {
			msTTS = s;
		}
    	
    	@Override
		public void run() {
	        Log.d(TAG, "In TTSThead::running"); 
	        int waitingTimes = 0; //waiting upto 10 seconds
	        while (mbSynthetizeOngoing && waitingTimes < 20) {
		        Log.d(TAG, "In TTSThead::running，waiting upto 10 seconds"); 
	        	waitingTimes ++;
	        	try {
	        		Thread.sleep(500);
	        	} catch (Exception e) {
	        		
	        	} 
	        }
	        mbSynthetizeOngoing = true;
    		synthetizeInSilence(msTTS);
		}
    }
    
}
