package com.luyun.easyway95;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.MatchResult;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficPub;
import com.luyun.easyway95.MapUtils.GeoPointHelper;
import com.luyun.easyway95.MapUtils.STPointLineDistInfo;

/*
 * MKRouteHelper��һ����������Baidu��MKRoute�����ɣ�Ϊÿ��·����һ��DrivingRoadWithTraffic
 */
public class MKRouteHelper implements Serializable{
	private static final String TAG = "MKRouteHelper";
	private MapUtils mMapUtils = null;
	
    private ArrayList<ArrayList<GeoPoint>> mAllPoints; //from driving route
    private ArrayList<GeoPoint> mAllPoints2; //one by one
    private ArrayList<GeoPoint> mMatchedPoints; //ǰ��ӵ�µ㼯�ϣ�һ��������б�
    
    private int mDistance;
    private GeoPoint mStart;
    private GeoPoint mEnd;
    private int mIndex;
    private int mNumSteps;
    private int mRouteType;
    private ArrayList<MKStep> mAllSteps;
    private int stepCursor = 0; //����ÿ��ȫ��ɨ�����е�·
    private MKRoute mRawRoute;
    
    //��Map����"·��·��"
    private Map<String, DrivingRoadWithTraffic> mRoadsWithTraffic;
    //����һ��Map����Step index��Road��Ӧ��ϵ�����ڿ��ٲ��Ҹ�Step��Road�Ķ�Ӧ��ϵ
    //֮���Բ���DrivingRoadWithTraffic�ﱣ��Step���б���������Ȼ�ģ����ǹؼ�����Ҫ���ٴ�step index���ҵ�·
    //����һ��˼·�ǣ�����һ��MKStepHelper��ר������һ����ԱRoad�����ַ����е���
    private Map<Integer, String> mStepsOfRoad;
	//private ArrayList<GeoPoint> matchedPoints;
    
	MKRouteHelper(MKRoute mkr, MapUtils mu) {
		mMapUtils = mu;
		mRawRoute = mkr;
		mAllPoints = mkr.getArrayPoints();
		mDistance = mkr.getDistance();
		mEnd = mkr.getEnd();
		mIndex = mkr.getIndex();
		mNumSteps = mkr.getNumSteps();
		mRouteType = mkr.getRouteType();
		mStart = mkr.getStart();
		mAllSteps = new ArrayList();
		Log.d(TAG, String.format("arrayPoints.size=%d, stepsize=%d", mAllPoints.size(), mNumSteps));
		for (int i=0; i<mNumSteps; i++) {
			mAllSteps.add(mkr.getStep(i));
		}
		mRoadsWithTraffic = new HashMap<String, DrivingRoadWithTraffic>();
		buildRoadsFromRoute();
    }
    
	MKRoute getRawRoute() {
		return mRawRoute;
	}
	
    ArrayList<ArrayList<GeoPoint>> getAllPoints() {
    	return mAllPoints;
    }
    
    public Map<String, DrivingRoadWithTraffic>  getRoadsWithTraffic() {
    	return mRoadsWithTraffic;
    }
    
    ArrayList<GeoPoint> getAllPoints2() { //get all points in one dimentional array
    	if (mAllPoints2 != null) return mAllPoints2;
    	mAllPoints2 = new ArrayList();
    	for (int i=0; i<mAllPoints.size(); i++) {
    		mAllPoints2.addAll(mAllPoints.get(i));
    	}
    	return mAllPoints2;
    }
    
    /*
     * ����stepCursor����Ӧ��step���ҳ�·����Ȼ���ҳ���·��������matchPoint
     * ���step.getContent()���������롰����ʾ�Ǵ�һ��·�л�����һ��·
     */
    public TrafficPoint getNextTrafficPoint(GeoPoint currentPoint) {
    	Log.d(TAG, "in MKRouteHelper::getTrafficPoint, stepCursor="+stepCursor);
    	if (stepCursor < 0 || mMatchedPoints == null || mMatchedPoints.size() == 0) {
    		return null;
    	}
    	String roadName = null;
    	//��ǰ����·��ƥ������ĵ�
    	for (int i=stepCursor; i<mAllSteps.size();i++) {
    		roadName = mStepsOfRoad.get(new Integer(i));
    		if (roadName == null) continue;
    		DrivingRoadWithTraffic rt = mRoadsWithTraffic.get(roadName);
    		if (rt == null) continue;
    		TrafficPoint tp = rt.getNextTrafficPoint(currentPoint);
    		if (tp == null) continue;
    		tp.setRoad(roadName);
    		return tp;
    	}
    	return null;
    }
    
