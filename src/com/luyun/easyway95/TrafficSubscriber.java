package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.regex.*;
import java.util.*;
import android.util.Log;

import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;
import com.google.protobuf.ByteString;
import com.luyun.easyway95.shared.TSSProtos.LYCoordinate;
import com.luyun.easyway95.shared.TSSProtos.LYCrontab;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYRoute;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficSub;
import com.luyun.easyway95.shared.TSSProtos;

public class TrafficSubscriber {
	LYNavigator mainActivity;
	private static final String TAG = "TrafficSubscriber";
	
	TrafficSubscriber (LYNavigator activity) {
		mainActivity = activity;
	}
	
	static LYRoute RoadAnalyzer (MKRoute route, int routeid) {
		int mNumStep = route.getNumSteps();
		//Log.d(TAG, "mNumStep..." + mNumStep);
		LYRoute.Builder mRouteBuilder = LYRoute.newBuilder();
		LYCoordinate start = LYCoordinate.newBuilder().setLat(0).setLng(0).build();
		LYCoordinate nextStart = LYCoordinate.newBuilder().setLat(0).setLng(0).build();
		String road = "";
		String nextRoad = "";
		MKStep step = null;
		//boolean isNextRoad = false;
		
		for (int index = 0; index < mNumStep; index++){
			//Log.d(TAG, "step index..." + index);
			step = route.getStep(index);
//			Log.d(TAG, "step point..." + step.getPoint().toString());
//			Log.d(TAG, "step content..." + step.getContent());
			Scanner scanner = new Scanner(step.getContent());
			scanner.useDelimiter("\\s*-\\s*");
			String pattern = ".*进入(.*)";
			if(scanner.hasNext(pattern)) {
				scanner.next(pattern);
				MatchResult match = scanner.match();
				nextRoad = match.group(1);
				//isNextRoad = true;
				//Log.d(TAG, "road..." + nextRoad);
				nextStart = com.luyun.easyway95.shared.TSSProtos.LYCoordinate.newBuilder()
						.setLat(step.getPoint().getLatitudeE6()/1E6)
						.setLng(step.getPoint().getLongitudeE6()/1E6)
						.build();
				if (road.length() != 0) {
					LYSegment segment = com.luyun.easyway95.shared.TSSProtos.LYSegment.newBuilder()
							.setRoad(road)
							.setStart(start)
							.setEnd(nextStart)
							.build();
					mRouteBuilder.addSegments(segment);
				}
				road = nextRoad;
				start = nextStart;
			}
			else {
				//isNextRoad = false;
				//Log.d(TAG, "no pattern matched");
			}
		}
		
		//最后一段
		if (null != step) {
			LYCoordinate end = LYCoordinate.newBuilder()
					.setLat(step.getPoint().getLatitudeE6()/1E6)
					.setLng(step.getPoint().getLongitudeE6()/1E6)
					.build();
			LYSegment segment = LYSegment.newBuilder()
					.setRoad(nextRoad)
					.setStart(nextStart)
					.setEnd(end)
					.build();
			mRouteBuilder.addSegments(segment);
		}

		return mRouteBuilder.setIdentity(routeid).build();
	}
	
	void SubTraffic (MKRoute route) {
	    LYRoute mRoute = RoadAnalyzer (route, 1024);
//	    Log.d(TAG, "SubTraffic, route:\n" + mRoute.toString());

		LYTrafficSub tsub = com.luyun.easyway95.shared.TSSProtos.LYTrafficSub.newBuilder()
				.setCity("深圳")
				.setOprType(com.luyun.easyway95.shared.TSSProtos.LYTrafficSub.LYOprType.LY_SUB_CREATE)
				.setPubType(com.luyun.easyway95.shared.TSSProtos.LYPubType.LY_PUB_EVENT)
				.setRoute(mRoute)
				.build();
		
		byte[] payload = tsub.toByteArray();
		
    	LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.newBuilder()
				.setVersion(1)
				.setFromParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_CLIENT)
				.setToParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_TSS)
				.setSndId(mainActivity.getDeviceID())
				.setMsgType(com.luyun.easyway95.shared.TSSProtos.LYMsgType.LY_TRAFFIC_SUB)
				.setMsgId(1024)
				.setTimestamp(System.currentTimeMillis()/1000)
				.setTrafficSub(tsub)
				.build();
    	//Log.d(TAG, msg.toString());
    	byte[] data = msg.toByteArray();
    	mainActivity.sendMsgToSvr(data);
	}
}
