package com.luyun.easyway95;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.GregorianCalendar;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.luyun.easyway95.Easyway95App.MyGeneralListener;
import com.luyun.easyway95.shared.TSSProtos;
import com.luyun.easyway95.shared.TSSProtos.LYCrontab;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYRoute;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficSub;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class LYCronService extends Service {
	private static int route_id = 2000;
	static int route_cnt;
	
	private static final String TAG = "LYCronService";
	MKPoiInfoHelper mHomeAddr;
	MKPoiInfoHelper mOfficeAddr;
	LYCronSub mCronSub;
	private Map<LYRoute, LYCrontab> mRouteCron;
	Easyway95App mApp;

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	public class LocalBinder extends Binder {
		LYCronService getService() {
			return LYCronService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "LYCronService onCreate");
		
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95",
				MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		mHomeAddr = up.getHomeAddr();
		mOfficeAddr = up.getOfficeAddr();
		mCronSub = new LYCronSub();
		mRouteCron = new java.util.HashMap<LYRoute, LYCrontab>();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
//		mBMapMan.destroy();
	}

	@Override
	public void onStart(Intent intent, int startid) {
//		boolean action = intent.getBooleanExtra("subscription", true);
		Log.d(TAG, "onStart : " + startid);

		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95",
				MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		mHomeAddr = up.getHomeAddr();
		mOfficeAddr = up.getOfficeAddr();

		onSubscription(up.getSubscript());
		
	}

	public void onSubscription(Boolean sub) {
		Log.d(TAG, "onSubscription action: " + sub);

		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95",
				MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		up.setAndCommitSubscript(sp, sub);

		if (sub) {
			mCronSub.subRouteCron(mOfficeAddr.getPt(), mHomeAddr.getPt());
		} else {
			mCronSub.deSubCron();
		}
	}


	// home work address change
	public void onAddressUpdate() {
		SharedPreferences sp = getSharedPreferences("com.luyun.easyway95",
				MODE_PRIVATE);
		UserProfile up = new UserProfile(sp);
		mHomeAddr = up.getHomeAddr();
		mOfficeAddr = up.getOfficeAddr();
		boolean action = up.getSubscript();

		if (action) {
			this.onSubscription(false);
			this.onSubscription(true);
		}
		mRouteCron.clear();
	}

	class LYCronSub {
		MKSearchHelper helper;
		public LYCronSub() {
			mApp = Easyway95App.mApp;
		}

		class MKSearchHelper implements MKSearchListener {
			LYCrontab tab;
			GeoPoint home;
			GeoPoint office;

			public MKSearchHelper(GeoPoint office, GeoPoint home, LYCrontab tab) {
				this.tab = tab;
				this.home = home;
				this.office = office;
			}

			@Override
			public void onGetDrivingRouteResult(MKDrivingRouteResult result,
					int iError) {
				if (result == null) {
					Log.d(TAG, "onGetDrivingRouteResult : " + iError);
					return;
				}
				LYRoute rt = TrafficSubscriber.RoadAnalyzer(result.getPlan(0)
						.getRoute(0), (++route_id));
				mRouteCron.put(rt, tab);

				Log.d(TAG, "sendSubCron route_cnt: " + route_cnt);
				sendSubCron(rt, tab);

				if ((++route_cnt) < 2) {
					Log.d(TAG, "sendSubCron circle :" + route_cnt);

					MKPlanNode start = new MKPlanNode();
					start.pt = home;
					MKPlanNode end = new MKPlanNode();
					end.pt = office;

					// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
					MKSearch mMKSearch = new MKSearch();
					mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
					mMKSearch.drivingSearch(null, start, null, end);

					helper = new MKSearchHelper(home, office, genGoworkTab());
					mMKSearch.init(mApp.mBMapMan, helper);
				}
			}

			@Override
			public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetTransitRouteResult(MKTransitRouteResult arg0,
					int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetWalkingRouteResult(MKWalkingRouteResult arg0,
					int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onGetRGCShareUrlResult(String arg0, int arg1) {
				// TODO Auto-generated method stub
			}
		}

		public void subRouteCron(GeoPoint office, GeoPoint home) {
			Log.d(TAG, "subRouteCron ");

			if(mRouteCron.size() > 0){
				reSendSubCron();
				return;
			}
			
			route_cnt = 0;
			MKPlanNode start = new MKPlanNode();
			start.pt = office;
			MKPlanNode end = new MKPlanNode();
			end.pt = home;

			// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
			MKSearch mMKSearch = new MKSearch();
			mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
			mMKSearch.drivingSearch(null, start, null, end);

			helper = new MKSearchHelper(office, home, genGohomeTab());
			mMKSearch.init(mApp.mBMapMan, helper);
			Log.d(TAG, "subRouteCron ...");
		}

		public void deSubCron() {
			LYTrafficSub tsub;
			Log.d(TAG, "deSubCron key set size: " + mRouteCron.keySet().size());

			Iterator<LYRoute> i = mRouteCron.keySet().iterator();
			while (i.hasNext()) {
				LYRoute route = i.next();
				LYCrontab tab = mRouteCron.get(route);
				tsub = TSSProtos.LYTrafficSub
						.newBuilder()
						.setCity("深圳")
						.setOprType(
								TSSProtos.LYTrafficSub.LYOprType.LY_SUB_DELETE)
						.setPubType(TSSProtos.LYPubType.LY_PUB_CRON)
						.setCronTab(tab).setRoute(route).build();

				LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir
						.newBuilder().setVersion(1)
						.setFromParty(TSSProtos.LYParty.LY_CLIENT)
						.setToParty(TSSProtos.LYParty.LY_TSS)
						.setSndId(mApp.getMainActivity().getDeviceID())
						.setMsgType(TSSProtos.LYMsgType.LY_TRAFFIC_SUB)
						.setMsgId(1000)
						.setTimestamp(System.currentTimeMillis() / 1000)
						.setTrafficSub(tsub).build();

				// Log.d(TAG, msg.toString());
				byte[] data = msg.toByteArray();
				mApp.getMainActivity().sendMsgToSvr(data);
				route_id--;
			}

//			mRouteCron.clear();
			route_cnt = 0;
		}

		private void reSendSubCron() {
			Iterator<LYRoute> i = mRouteCron.keySet().iterator();
			while (i.hasNext()) {
				LYRoute route = i.next();
				LYCrontab tb = mRouteCron.get(route);
				sendSubCron(route, tb);
			}
		}
		
		private void sendSubCron(LYRoute route, LYCrontab tab) {
			Log.d(TAG, "enter sendSubCron :" + route.getIdentity());

			LYTrafficSub tsub;
			tsub = TSSProtos.LYTrafficSub.newBuilder().setCity("深圳")
					.setOprType(TSSProtos.LYTrafficSub.LYOprType.LY_SUB_CREATE)
					.setPubType(TSSProtos.LYPubType.LY_PUB_CRON)
					.setCronTab(tab).setRoute(route).build();

			LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir
					.newBuilder().setVersion(1)
					.setFromParty(TSSProtos.LYParty.LY_CLIENT)
					.setToParty(TSSProtos.LYParty.LY_TSS)
					.setSndId(mApp.getMainActivity().getDeviceID())
					.setMsgType(TSSProtos.LYMsgType.LY_TRAFFIC_SUB)
					.setMsgId(1000)
					.setTimestamp(System.currentTimeMillis() / 1000)
					.setTrafficSub(tsub).build();
			// Log.d(TAG, msg.toString());
			byte[] data = msg.toByteArray();
			mApp.getMainActivity().sendMsgToSvr(data);

			Log.d(TAG, "mRouteCron size: " + mRouteCron.size());
		}
	}

	static public LYCrontab genGoworkTab() {
		return LYCrontab.newBuilder().setDow(62).setHour(0x1 << 8)
				.setMinute(0x1L << 0)
				.setCronType(TSSProtos.LYCrontab.LYCronType.LY_REP_DOW).build();
	}

	static public LYCrontab genGohomeTab() {
		return LYCrontab.newBuilder().setDow(62).setHour(0x1 << 18)
				.setMinute(0x1L << 0)
				.setCronType(TSSProtos.LYCrontab.LYCronType.LY_REP_DOW).build();
	}
}
