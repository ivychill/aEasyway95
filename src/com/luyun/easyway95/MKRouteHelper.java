package com.luyun.easyway95;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
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

public class MKRouteHelper implements Serializable{
	private static final String TAG = "MKRouteHelper";
	
    private ArrayList<ArrayList<GeoPoint>> mAllPoints; //from driving route
    private ArrayList<GeoPoint> mAllPoints2; //one by one
    private ArrayList<GeoPoint> mMatchedPoints; //前方拥堵点集合，一个排序的列表
    
    private int mDistance;
    private GeoPoint mEnd;
    private int mIndex;
    private int mNumSteps;
    private int mRouteType;
    private GeoPoint mStart;
    private ArrayList<MKStep> mAllSteps;
    private MKRoute mRawRoute;
    
    //用Map保存"路及路况"
    private Map<String, RoadTrafficHelper> mRoadsWithTraffic;
	//private ArrayList<GeoPoint> matchedPoints;
    
	MKRouteHelper(MKRoute mkr) {
		mRawRoute = mkr;
		mAllPoints = mkr.getArrayPoints();
		mDistance = mkr.getDistance();
		mEnd = mkr.getEnd();
		mIndex = mkr.getIndex();
		mNumSteps = mkr.getNumSteps();
		mRouteType = mkr.getRouteType();
		mStart = mkr.getStart();
		mAllSteps = new ArrayList();
		for (int i=0; i<mNumSteps; i++) {
			mAllSteps.add(mkr.getStep(i));
		}
		mRoadsWithTraffic = new HashMap<String, RoadTrafficHelper>();
		buildRoadsFromRoute();
    }
    
	MKRoute getRawRoute() {
		return mRawRoute;
	}
	
    ArrayList<ArrayList<GeoPoint>> getAllPoints() {
    	return mAllPoints;
    }
    
