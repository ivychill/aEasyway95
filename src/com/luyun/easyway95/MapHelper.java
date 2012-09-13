package com.luyun.easyway95;

import java.util.List;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.shared.TSSProtos.SegmentTraffic;

public class MapHelper {
	public boolean createTrafficPolylineInfo(SegmentTraffic segTraffic, RoadInfo roadInfo) {
		return true;
	}
	
	public class RoadInfo {
		private String name;
		private List<PointHelper> points;
	}
	
	public class PointHelper {
		private int stepIndex;
		private int pointIndex;
		private GeoPoint coordinate;
	}
}