    /*
     * ����stepCursor����Ӧ��step���ҳ�·����Ȼ���ҳ���·��������matchPoint
     * ���step.getContent()���������롰����ʾ�Ǵ�һ��·�л�����һ��·
     */
    public ArrayList<TrafficPoint> getAllTrafficPointsAhead(GeoPoint currentPoint) {
    	Log.d(TAG, "in MKRouteHelper::getAllTrafficPointsAhead, stepCursor="+stepCursor);
    	if (stepCursor < 0 || mMatchedPoints == null || mMatchedPoints.size() == 0) {
    		return null;
    	}
    	String roadName = null;
    	ArrayList<TrafficPoint> allPointsAhead = new ArrayList<TrafficPoint>();
    	//��ǰ����·��ƥ������ĵ�
    	for (int i=stepCursor; i<mAllSteps.size();i++) {
    		roadName = mStepsOfRoad.get(new Integer(i));
    		if (roadName == null) continue;
    		DrivingRoadWithTraffic rt = mRoadsWithTraffic.get(roadName);
    		if (rt == null) continue;
    		TrafficPoint tp = rt.getNextTrafficPoint(currentPoint);
    		if (tp == null) continue;
    		tp.setRoad(roadName);
    		Log.d(TAG, tp.toString());
    		allPointsAhead.add(tp);
    	}
    	return allPointsAhead;
    }
    