    public Map<String, RoadTrafficHelper>  getRoadsWithTraffic() {
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
    
    public void onTraffic(LYTrafficPub trafficPub, boolean rebuildRoads) {
		List<LYRoadTraffic> roadTraffics = trafficPub.getCityTraffic().getRoadTrafficsList();
		if (mRoadsWithTraffic == null || rebuildRoads) {
			mRoadsWithTraffic = new HashMap<String, RoadTrafficHelper>();
			mMatchedPoints = new ArrayList<GeoPoint>();
		}
		for (int i=0; i<roadTraffics.size(); i++) {
			String road = roadTraffics.get(i).getRoad();
			//find by the road name
			
			RoadTrafficHelper rt = mRoadsWithTraffic.get(road);
			if (rt == null) { //not in the driving roads
				if (rebuildRoads) {
					mRoadsWithTraffic.put(road, (new RoadTrafficHelper()).build(roadTraffics.get(i)));
				}
				continue;
			}
			rt.build(roadTraffics.get(i));
			rt.matchRoute();
			//找出所有的MatchedPoints
	        ArrayList<GeoPoint> matchedPoints = rt.getMatchedPoints();
	        if (matchedPoints == null || matchedPoints.size() == 0) continue;
	        if (mMatchedPoints == null) mMatchedPoints = new ArrayList<GeoPoint>();
	        mMatchedPoints.addAll(matchedPoints);
		}
    }
    
    //不直接用proto生成的LYRoadTraffic的理由：1.这个用作容器，收纳LYRoadTraffic信息，需要比较方便的set/get，如果用LYRoadTraffic，会比较麻烦
    //另外，容器需要跟协议的耦合度降低，只在这个容器里处理不同的协议。
    //同时将原来协议传送的List换成Map的形式，便于程序处理和效率的考虑
    public class RoadTrafficHelper {
    	//private String road;
    	private Timestamp timestamp; //timestamp when last traffic received
    	private String href;
    	private String desc;
    	private ArrayList<LYSegmentTraffic> mSegmentTraffic;
    	private ArrayList<GeoPoint> mPointsOfRoute;
    	private ArrayList<GeoPoint> matchedPoints;
    	private boolean needClearSegment = false;
    	
    	RoadTrafficHelper() {
    		//road = road;
    	}
    	
    	String getDesc() {
    		return desc;
    	}
    	
    	long getTimestamp() {
    		return this.timestamp.getTime();
    	}
    	
    	ArrayList<LYSegmentTraffic> getSegments() {
    		if (needClearSegment == false) return mSegmentTraffic;
    		Iterator it=mSegmentTraffic.iterator();
    		while (it.hasNext()) {
    			LYSegmentTraffic st = (LYSegmentTraffic)it.next();
    			if (st == null) it.remove();
    		}
    		return mSegmentTraffic;
    	}
    	
    	void clearSegment(int i) {
    		if (i < 0 || i > mSegmentTraffic.size()-1) return; //do nothing
    		mSegmentTraffic.set(i, null);
    		needClearSegment = true;
    	}
    	
    	ArrayList<GeoPoint> getMatchedPoints() {
    		return matchedPoints;
    	}
    	
    	RoadTrafficHelper build(LYRoadTraffic rt) {
    		this.timestamp = new Timestamp(rt.getTimestamp());
    		this.href = new String(rt.getHref());
    		this.desc = new String(rt.getDesc());
    		this.mSegmentTraffic = new ArrayList<LYSegmentTraffic>(rt.getSegmentTrafficsList());
    		return this;
    	}
    	
    	void addPoints(ArrayList<GeoPoint> points) {
    		if (mPointsOfRoute == null) {
    			mPointsOfRoute = new ArrayList<GeoPoint>();
    		}
    		mPointsOfRoute.addAll(points);
    	}
    	
    	void matchRoute() {
    		ArrayList<GeoPoint> tmpMatchedPoints = new ArrayList();
    		matchedPoints = new ArrayList();
    		for (int i=0; i<mSegmentTraffic.size(); i++) {
    			MatchRoadAndTraffic(mSegmentTraffic.get(i).getSegment(), mPointsOfRoute, tmpMatchedPoints);
    			matchedPoints.addAll(tmpMatchedPoints);
    		}
    		Log.d(TAG, "matchedPoints="+matchedPoints.size()+","+matchedPoints.toString());
    	}
    }
    
    private void buildRoadsFromRoute() {
		String road = "";
    	for (int index = 0; index < mNumSteps; index++){
			Log.d(TAG, "step index..." + index);
			MKStep step = mAllSteps.get(index);
			Log.d(TAG, "step content..." + step.getContent());
			Scanner scanner = new Scanner(step.getContent());
			scanner.useDelimiter("\\s*-\\s*");
			String pattern = ".*进入(.*)";
			if(scanner.hasNext(pattern)) {
				scanner.next(pattern);
				MatchResult match = scanner.match();
				road = match.group(1);
				RoadTrafficHelper rt = null;
				if (mRoadsWithTraffic == null) {
					mRoadsWithTraffic = new HashMap<String, RoadTrafficHelper>();
				} else {
					rt = mRoadsWithTraffic.get(road);
				}
				if (rt == null) {
					rt = new RoadTrafficHelper();
					mRoadsWithTraffic.put(road, rt);
				}
				if (index < mNumSteps-2) {
					rt.addPoints(mAllPoints.get(index));
				}
			}
		}
    }
    //#Begin.....//路径拟合以及点和路径相关的判断算法
    //弧度计算
    private static double rad(double d)  
    {  
        return d * Math.PI / 180.0;  
    } 

    //返回两点之间的距离，单位为米
    public static double getDistance(Location loc1, Location loc2) {
    	return GetDistance(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }
    
    public static double GetDistance(double lat1, double lng1, double lat2, double lng2)  
    {  
    	double EARTH_RADIUS = 6378.137;
    	
        double radLat1 = rad(lat1);  
        double radLat2 = rad(lat2);  
        double a = radLat1 - radLat2;  
        double b = rad(lng1) - rad(lng2);  
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +   
            Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));  
        s = s * EARTH_RADIUS * 1000.0;  //单位:M
        s = Math.round(s * 10000) / 10000;  
        return s;  
    }  

