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
 * MKRouteHelper是一个容器，从Baidu的MKRoute中生成，为每条路创建一个DrivingRoadWithTraffic
 */
public class MKRouteHelper implements Serializable{
	private static final String TAG = "MKRouteHelper";
	private MapUtils mMapUtils = null;
	
    private ArrayList<ArrayList<GeoPoint>> mAllPoints; //from driving route
    private ArrayList<GeoPoint> mAllPoints2; //one by one
    private ArrayList<GeoPoint> mMatchedPoints; //前方拥堵点集合，一个排序的列表
    
    private int mDistance;
    private GeoPoint mStart;
    private GeoPoint mEnd;
    private int mIndex;
    private int mNumSteps;
    private int mRouteType;
    private ArrayList<MKStep> mAllSteps;
    private int stepCursor = 0; //避免每次全部扫描所有的路
    private MKRoute mRawRoute;
    
    //用Map保存"路及路况"
    private Map<String, DrivingRoadWithTraffic> mRoadsWithTraffic;
    //另外一个Map保存Step index和Road对应关系，便于快速查找该Step和Road的对应关系
    //之所以不在DrivingRoadWithTraffic里保存Step的列表，理由是显然的，我们关键是需要快速从step index中找到路
    //还有一种思路是，创建一个MKStepHelper，专门增加一个成员Road，这种方法有点重
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
     * 根据stepCursor所对应的step，找出路名，然后找出该路所包括的matchPoint
     * 如果step.getContent()包括”进入“，表示是从一条路切换到另一条路
     */
    public TrafficPoint getNextTrafficPoint(GeoPoint currentPoint) {
    	Log.d(TAG, "in MKRouteHelper::getTrafficPoint, stepCursor="+stepCursor);
    	if (stepCursor < 0 || mMatchedPoints == null || mMatchedPoints.size() == 0) {
    		return null;
    	}
    	String roadName = null;
    	//找前方的路上匹配最近的点
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
     * 根据stepCursor所对应的step，找出路名，然后找出该路所包括的matchPoint
     * 如果step.getContent()包括”进入“，表示是从一条路切换到另一条路
     */
    public ArrayList<TrafficPoint> getAllTrafficPointsAhead(GeoPoint currentPoint) {
    	Log.d(TAG, "in MKRouteHelper::getAllTrafficPointsAhead, stepCursor="+stepCursor);
    	if (stepCursor < 0 || mMatchedPoints == null || mMatchedPoints.size() == 0) {
    		return null;
    	}
    	String roadName = null;
    	ArrayList<TrafficPoint> allPointsAhead = new ArrayList<TrafficPoint>();
    	//找前方的路上匹配最近的点
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
     * 根据stepCursor，寻找当前、后面、前面三个step的points去匹配，同时更新stepCursor
     * 如果不匹配，则从头循环step找出匹配点，同时更新该cursor
     * 从前方的step中找出路名，然后从路里找出matchedPoints
     */
    public boolean isOnRoute(GeoPoint currentPoint) {
    	int i = 0;
    	int startIndex = stepCursor>2?stepCursor-3:0;
    	int endIndex = stepCursor+3<mAllPoints.size()?stepCursor+3:mAllPoints.size();
    	//首先找最可能的next 3个step
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
    	//其次往回找最可能的previous 3个step
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
    	//不能快速匹配则从头循环查找，是否可以取getAllPoints2()，找出当前点投影在哪一段，然后再计算出stepCursor，不过不一定减少运算量。
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
    	stepCursor = -1; //不在本DrivingRoute上
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
		//首先通过名字匹配，找到相应的路，然后在路上的折线点mPointsOfRoute中与RoadTraffic进行匹配
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
			//调用matchRoute有两个作用：首先将SegmentTraffic保存起来，其次计算出来匹配点
			rt.matchRoute(roadTraffics.get(i));
			//找出所有的MatchedPoints
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
	        	String formatedStr = String.format("%d分钟前，%s", interval, strSpeed);
	            map.put("timestamp", formatedStr);
	        	list.add(map);
        	}
        }
    	return list;
    }
    
    /*
     * 这个函数只在构造时调用一次，创建路作为容器，收纳所有路况
     * 并创建一个Map，用于快速索引step到路
     */
    private void buildRoadsFromRoute() {
		String road = "";
    	for (int index = 0; index < mNumSteps; index++){
			//Log.d(TAG, "step index..." + index);
			MKStep step = mAllSteps.get(index);
			//Log.d(TAG, "step content..." + step.getContent());
			Scanner scanner = new Scanner(step.getContent());
			scanner.useDelimiter("\\s*-\\s*");
			String pattern = ".*进入(.*)";
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
				//Baidu返回的mAllPoints是Array of Array of GeoPoints
				if (index < mNumSteps-2) {
					rt.addPointsToRoute(mAllPoints.get(index));
				}
			} 
			if (road.equals("")) continue; //还没有找到第一条路
			if (mStepsOfRoad == null) {
				mStepsOfRoad = new HashMap<Integer, String>();
			}
			mStepsOfRoad.put(index, road);
		}
    }
}

