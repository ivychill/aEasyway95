package com.luyun.easyway95;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
		Log.d(TAG, "onCreate");
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
		Log.d(TAG, "onBind");
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
		mzsLifeCycleInproc.send(Constants.ZMQ_QUITTING_CMD.getBytes(), 0); //inform iothread to exit
		mzsLifeCycleInproc.close();
		mzcContextInproc.term();
		
		super.onDestroy();
	}
	
	public void reconnect() {
		mzsLifeCycleInproc.send(Constants.ZMQ_RECONNECT_CMD.getBytes(), 0); //inform iothread to reconnect
	}
	
	public void sendMsgToSvr(byte[] data) {
		mzsLifeCycleInproc.send(data, 0);
	}
    
	public class ZMQThread extends Thread {
		String strProTSS;
		ZMQ.Poller items;
		
		@Override
		public void run() {
	        Log.d(TAG, "In running"); 
			mzcContextSvrEnd = ZMQ.context(1);
			items = mzcContextSvrEnd.poller(2);
			
	        //bind inproc socket
			mzsLifeCycleSvrEnd = mzcContextInproc.socket(ZMQ.PAIR); 
	        String strLifeCycle = 
					"inproc://lifecycle";
	        mzsLifeCycleSvrEnd.bind (strLifeCycle); 
	        //create a separate thread to retrieve data from server
			//  Initialize poll set
			items.register(mzsLifeCycleSvrEnd, ZMQ.Poller.POLLIN);
			
	        connectSVR();


			//  Process messages from both sockets
			while (true) {
				byte[] data = null;
				items.poll();
				if (items.pollin(0)) {
					data = mzsLifeCycleSvrEnd.recv(0);
					//2�ֽڰ����ڲ�����
					if (data.length == 2) {
						String cmd = new String(data);
						if (cmd.equals(Constants.ZMQ_QUITTING_CMD)) {
							Log.d(TAG, "ZMQService quitting...");
							break; //break the loop 
						} else if (cmd.equals(Constants.ZMQ_RECONNECT_CMD)) {
							Log.d(TAG, "ZMQService reconnecting ...");
							mzsProSvrEnd.close();
							connectSVR();
							continue;
						}
					}
					//mzsDevSvrEnd.send(data, 0);
			        Log.d(TAG, String.format("send msg, ZMQID: %s, IP: %s", new String(mzsProSvrEnd.getIdentity()), getLocalIpAddress()));
					mzsProSvrEnd.send(data, 0);
					continue;
				}
				if (items.pollin(1)) {
					Log.d(TAG, "get data from product server");
					data = mzsProSvrEnd.recv(0);
					//data = mzsDevSvrEnd.recv(0);
				}
				/*if (items.pollin(2)) {
				Log.d(TAG, "get data from dev server");
				data = mzsDevSvrEnd.recv(0);
				}*/
		        Message msg = new Message();
		        msg.what = Constants.TRAFFIC_UPDATE_CMD;
		        Bundle bdl = new Bundle();
		        bdl.putByteArray(Constants.TRAFFIC_UPDATE, data);
		        msg.setData(bdl);
		        // The PendingIntent to launch our activity
		        //PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
		        //        new Intent(getApplicationContext(), MainActivity.class), 0);

		        mTriggerHdl.sendMessage(msg);
			}
			
			mzsLifeCycleSvrEnd.close();
			//mzsDevSvrEnd.close();
			mzsProSvrEnd.close();
			mzcContextSvrEnd.term();
		}
		
		public void connectSVR() {
			if (mzsProSvrEnd != null) {
				items.unregister(mzsProSvrEnd);
			}
	        mzsProSvrEnd = mzcContextSvrEnd.socket(ZMQ.DEALER); 
	        try {
		        strProTSS = 
						"tcp://"
						+Constants.TSS_PRO_HOST
						+":"
						+Constants.TSS_SERVER_PORT;
		        mzsProSvrEnd.connect (strProTSS);
		        String localIP = getLocalIpAddress();
		        if (localIP != null) {
		        	Log.d(TAG, String.format("ZMQ ID: %s, IP: %s connected to %s", new String(mzsProSvrEnd.getIdentity()), localIP, strProTSS));
		        }
	        } catch (Exception e) {
	        	Log.e(TAG, "connect to server failed."+e.getMessage());
	        }
			items.register(mzsProSvrEnd, ZMQ.Poller.POLLIN);
		}
	}
	
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(TAG, ex.toString());
	    }
	    return null;
	}
}
