package com.luyun.easyway95;

import com.iflytek.speech.SpeechError;
import com.iflytek.speech.SynthesizerPlayer;
import com.iflytek.speech.SynthesizerPlayerListener;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TTSService extends IntentService implements SynthesizerPlayerListener{
	private static String TAG = "TTSService";
	
	public SynthesizerPlayer mSynthetizerPlayer;
	
	public TTSService() {
		super("TTSService");
	}
	
    public class LocalBinder extends Binder {
        TTSService getService() {
            return TTSService.this;
        }
    }
    
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "in onCreate");
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "in onStart");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		String text = intent.getStringExtra("text");
		Log.d(TAG, "in onHandleIntent, text="+text);
		synthetizeInSilence(text);
	}
	
	private void synthetizeInSilence(String text) {
		if (null == mSynthetizerPlayer) {
			mSynthetizerPlayer = SynthesizerPlayer.createSynthesizerPlayer(
					this, "appid=" + getString(R.string.app_id));
		}

		int speed = 50;
		mSynthetizerPlayer.setSpeed(speed);

		int volume = 50;
		mSynthetizerPlayer.setVolume(volume);

		mSynthetizerPlayer.playText(text, null,this);
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
		MainActivity mainActivity = (MainActivity)((Easyway95App)getApplication()).getMainActivity();
		((MainActivity)mainActivity).handler.sendMessage(Message.obtain(((MainActivity)mainActivity).handler, Constants.SYNTHESIZE_DONE));
	}
}