    /*
     * ����stepCursor��Ѱ�ҵ�ǰ�����桢ǰ������step��pointsȥƥ�䣬ͬʱ����stepCursor
     * �����ƥ�䣬���ͷѭ��step�ҳ�ƥ��㣬ͬʱ���¸�cursor
     * ��ǰ����step���ҳ�·����Ȼ���·���ҳ�matchedPoints
     */
    public boolean isOnRoute(GeoPoint currentPoint) {
    	int i = 0;
    	int startIndex = stepCursor>2?stepCursor-3:0;
    	int endIndex = stepCursor+3<mAllPoints.size()?stepCursor+3:mAllPoints.size();
    	//����������ܵ�next 3��step
    	for (i=stepCursor>=0?stepCursor:0; i<endIndex; i++) {
        	STPointLineDistInfo stPointLineDistInfo = mMapUtils.new STPointLineDistInfo();
        	double distanceOffRoad = 0.0;
    		distanceOffRoad = mMapUtils.getNearestDistanceOfRoad(currentPoint, mAllPoints.get(i), stPointLineDistInfo);
        	if (Math.abs(distanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { 
        		//on road
        		stepCursor = i;
        		return true;
        	}
    	}
    	//�������������ܵ�previous 3��step
    	for (i=startIndex; i<stepCursor; i++) {
        	STPointLineDistInfo stPointLineDistInfo = mMapUtils.new STPointLineDistInfo();
        	double distanceOffRoad = 0.0;
    		distanceOffRoad = mMapUtils.getNearestDistanceOfRoad(currentPoint, mAllPoints.get(i), stPointLineDistInfo);
        	if (Math.abs(distanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { 
        		//on road
        		stepCursor = i;
        		return true;
        	}
    	}
    	//���ܿ���ƥ�����ͷѭ�����ң��Ƿ����ȡgetAllPoints2()���ҳ���ǰ��ͶӰ����һ�Σ�Ȼ���ټ����stepCursor��������һ��������������
    	for (i=0; i<mAllPoints.size(); i++) {
        	STPointLineDistInfo stPointLineDistInfo = mMapUtils.new STPointLineDistInfo();
        	double distanceOffRoad = 0.0;
    		distanceOffRoad = mMapUtils.getNearestDistanceOfRoad(currentPoint, mAllPoints.get(i), stPointLineDistInfo);
        	if (Math.abs(distanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { 
        		//on road
        		stepCursor = i;
        		return true;
        	}
    	}
    	stepCursor = -1; //���ڱ�DrivingRoute��
    	return false;
    }
    
    ArrayList<GeoPoint> getMatchedPoints() {
    	return mMatchedPoints;
    }
    
    public void onTraffic(LYTrafficPub trafficPub, boolean rebuildRoads) {
		List<LYRoadTraffic> roadTraffics = trafficPub.getCityTraffic().getRoadTrafficsList();
		if (mRoadsWithTraffic == null || rebuildRoads) {
			mRoadsWithTraffic = new HashMap<String, DrivingRoadWithTraffic>();
			mMatchedPoints = new ArrayList<GeoPoint>();
		}
		//����ͨ������ƥ�䣬�ҵ���Ӧ��·��Ȼ����·�ϵ����ߵ�mPointsOfRoute����RoadTraffic����ƥ��
		for (int i=0; i<roadTraffics.size(); i++) {
			String road = roadTraffics.get(i).getRoad();
			Log.d(TAG, String.format("road=%s", road));
			//find by the road name
			
			DrivingRoadWithTraffic rt = mRoadsWithTraffic.get(road);
			if (rt == null) { //not in the driving roads
				if (rebuildRoads) {
					mRoadsWithTraffic.put(road, (new DrivingRoadWithTraffic(mMapUtils)).buildSegmentsFromAir(roadTraffics.get(i)));
				}
				continue;
			}
			//����matchRoute���������ã����Ƚ�SegmentTraffic������������μ������ƥ���
			rt.matchRoute(roadTraffics.get(i));
			//�ҳ����е�MatchedPoints
			ArrayList<GeoPoint> matchedPoints = rt.getMatchedPointsByList();
	        if (matchedPoints == null || matchedPoints.size() == 0) continue;
	        if (mMatchedPoints == null) mMatchedPoints = new ArrayList<GeoPoint>();
            mMatchedPoints.addAll(matchedPoints); 
		}
    }

    public List<Map<String, Object>> getAllRoadsWithTrafficByList() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = null;
        Log.d(TAG, "fetching data from internal container!"+mRoadsWithTraffic.toString());
        Iterator it = mRoadsWithTraffic.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<String, DrivingRoadWithTraffic> entry = (Entry<String, DrivingRoadWithTraffic>) it.next();
        	DrivingRoadWithTraffic rt = (DrivingRoadWithTraffic) entry.getValue();
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
	        	if (speed >= 20) strSpeed = Constants.TRAFFIC_JAM_LVL_MIDDLE;
	        	if (speed < 20 && speed >=10) strSpeed = Constants.TRAFFIC_JAM_LVL_LOW;
	        	String formatedStr = String.format("%d����ǰ��%s", interval, strSpeed);
	            map.put("timestamp", formatedStr);
	        	list.add(map);
        	}
        }
    	return list;
    }
    
    /*
     * �������ֻ�ڹ���ʱ����һ�Σ�����·��Ϊ��������������·��
     * ������һ��Map�����ڿ�������step��·
     */
    private void buildRoadsFromRoute() {
		String road = "";
    	for (int index = 0; index < mNumSteps; index++){
			//Log.d(TAG, "step index..." + index);
			MKStep step = mAllSteps.get(index);
			//Log.d(TAG, "step content..." + step.getContent());
			Scanner scanner = new Scanner(step.getContent());
			scanner.useDelimiter("\\s*-\\s*");
			String pattern = ".*����(.*)";
			if(scanner.hasNext(pattern)) {
				scanner.next(pattern);
				MatchResult match = scanner.match();
				road = match.group(1);
				DrivingRoadWithTraffic rt = null;
				if (mRoadsWithTraffic == null) {
					mRoadsWithTraffic = new HashMap<String, DrivingRoadWithTraffic>();
				} else {
					rt = mRoadsWithTraffic.get(road);
				}
				if (rt == null) {
					rt = new DrivingRoadWithTraffic(mMapUtils);
					mRoadsWithTraffic.put(road, rt);
				}
				//Baidu���ص�mAllPoints��Array of Array of GeoPoints
				if (index < mNumSteps-2) {
					rt.addPointsToRoute(mAllPoints.get(index));
				}
			} 
			if (road.equals("")) continue; //��û���ҵ���һ��·
			if (mStepsOfRoad == null) {
				mStepsOfRoad = new HashMap<Integer, String>();
			}
			mStepsOfRoad.put(index, road);
		}
    }
}