    //返回两点之间的距离，输入是百度的微度 !~_~
    public static double MetersBetweenGeoPoints(GeoPoint geoPoint1, GeoPoint geoPoint2)  
    {  
    	double lat1 = geoPoint1.getLatitudeE6()/1000000.0;
    	double lng1 = geoPoint1.getLongitudeE6()/1000000.0;
    	double lat2 = geoPoint2.getLatitudeE6()/1000000.0;
    	double lng2 = geoPoint2.getLongitudeE6()/1000000.0;
    	return GetDistance(lat1, lng1, lat2, lng2);
    }  

    //这里定义一个直接使用经纬度的，是为了继承IOS的函数用法不用改动太大
    private class RTTGeoPoint
    {
    	public double lat;
    	public double lng;
    }

    //判断pnt是否在由（p1, p2）两点组成的线段范围内
    //方法：计算投影点，然后判断投影点是否在线段内；如果是，则返回距离，否则返回－1.0；
    //Note: 允许投影点在线段两端的误差，目前本函数还没加入这个误差；
    //retproj 必须在外面实例化，是返回的投影点
    public  double GetNearLineDistance(GeoPoint locPoint,  GeoPoint lineP1, GeoPoint lineP2, GeoPoint retproj)
    {
      double a;    
      double b;    
      double c;
      
      RTTGeoPoint pnt = new RTTGeoPoint();
      RTTGeoPoint p1 = new RTTGeoPoint();
      RTTGeoPoint p2 = new RTTGeoPoint();
      
      pnt.lat = locPoint.getLatitudeE6()/1000000.0;
      pnt.lng = locPoint.getLongitudeE6()/1000000.0;
      p1.lat = lineP1.getLatitudeE6()/1000000.0;
      p1.lng = lineP1.getLongitudeE6()/1000000.0;
      p2.lat = lineP2.getLatitudeE6()/1000000.0;
      p2.lng = lineP2.getLongitudeE6()/1000000.0;

      a = (p2.lat-p1.lat);
      b = p1.lng-p2.lng;
      c = p1.lat*p2.lng-p1.lng*p2.lat;    
           
      double dSPtX = (b*b*pnt.lng - a*(b*pnt.lat + c))/(a*a + b*b);
      double dSPtY = (a*a*pnt.lat - b*(a*pnt.lng + c))/(a*a + b*b);
      
      
      if (retproj != null)
      {
          retproj.setLatitudeE6((int)(dSPtY*1000000.0));
          retproj.setLongitudeE6((int)(dSPtX*1000000.0));
      }
      
      
      //投影点是否在线段内；之所以这么写是为了避免复杂浮点运算；
      if (p1.lng < p2.lng)//横坐标判断
      {
          if ((dSPtX < p1.lng) || (dSPtX > p2.lng)) //不在线段内，还没加入误差
          {
              return -1.0;
          }
      }
      else 
      {
          if ((dSPtX > p1.lng) || (dSPtX < p2.lng)) //不在线段内，还没加入误差
          {
              return -1.0;
          }
      }
      
      if (p1.lat < p2.lat) //纵坐标判断
      {
          if ((dSPtY < p1.lat) || (dSPtY > p2.lat)) //不在线段内，还没加入误差
          {
              return -1.0;
          }
      }
      else 
      {
          if ((dSPtY > p1.lat) || (dSPtY < p2.lat)) //不在线段内，还没加入误差
          {
              return -1.0;
          }
      }
      
      double distance = GetDistance(pnt.lat, pnt.lng, dSPtY, dSPtX);
      return distance;
    };

