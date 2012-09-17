package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.regex.*;
import java.util.*;
import android.util.Log;

import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;

public class TrafficSubscriber {
	MainActivity mainActivity;
	private static final String TAG = "TrafficSubscriber";
	
	TrafficSubscriber (MainActivity activity) {
		mainActivity = activity;
	}
	
	static com.luyun.easyway95.shared.TSSProtos.Route RoadAnalyzer (MKRoute route) {
		int mNumStep = route.getNumSteps();
		Log.d(TAG, "mNumStep..." + mNumStep);
		com.luyun.easyway95.shared.TSSProtos.Route.Builder mRouteBuilder = com.luyun.easyway95.shared.TSSProtos.Route.newBuilder();
		com.luyun.easyway95.shared.TSSProtos.Coordinate start = com.luyun.easyway95.shared.TSSProtos.Coordinate.newBuilder()
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
				com.luyun.easyway95.shared.TSSProtos.Coordinate nextStart = com.luyun.easyway95.shared.TSSProtos.Coordinate.newBuilder()
						.setLat(step.getPoint().getLatitudeE6()/1E6)
						.setLng(step.getPoint().getLongitudeE6()/1E6)
						.build();
				if (road.length() != 0) {
					com.luyun.easyway95.shared.TSSProtos.Segment segment = com.luyun.easyway95.shared.TSSProtos.Segment.newBuilder()
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
	    com.luyun.easyway95.shared.TSSProtos.Route mRoute = RoadAnalyzer (route);
	    Log.d(TAG, mRoute.toString());
		com.luyun.easyway95.shared.TSSProtos.TrafficSub tsub = com.luyun.easyway95.shared.TSSProtos.TrafficSub.newBuilder()
				.setCity("ÉîÛÚ")
				.setOprType(com.luyun.easyway95.shared.TSSProtos.OprType.SUB_CREATE)
				.setPubType(com.luyun.easyway95.shared.TSSProtos.PubType.PUB_ONCE)
				.setRoute(mRoute)
				.build();
    	com.luyun.easyway95.shared.TSSProtos.Package pkg = com.luyun.easyway95.shared.TSSProtos.Package.newBuilder()
				.setVersion(1)
				.setMsgDir(com.luyun.easyway95.shared.TSSProtos.MsgDir.CLIENT2TSS)
				.setMsgType(com.luyun.easyway95.shared.TSSProtos.MsgType.TRAFFIC_SUB)
				.setMsgId(1000)
				.setTimestamp(System.currentTimeMillis()/1000)
				.setTrafficSub(tsub)
				.build();
    	Log.i(TAG, pkg.toString());
    	byte[] data = pkg.toByteArray();
    	mainActivity.sendMsgToSvr(data);
	}
}
