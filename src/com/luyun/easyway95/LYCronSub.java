package com.luyun.easyway95;

import android.util.Log;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.luyun.easyway95.shared.TSSProtos;
import com.luyun.easyway95.shared.TSSProtos.LYCrontab;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYRoute;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficSub;
import java.util.Iterator;
import java.util.Map;

public class LYCronSub {
	private int route_id = 2000;
	private static String TAG = "LYCronSub";
	BMapManager mBMapMan;
	private LYNavigator mainActivity;
	private Map<LYRoute, LYCrontab> mRouteCron;

	public LYCronSub(LYNavigator nv) {
		mainActivity = nv;
		mRouteCron = new java.util.HashMap<LYRoute, LYCrontab>();
		mBMapMan = mainActivity.mMapHelper.mBMapMan;
	}

	class MKSearchHelper implements MKSearchListener {
		LYCrontab tab;
		
		public MKSearchHelper(LYCrontab tab){
			this.tab = tab;
		}

		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult result,
				int iError) {
			if (result == null) {
				Log.d(TAG, "onGetDrivingRouteResult : " + iError);
				return;
			}
			LYRoute rt = TrafficSubscriber.RoadAnalyzer(result.getPlan(0).getRoute(0), (++route_id));
			mRouteCron.put(rt, tab);
			sendSubCron(rt, tab);
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
		public void onGetTransitRouteResult(MKTransitRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onGetRGCShareUrlResult(String arg0, int arg1) {
			// TODO Auto-generated method stub
		}
	}

	public void subRouteCron(GeoPoint startPt, GeoPoint endPt, boolean isGohome) {
		Log.d(TAG,
				"subaction" + " :enter subRoute, start=" + startPt.toString()
						+ ",end=" + endPt.toString());

		MKPlanNode start = new MKPlanNode();
		start.pt = startPt;
		MKPlanNode end = new MKPlanNode();
		end.pt = endPt;

		// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
		MKSearch mMKSearch = new MKSearch();
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		mMKSearch.drivingSearch(null, start, null, end);

		LYCrontab tab = isGohome ? genGohomeTab() : genGoworkTab();
		
		MKSearchHelper helper = new MKSearchHelper(tab);
		mMKSearch.init(mBMapMan, helper);
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

	public void deSubCron() {
		LYTrafficSub tsub;
		Log.d(TAG, "deSubCron key set size: " + mRouteCron.keySet().size());
		
		Iterator<LYRoute> i = mRouteCron.keySet().iterator(); 
		while(i.hasNext()){
			LYRoute route =  i.next();
			LYCrontab tab =  mRouteCron.get(route);
			tsub = TSSProtos.LYTrafficSub.newBuilder().setCity("深圳")
					.setOprType(TSSProtos.LYTrafficSub.LYOprType.LY_SUB_DELETE)
					.setPubType(TSSProtos.LYPubType.LY_PUB_CRON)
					.setCronTab(tab).setRoute(route).build();

			LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir
					.newBuilder().setVersion(1)
					.setFromParty(TSSProtos.LYParty.LY_CLIENT)
					.setToParty(TSSProtos.LYParty.LY_TSS)
					.setSndId(mainActivity.getDeviceID())
					.setMsgType(TSSProtos.LYMsgType.LY_TRAFFIC_SUB)
					.setMsgId(1000)
					.setTimestamp(System.currentTimeMillis() / 1000)
					.setTrafficSub(tsub).build();
			
			// Log.d(TAG, msg.toString());
			byte[] data = msg.toByteArray();
			mainActivity.sendMsgToSvr(data);
			this.route_id--;
		}
		
		mRouteCron.clear();
	}

	private void sendSubCron(LYRoute route, LYCrontab tab) {
		Log.d(TAG, "enter sendSubCron ");

		LYTrafficSub tsub;
		tsub = TSSProtos.LYTrafficSub.newBuilder().setCity("深圳")
				.setOprType(TSSProtos.LYTrafficSub.LYOprType.LY_SUB_CREATE)
				.setPubType(TSSProtos.LYPubType.LY_PUB_CRON).setCronTab(tab)
				.setRoute(route).build();

		LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir
				.newBuilder().setVersion(1)
				.setFromParty(TSSProtos.LYParty.LY_CLIENT)
				.setToParty(TSSProtos.LYParty.LY_TSS)
				.setSndId(mainActivity.getDeviceID())
				.setMsgType(TSSProtos.LYMsgType.LY_TRAFFIC_SUB).setMsgId(1000)
				.setTimestamp(System.currentTimeMillis() / 1000)
				.setTrafficSub(tsub).build();
		// Log.d(TAG, msg.toString());
		byte[] data = msg.toByteArray();
		mainActivity.sendMsgToSvr(data);
		
		Log.d(TAG, "mRouteCron size: " + mRouteCron.size());
	}
}