    //该结构用于在判断一个点和路径上的关系的时候，返回最短距离、投影点、以及投影点在路径点数组中对应Index等；
    public class STPointLineDistInfo
    {
	    private double distance;
	    private RTTGeoPoint projection;
	    private int pointindex;
		
	    public RTTGeoPoint getProjection() {
			return projection;
		}
		public void setProjection(RTTGeoPoint projection) {
			this.projection = projection;
		}
		public double getDistance() {
			return distance;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
		public int getPointindex() {
			return pointindex;
		}
		public void setPointindex(int pointindex) {
			this.pointindex = pointindex;
		}
    }
    
    //百度的微度到度之间的转换......
    private RTTGeoPoint GeoPoint2RttGeoPoint(GeoPoint point)
    {
    	RTTGeoPoint retPoint = new RTTGeoPoint();
    	retPoint.lat = point.getLatitudeE6()/1000000.0;
    	retPoint.lng = point.getLongitudeE6()/1000000.0;
    	return retPoint;
    }

    public static class GeoPointHelper {
    	public static GeoPoint buildGeoPoint(Location loc) {
    		return new GeoPoint((int)(loc.getLatitude()*1E6), (int)(loc.getLongitude()*1E6));
    	}
    }
    //该方法获取LocationPoint和路径（数组roadPoints给出）的最短距离；输入参数pointCount是数组的size（Java中其实可以不用，还没改）；
    //返回结构该结构用于在判断一个点和路径上的关系的时候，返回最短距离、投影点、以及投影点在路径点数组中对应Index等；必须在调用的函数中实例化
    //该Index特指：投影点在路径中的Index以及Index+1之间；
    public double getNearestDistanceOfRoad(GeoPoint LoctionPoint, java.util.ArrayList<GeoPoint> roadPoints, STPointLineDistInfo retPLDInfo)
    {
      int pointCount = roadPoints.size();
      if (pointCount < 2)
          return -1.0;
      
      double nearestDistance = MetersBetweenGeoPoints(LoctionPoint, roadPoints.get(0));
      if (null != retPLDInfo)
      {
          retPLDInfo.setProjection(GeoPoint2RttGeoPoint(roadPoints.get(0)));
          retPLDInfo.setPointindex(0);
      }
      
      GeoPoint projPoint = new GeoPoint(0, 0);
      for (int i=0; i<(pointCount-1); i++)
      {
          double dist = GetNearLineDistance(LoctionPoint, roadPoints.get(i), roadPoints.get(i+1), projPoint);
          if ((dist>=0.0) && (dist <= nearestDistance))
          {
              nearestDistance = dist;
              if (null != retPLDInfo)
              {
                  retPLDInfo.setPointindex(i);
                  retPLDInfo.setProjection(GeoPoint2RttGeoPoint(projPoint));
              }
          }
          dist = MetersBetweenGeoPoints(LoctionPoint, roadPoints.get(i+1)); //避免落在投影外的情况，譬如凸折现连接外的点
          if ((dist>=0.0) && (dist <= nearestDistance))
          {
              nearestDistance = dist;
              if (null != retPLDInfo)
              {
                  retPLDInfo.setPointindex(i);
                  retPLDInfo.setProjection(GeoPoint2RttGeoPoint(roadPoints.get(i+1)));
              }
          }

      }
      
      if (null != retPLDInfo)
      {
          retPLDInfo.setDistance(nearestDistance);
      }
      
      return nearestDistance;
    }
    
