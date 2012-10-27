package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficPub;

public class HotRoadsWithTraffic {
	private static final String TAG = "HotRoadsWithTraffic";
    //用Map保存"路及路况"
    private Map<String, RoadWithTraffic> mRoadsWithTraffic;

    public void onTraffic(LYTrafficPub trafficPub) {
		List<LYRoadTraffic> roadTraffics = trafficPub.getCityTraffic().getRoadTrafficsList();
		if (mRoadsWithTraffic == null) {
			mRoadsWithTraffic = new HashMap<String, RoadWithTraffic>();
		}
		//首先通过名字匹配，找到相应的路，然后在路上的折线点mPointsOfRoute中与RoadTraffic进行匹配
		for (int i=0; i<roadTraffics.size(); i++) {
			String road = roadTraffics.get(i).getRoad();
			Log.d(TAG, String.format("road=%s", road));
			//find by the road name
			
			RoadWithTraffic rt = mRoadsWithTraffic.get(road);
			if (rt == null) { //not in the driving roads
				mRoadsWithTraffic.put(road, (new RoadWithTraffic()).buildSegmentsFromAir(roadTraffics.get(i)));
				continue;
			} else {
				rt.buildSegmentsFromAir(roadTraffics.get(i));
			}
		}
    }
    
    public List<Map<String, Object>> getAllRoadsWithTrafficByList() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	
        Map<String, Object> map = null;
        Log.d(TAG, "fetching data from internal container");
        if (mRoadsWithTraffic == null) return list;
        Iterator it = mRoadsWithTraffic.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<String, RoadWithTraffic> entry = (Entry<String, RoadWithTraffic>) it.next();
        	RoadWithTraffic rt = (RoadWithTraffic) entry.getValue();
        	ArrayList<LYSegmentTraffic> segments = rt.getSegments();
        	if (segments == null) continue;
        	long nowTime = System.currentTimeMillis()/1000;
        	for (int i=0; i<segments.size(); i++) {
	        	long time_stamp = segments.get(i).getTimestamp();
	        	long interval = (nowTime - time_stamp)/60;
	        	if (interval > Constants.TRAFFIC_LAST_DURATION) {
	        		rt.clearSegment(i);
	        		continue;
	        	}
        		map = new HashMap<String, Object>();
	        	map.put("road", entry.getKey());
	        	map.put("desc", segments.get(i).getDetails());
	        	int speed = segments.get(i).getSpeed();
	        	String strSpeed = Constants.TRAFFIC_JAM_LVL_HIGH;
	        	if (speed >= 15) strSpeed = Constants.TRAFFIC_JAM_LVL_MIDDLE;
	        	if (speed < 15 && speed >=6) strSpeed = Constants.TRAFFIC_JAM_LVL_LOW;
	        	String formatedStr = String.format("%d分钟前，%s", interval, strSpeed);
	            map.put("timestamp", formatedStr);
	        	list.add(map);
        	}
        }
    	return list;
    }
    
    public boolean hasRoad(String road) {
		if (mRoadsWithTraffic == null || road == null) {
			return false;
		}
		return (mRoadsWithTraffic.get(road) != null);
    }


}
