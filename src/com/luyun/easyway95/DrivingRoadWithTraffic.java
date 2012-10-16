package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.location.Location;
import android.util.Log;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.MapUtils.GeoPointHelper;
import com.luyun.easyway95.MapUtils.STPointLineDistInfo;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;

//不直接用proto生成的LYRoadTraffic的理由：1.这个用作容器，收纳LYRoadTraffic信息，需要比较方便的set/get，如果用LYRoadTraffic，会比较麻烦
//另外，容器需要跟协议的耦合度降低，只在这个容器里处理不同的协议。
//同时将原来协议传送的List换成Map的形式，便于程序处理和效率的考虑
public class DrivingRoadWithTraffic extends RoadWithTraffic {
	private static final String TAG = "DrivingRoadWithTraffic";
	private MapUtils mMapUtils = null;
	
	private ArrayList<GeoPoint> mPointsOfRoute;
	private Map<Integer, ArrayList<GeoPoint>> matchedPoints;
	
	DrivingRoadWithTraffic(MapUtils mu) {
		mMapUtils = mu;
	}

	@Override
	void clearSegment(int i) {
		super.clearSegment(i);
		if (matchedPoints != null) matchedPoints.remove(new Integer(i));
	}
	
	void addPointsToRoute(ArrayList<GeoPoint> points) {
		if (mPointsOfRoute == null) {
			mPointsOfRoute = new ArrayList<GeoPoint>();
		}
		mPointsOfRoute.addAll(points);
	}
	
	Map<Integer, ArrayList<GeoPoint>> getMatchedPoints() {
		if (matchedPoints == null) return null;
		return matchedPoints;
	}
	
	/*
	 * 获取MatchedPoints列表，调用者用于快速查询下一个匹配的TrafficPoint等功能
	 */
	ArrayList<GeoPoint> getMatchedPointsByList() {
		if (matchedPoints == null) return null;
		
		ArrayList<GeoPoint> listPoints = new ArrayList<GeoPoint>();
    	ArrayList<LYSegmentTraffic> segments = getSegments();
    	if (segments == null) return null;
    	long nowTime = System.currentTimeMillis()/1000;
    	for (int i=0; i<segments.size(); i++) {
        	//检查是否在matchedPoints的map里
        	ArrayList<GeoPoint> tmpPoints = matchedPoints.get(i);
        	if (tmpPoints == null) continue; //不在Map里，说明该段无拟合，可能是反方向，也可能是其它路况
        	listPoints.addAll(tmpPoints);
    	}
		return listPoints;
	}
	/*
	 * 匹配后得到如下信息：有哪些拟合后的点：保存在matchedPoints所对应的values里，其键值为segment的索引
	 */
	void matchRoute(LYRoadTraffic rt) {
		this.buildSegmentsFromAir(rt);
		matchedPoints = new HashMap<Integer, ArrayList<GeoPoint>>();
		//Log.d(TAG, mPointsOfRoute.toString());
		ArrayList<LYSegmentTraffic> segments = getSegments();
		for (int i=0; i<segments.size(); i++) {
    		ArrayList<GeoPoint> tmpMatchedPoints = new ArrayList();
    		//Log.d(TAG, segments.get(i).getSegment().toString());
    		mMapUtils.MatchRoadAndTraffic(segments.get(i).getSegment(), mPointsOfRoute, tmpMatchedPoints);
			if (tmpMatchedPoints.size() >0) {
				matchedPoints.put(i, tmpMatchedPoints);
			}
		}
		Log.d(TAG, "road="+getHref()+"matchedPoints="+matchedPoints.size()+","+matchedPoints.toString());
	}

	/*
	 * 遍历mSegmentTraffic，因之前已将拟合后的投影点保存在matchedPoints
	 * 调用findNextPoint(pt, tmpPoints)
	 */
	public TrafficPoint getNextTrafficPoint(GeoPoint pt) {
		//Log.d(TAG, "in getNextTrafficPoint, point="+pt.toString());
		if (matchedPoints == null || matchedPoints.size() == 0) return null;
		//Log.d(TAG, matchedPoints.toString());
		ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();
		int indexOfSegment = -1;
		GeoPointHelper nextPoint = null;
		ArrayList<LYSegmentTraffic> segments = getSegments();
		for (int i=0; i<segments.size(); i++) {
			ArrayList<GeoPoint> tmpPoints = matchedPoints.get(new Integer(i));
			if (tmpPoints == null) continue;
    		//调用一次getNearestDistanceOfRoad，用于判断
        	double distanceOffRoad = 0.0;
    		GeoPointHelper tmpPoint = null;
    		tmpPoint = mMapUtils.findClosestPoint(pt, tmpPoints);
    		if (tmpPoint == null) continue; //永远不会返回null
    		if (nextPoint == null || tmpPoint.getDistance() < nextPoint.getDistance()) {
    			nextPoint = tmpPoint;
    			indexOfSegment = i;
    		}
		}
		if (nextPoint == null || indexOfSegment < 0) return null;
		//Log.d(TAG, "in getNextTrafficPoint, found nextPoint="+nextPoint.toString());
		TrafficPoint tp = new TrafficPoint();
		tp.setPoint(nextPoint.getPoint());
		tp.setDistance(nextPoint.getDistance());
		tp.setDesc(segments.get(indexOfSegment).getDetails());
		tp.setSpeed(segments.get(indexOfSegment).getSpeed());
		//Log.d(TAG, "in getNextTrafficPoint, found nextTrafficPoint="+tp.toString());
		return tp;
	}
	
	@Override
	public  DrivingRoadWithTraffic buildSegmentsFromAir(LYRoadTraffic rt) {
		return (DrivingRoadWithTraffic)super.buildSegmentsFromAir(rt);
	}
	
}