   //路径拟合判断；如果拥堵路段（segTraffic）在roadPoints组成的路径中，则返回true，否则返回false
   public Boolean MatchRoadAndTraffic(LYSegment trfSeg, java.util.ArrayList<GeoPoint> roadPoints, java.util.ArrayList<GeoPoint> retMatchedPoints)
   {
	   int roadPoincnt = roadPoints.size();
       GeoPoint minRectPoint = new GeoPoint(5000*1000000,5000*1000000);
       GeoPoint maxRectPoint = new GeoPoint(0,0);
       
       for (int i=0; i < roadPoincnt; i++)
       {
           if (roadPoints.get(i).getLatitudeE6() < minRectPoint.getLatitudeE6())
           {
               minRectPoint.setLatitudeE6(roadPoints.get(i).getLatitudeE6());
           }
           if (roadPoints.get(i).getLongitudeE6() < minRectPoint.getLongitudeE6())
           {
               minRectPoint.setLongitudeE6(roadPoints.get(i).getLongitudeE6());
           }
           
           if (roadPoints.get(i).getLatitudeE6() > maxRectPoint.getLatitudeE6())
           {
               maxRectPoint.setLatitudeE6(roadPoints.get(i).getLatitudeE6());
           }
           if (roadPoints.get(i).getLongitudeE6() > maxRectPoint.getLongitudeE6())
           {
               maxRectPoint.setLongitudeE6(roadPoints.get(i).getLongitudeE6());
           }
       }
       
       //判断矩形是否重合
       GeoPoint trafficRectPoint1 = new GeoPoint(0,0);
       GeoPoint trafficRectPoint2 = new GeoPoint(0,0);
       trafficRectPoint1.setLatitudeE6((int) (trfSeg.getStart().getLat()*1000000));
       trafficRectPoint1.setLongitudeE6((int) (trfSeg.getStart().getLng()*1000000));
       trafficRectPoint2.setLatitudeE6((int) (trfSeg.getEnd().getLat()*1000000));
       trafficRectPoint2.setLongitudeE6((int) (trfSeg.getEnd().getLng()*1000000));
       
       
       GeoPoint minTrfRect = new GeoPoint(0,0);
       GeoPoint maxTrfRect = new GeoPoint(0,0);
       if (trafficRectPoint1.getLatitudeE6() < trafficRectPoint2.getLatitudeE6())
       {
    	   minTrfRect.setLatitudeE6(trafficRectPoint1.getLatitudeE6());
    	   maxTrfRect.setLatitudeE6(trafficRectPoint2.getLatitudeE6());
       }
       else
       {
    	   minTrfRect.setLatitudeE6(trafficRectPoint2.getLatitudeE6());
    	   maxTrfRect.setLatitudeE6(trafficRectPoint1.getLatitudeE6());
       }
       
       if (trafficRectPoint1.getLongitudeE6() < trafficRectPoint2.getLongitudeE6())
       {
    	   minTrfRect.setLongitudeE6(trafficRectPoint1.getLongitudeE6());
    	   maxTrfRect.setLongitudeE6(trafficRectPoint2.getLongitudeE6());
       }
       else
       {
    	   minTrfRect.setLongitudeE6(trafficRectPoint2.getLongitudeE6());
    	   maxTrfRect.setLongitudeE6(trafficRectPoint1.getLongitudeE6());
       }
       
       
       GeoPoint CommRectP1 = new GeoPoint(0,0);
       GeoPoint CommRectP2 = new GeoPoint(0,0);
       
       if ( (maxRectPoint.getLatitudeE6() < minTrfRect.getLatitudeE6()) || (minRectPoint.getLatitudeE6() > maxTrfRect.getLatitudeE6()) 
    		  || (maxRectPoint.getLongitudeE6() < minTrfRect.getLongitudeE6()) || (minRectPoint.getLongitudeE6() > maxTrfRect.getLongitudeE6()) )
       {
//           Log.d(TAG, "没有拟合的矩形");
//           String strrectinfo = String.format("RectRd P1=%f,%f, P2=%f,%f; Traffic P1=%f,%f, P1=%f,%f", 
//        		   minRectPoint.getLatitudeE6()/1000000.0, minRectPoint.getLongitudeE6()/1000000.0, 
//        		   maxRectPoint.getLatitudeE6()/1000000.0, maxRectPoint.getLongitudeE6()/1000000.0,
//        		   trafficRectPoint1.getLatitudeE6()/1000000.0, trafficRectPoint1.getLongitudeE6()/1000000.0,
//        		   trafficRectPoint2.getLatitudeE6()/1000000.0, trafficRectPoint2.getLongitudeE6()/1000000.0);
//           Log.d(TAG,strrectinfo);
           return false; 
       }
       else
       {   
    	   //上面已经排除非重合的情况，这里先对两个矩形的XY分别进行排序，然后取中间区间作为重合（交集）区域
    	   int[] lat = new int[4];
    	   int[] lng = new int[4];
    	
    	   lat[0] = minTrfRect.getLatitudeE6();
    	   lat[1] = maxTrfRect.getLatitudeE6();
    	   lat[2] = minRectPoint.getLatitudeE6();
    	   lat[3] = maxRectPoint.getLatitudeE6();
    	   
    	   lng[0] = minTrfRect.getLongitudeE6();
    	   lng[1] = maxTrfRect.getLongitudeE6();
    	   lng[2] = minRectPoint.getLongitudeE6();
    	   lng[3] = maxRectPoint.getLongitudeE6();
    	   
    	   Arrays.sort(lat);
    	   Arrays.sort(lng);
    	   
    	   CommRectP1.setLatitudeE6(lat[1]);
    	   CommRectP1.setLongitudeE6(lng[1]);
    	   CommRectP2.setLatitudeE6(lat[2]);
    	   CommRectP2.setLongitudeE6(lng[2]);
    	   
//    	   String strLog = String.format("拟合矩形 P1=%f,%f; P2=%f,%f", 
//    			   CommRectP1.getLongitudeE6()/1000000.0, CommRectP1.getLatitudeE6()/1000000.0,
//    			   CommRectP2.getLongitudeE6()/1000000.0, CommRectP2.getLatitudeE6()/1000000.0);
//    	   Log.d(TAG,strLog);

    	   GeoPoint comPoint1 = new GeoPoint(0,0);
    	   GeoPoint comPoint2 = new GeoPoint(0,0);
    	   
           
           double slope =  (trafficRectPoint2.getLatitudeE6() - trafficRectPoint1.getLatitudeE6())/(trafficRectPoint2.getLongitudeE6() - trafficRectPoint1.getLongitudeE6());
           
           if (slope > 0.0) //正的斜率，取交集矩形最靠近坐标(0,0)的点和对角点; 地图坐标轴是以左上角为原点; 注意经纬度和直角坐标的区别;
           {
               if (trafficRectPoint2.getLatitudeE6() > trafficRectPoint1.getLatitudeE6())
               {
                   comPoint1 = CommRectP1;
                   comPoint2 = CommRectP2;
               }
               else 
               {
                   comPoint1 = CommRectP2;
                   comPoint2 = CommRectP1;
               }
               
           }
           else 
           {
               if (trafficRectPoint2.getLatitudeE6() < trafficRectPoint1.getLatitudeE6())
               {
            	   //左上角
            	   comPoint1.setLatitudeE6(CommRectP2.getLatitudeE6());
            	   comPoint1.setLongitudeE6(CommRectP1.getLongitudeE6());
            	   
            	   //右下角
            	   comPoint2.setLatitudeE6(CommRectP1.getLatitudeE6());
            	   comPoint2.setLongitudeE6(CommRectP2.getLongitudeE6());
               }
               else 
               {
            	   //右下角
            	   comPoint1.setLatitudeE6(CommRectP1.getLatitudeE6());
            	   comPoint1.setLongitudeE6(CommRectP2.getLongitudeE6());
            	   
            	   //左上角
            	   comPoint2.setLatitudeE6(CommRectP2.getLatitudeE6());
            	   comPoint2.setLongitudeE6(CommRectP1.getLongitudeE6());
               }
           }
           
           double cmbRange = MetersBetweenGeoPoints(comPoint1, comPoint2);
           if (cmbRange < 50.0) //避免转弯时路口坐标偏差导致的小段拥堵误报
           {
//        	   strLog = String.format("距离太短，丢弃；矩形距离=%f\r\n P1=%f,%f; P2=%f,%f", 
//        			   cmbRange,
//        			   comPoint1.getLongitudeE6()/1000000.0, comPoint1.getLatitudeE6()/1000000.0,
//        			   comPoint2.getLongitudeE6()/1000000.0, comPoint2.getLatitudeE6()/1000000.0);
//        	   Log.d(TAG,strLog);
               return false;
           }
           else
           {
        	   
//        	   strLog = String.format("方向判断，矩形 P1=%f,%f; P2=%f,%f", 
//        			   comPoint1.getLongitudeE6()/1000000.0, comPoint1.getLatitudeE6()/1000000.0,
//        			   comPoint2.getLongitudeE6()/1000000.0, comPoint2.getLatitudeE6()/1000000.0);
//        	   Log.d(TAG,strLog);
        	   
		        //判断方向
		        STPointLineDistInfo stPLDinfoC1 = new STPointLineDistInfo();
		        double distCP1 = getNearestDistanceOfRoad(comPoint1, roadPoints, stPLDinfoC1);
		         
		        STPointLineDistInfo stPLDinfoC2 = new STPointLineDistInfo();
		        double distCP2 = getNearestDistanceOfRoad(comPoint2, roadPoints, stPLDinfoC2);
		         		         
		        if ((distCP1 >= 0.0 && distCP1 <= 100.0) && (distCP2 >= 0.0 && distCP2 <= 100.0))
		        {
		            if (stPLDinfoC1.pointindex > stPLDinfoC2.pointindex) 
		            {
//		            	Log.d(TAG,"方向相反");
//			        	strLog = String.format("CP1, IDX=%d, Dist=%f; CP1, IDX=%d, Dist=%f", stPLDinfoC1.pointindex, distCP1, stPLDinfoC2.pointindex, distCP2);
//			        	Log.d(TAG,strLog);
		            	return false;
		            }
		            
		            //正确拟合，添加拟合路段的坐标点（有序）
		            GeoPoint firstPrjPoint = new GeoPoint(0,0);
		            GeoPoint endPrjPoint = new GeoPoint(0,0);
		            firstPrjPoint.setLatitudeE6((int) (stPLDinfoC1.getProjection().lat*1E6));
		            endPrjPoint.setLatitudeE6((int) (stPLDinfoC2.getProjection().lat*1E6));
		            firstPrjPoint.setLongitudeE6((int) (stPLDinfoC1.getProjection().lng*1E6));
		            endPrjPoint.setLongitudeE6((int) (stPLDinfoC2.getProjection().lng*1E6));
		            
		            retMatchedPoints.add(firstPrjPoint);
		            for (int j=stPLDinfoC1.pointindex; j < stPLDinfoC2.pointindex; j++)
		            {
		            	retMatchedPoints.add(roadPoints.get(j+1));
		            }
		            retMatchedPoints.add(endPrjPoint);

		        }
		        else
		        {
//		            strLog = String.format("拟合距离中拥堵点到路径的距离太大，丢弃；距离 P1=%f  P2=%f", distCP1, distCP2); 
//		        	Log.d(TAG,strLog);
//		        	strLog = String.format("CP1, IDX=%d, Dist=%f; CP1, IDX=%d, Dist=%f", stPLDinfoC1.pointindex, distCP1, stPLDinfoC2.pointindex, distCP2);
//		        	Log.d(TAG,strLog);
		        	return false;
		        }
           }
           
       }
       return true;
   }
 //#End.....//路径拟合以及点和路径相关的判断算法
}

