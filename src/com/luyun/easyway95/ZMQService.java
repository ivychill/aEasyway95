package com.luyun.easyway95;

import org.zeromq.ZMQ;
import org.zeromq.ZMQQueue;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ZMQService extends Service {
	private static String TAG = "ZMQService";
	//zmq & protobuf
	private ZMQ.Context mzcContextInproc;   //explicitly to use two different context! Be very careful here!
	private ZMQ.Socket mzsLifeCycleInproc;  //tell mztIO thread to exit
	//private ZMQ.Socket mzsProInproc;  //owned and manipulated by main thread
	//private ZMQ.Socket mzsDevInproc;  //owned and manipulated by main thread
	
	private ZMQ.Context mzcContextSvrEnd; //initialized by iothread
	private ZMQ.Socket mzsLifeCycleSvrEnd;  //tell mztIO thread to exit
	private ZMQ.Socket mzsProSvrEnd;  //owned and manipulated by mztIO
	private ZMQ.Socket mzsDevSvrEnd;  //owned and manipulated by mztIO
	
	//private ZMQQueue mzqPro;          //pipe for production
	//private ZMQQueue mzqDev;          //pipe for production
	
	private ZMQThread mztIO;
	
	private Handler mTriggerHdl;
	private String mDeviceID;
	
	//public void registerHandler(Handler hdl) {
	//	mTriggerHdl = hdl;
	//}

	@Override
	public void onCreate() {
		super.onCreate();
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mDeviceID = tm.getDeviceId();
        
		mTriggerHdl = ((LYNavigator)((Easyway95App)getApplication()).getMainActivity()).handler;
		
		mzcContextInproc = ZMQ.context(1);
		mztIO = new ZMQThread();
		mztIO.start();
		try {
			Thread.sleep(500);
		}catch (Exception e) {
			
		}
		
        //bind inproc socket
		mzsLifeCycleInproc = mzcContextInproc.socket(ZMQ.PAIR); 
        String strLifeCycle = "inproc://lifecycle";
        mzsLifeCycleInproc.connect (strLifeCycle); 
	}
	
    public class LocalBinder extends Binder {
        ZMQService getService() {
            return ZMQService.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		mzsLifeCycleInproc.send("".getBytes(), 0); //inform iothread to exit
		mzsLifeCycleInproc.close();
		mzcContextInproc.term();
		
		super.onDestroy();
	}
	
	public void sendMsgToSvr(byte[] data) {
		Log.d(TAG, "in sendMsgToSvr!");
		mzsLifeCycleInproc.send(data, 0);
	}
    
	public class ZMQThread extends Thread {
		@Override
		public void run() {
	        Log.d(TAG, "In running"); 
			mzcContextSvrEnd = ZMQ.context(1);
			
	        //bind inproc socket
			mzsLifeCycleSvrEnd = mzcContextInproc.socket(ZMQ.PAIR); 
	        String strLifeCycle = 
					"inproc://lifecycle";
	        mzsLifeCycleSvrEnd.bind (strLifeCycle); 
	        Log.d(TAG, "bound lifecycle.");
			
//	        mzsDevSvrEnd = mzcContextSvrEnd.socket(ZMQ.DEALER); 
//	        String strDevTSS = 
//					"tcp://"
//					+Constants.TSS_DEV_HOST
//					+":"
//					+Constants.TSS_SERVER_PORT;
//	        mzsDevSvrEnd.connect (strDevTSS);
	        
	        
	        mzsProSvrEnd = mzcContextSvrEnd.socket(ZMQ.DEALER); 
	        mzsProSvrEnd.setIdentity(mDeviceID.getBytes());
	        String strProTSS = 
					"tcp://"
					+Constants.TSS_PRO_HOST
					+":"
					+Constants.TSS_SERVER_PORT;
	        mzsProSvrEnd.connect (strProTSS);
	        Log.d(TAG, strProTSS);
	        
	        //create a separate thread to retrieve data from server
			//  Initialize poll set
			ZMQ.Poller items = mzcContextSvrEnd.poller(2);
			items.register(mzsLifeCycleSvrEnd, ZMQ.Poller.POLLIN);
			//items.register(mzsDevSvrEnd, ZMQ.Poller.POLLIN);
			items.register(mzsProSvrEnd, ZMQ.Poller.POLLIN);

			//  Process messages from both sockets
			while (true) {
				byte[] data = null;
				items.poll();
				if (items.pollin(0)) {
					data = mzsLifeCycleSvrEnd.recv(0);
					if (data.length == 0) {
						break; //break the loop 
					}
					//mzsDevSvrEnd.send(data, 0);
					mzsProSvrEnd.send(data, 0);
					continue;
				}
				if (items.pollin(1)) {
					Log.i(TAG, "get data from product server");
					data = mzsProSvrEnd.recv(0);
					//data = mzsDevSvrEnd.recv(0);
				}
				/*if (items.pollin(2)) {
				Log.i(TAG, "get data from dev server");
				data = mzsDevSvrEnd.recv(0);
				}*/
		        Message msg = new Message();
		        Bundle bdl = new Bundle();
		        bdl.putByteArray(Constants.TRAFFIC_UPDATE, data);
		        msg.setData(bdl);
		        // The PendingIntent to launch our activity
		        //PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
		        //        new Intent(getApplicationContext(), MainActivity.class), 0);
		        //((MainActivity)getApplicationContext()).handler.sendMessage(msg);
		        mTriggerHdl.sendMessage(msg);
			}
			
			mzsLifeCycleSvrEnd.close();
			//mzsDevSvrEnd.close();
			mzsProSvrEnd.close();
			mzcContextSvrEnd.term();
		}
	}
}
