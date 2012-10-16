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

//��ֱ����proto���ɵ�LYRoadTraffic�����ɣ�1.�����������������LYRoadTraffic��Ϣ����Ҫ�ȽϷ����set/get�������LYRoadTraffic����Ƚ��鷳
//���⣬������Ҫ��Э�����϶Ƚ��ͣ�ֻ����������ﴦ��ͬ��Э�顣
//ͬʱ��ԭ��Э�鴫�͵�List����Map����ʽ�����ڳ������Ч�ʵĿ���
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
	 * ��ȡMatchedPoints�б����������ڿ��ٲ�ѯ��һ��ƥ���TrafficPoint�ȹ���
	 */
	ArrayList<GeoPoint> getMatchedPointsByList() {
		if (matchedPoints == null) return null;
		
		ArrayList<GeoPoint> listPoints = new ArrayList<GeoPoint>();
    	ArrayList<LYSegmentTraffic> segments = getSegments();
    	if (segments == null) return null;
    	long nowTime = System.currentTimeMillis()/1000;
    	for (int i=0; i<segments.size(); i++) {
        	//����Ƿ���matchedPoints��map��
        	ArrayList<GeoPoint> tmpPoints = matchedPoints.get(i);
        	if (tmpPoints == null) continue; //����Map�˵���ö�����ϣ������Ƿ�����Ҳ����������·��
        	listPoints.addAll(tmpPoints);
    	}
		return listPoints;
	}
	/*
	 * ƥ���õ�������Ϣ������Щ��Ϻ�ĵ㣺������matchedPoints����Ӧ��values����ֵΪsegment������
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
	 * ����mSegmentTraffic����֮ǰ�ѽ���Ϻ��ͶӰ�㱣����matchedPoints
	 * ����findNextPoint(pt, tmpPoints)
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
    		//����һ��getNearestDistanceOfRoad�������ж�
        	double distanceOffRoad = 0.0;
    		GeoPointHelper tmpPoint = null;
    		tmpPoint = mMapUtils.findClosestPoint(pt, tmpPoints);
    		if (tmpPoint == null) continue; //��Զ���᷵��null
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
