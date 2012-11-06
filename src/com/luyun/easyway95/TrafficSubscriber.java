package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.regex.*;
import java.util.*;
import android.util.Log;

import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;
import com.luyun.easyway95.shared.TSSProtos.LYCoordinate;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYRoute;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficSub;

public class TrafficSubscriber {
	LYNavigator mainActivity;
	private static final String TAG = "TrafficSubscriber";
	
	TrafficSubscriber (LYNavigator activity) {
		mainActivity = activity;
	}
	
	static LYRoute RoadAnalyzer (MKRoute route) {
		int mNumStep = route.getNumSteps();
		Log.d(TAG, "mNumStep..." + mNumStep);
		com.luyun.easyway95.shared.TSSProtos.LYRoute.Builder mRouteBuilder = com.luyun.easyway95.shared.TSSProtos.LYRoute.newBuilder();
		com.luyun.easyway95.shared.TSSProtos.LYCoordinate start = com.luyun.easyway95.shared.TSSProtos.LYCoordinate.newBuilder()
				.setLat(0)
				.setLng(0)
				.build();
		String road = "";
		String nextRoad = "";
		//boolean isNextRoad = false;
		
		for (int index = 0; index < mNumStep; index++){
			Log.d(TAG, "step index..." + index);
			MKStep step = route.getStep(index);
			//Log.d(TAG, "step point..." + step.getPoint().toString());
			Log.d(TAG, "step content..." + step.getContent());
			Scanner scanner = new Scanner(step.getContent());
			scanner.useDelimiter("\\s*-\\s*");
			String pattern = ".*½øÈë(.*)";
			if(scanner.hasNext(pattern)) {
				scanner.next(pattern);
				MatchResult match = scanner.match();
				nextRoad = match.group(1);
				//isNextRoad = true;
				Log.d(TAG, "road..." + nextRoad);
				LYCoordinate nextStart = com.luyun.easyway95.shared.TSSProtos.LYCoordinate.newBuilder()
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
				Log.d(TAG, "no pattern matched");
			}
		}
		
		return mRouteBuilder.setIdentity(1).build();
	}
	
	void SubTraffic (MKRoute route) {
	    LYRoute mRoute = RoadAnalyzer (route);
	    Log.d(TAG, mRoute.toString());
	    
		LYTrafficSub tsub = com.luyun.easyway95.shared.TSSProtos.LYTrafficSub.newBuilder()
				.setCity("ÉîÛÚ")
				.setOprType(com.luyun.easyway95.shared.TSSProtos.LYTrafficSub.LYOprType.LY_SUB_CREATE)
				.setPubType(com.luyun.easyway95.shared.TSSProtos.LYTrafficSub.LYPubType.LY_PUB_EVENT)
				.setRoute(mRoute)
				.build();
		byte[] payload = tsub.toByteArray();
		int checkSum = LYCheckSum.genCheckSum(payload);
		Log.d(TAG, String.format("check sum %d", checkSum));
		
    	LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.newBuilder()
				.setVersion(1)
				.setFromParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_CLIENT)
				.setToParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_TSS)
				.setSndId(mainActivity.getDeviceID())
				.setMsgType(com.luyun.easyway95.shared.TSSProtos.LYMsgType.LY_TRAFFIC_SUB)
				.setMsgId(1000)
				.setTimestamp(System.currentTimeMillis()/1000)
				.setChecksum(checkSum)
				.setTrafficSub(tsub)
				.build();
    	//Log.d(TAG, msg.toString());
    	byte[] data = msg.toByteArray();
    	mainActivity.sendMsgToSvr(data);
	}
}
