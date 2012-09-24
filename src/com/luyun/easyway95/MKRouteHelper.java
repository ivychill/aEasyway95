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
    private ArrayList<GeoPoint> mMatchedPoints; //ǰ��ӵ�µ㼯�ϣ�һ��������б�
    
    private int mDistance;
    private GeoPoint mEnd;
    private int mIndex;
    private int mNumSteps;
    private int mRouteType;
    private GeoPoint mStart;
    private ArrayList<MKStep> mAllSteps;
    
    //��Map����"·��·��"
    private Map<String, RoadTrafficHelper> mRoadsWithTraffic;
	//private ArrayList<GeoPoint> matchedPoints;
    
	MKRouteHelper(MKRoute mkr) {
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
			//�ҳ����е�MatchedPoints
	        ArrayList<GeoPoint> matchedPoints = rt.getMatchedPoints();
	        if (matchedPoints == null || matchedPoints.size() == 0) continue;
	        if (mMatchedPoints == null) mMatchedPoints = new ArrayList<GeoPoint>();
	        mMatchedPoints.addAll(matchedPoints);
		}
    }
    
    //��ֱ����proto���ɵ�LYRoadTraffic�����ɣ�1.�����������������LYRoadTraffic��Ϣ����Ҫ�ȽϷ����set/get�������LYRoadTraffic����Ƚ��鷳
    //���⣬������Ҫ��Э�����϶Ƚ��ͣ�ֻ����������ﴦ��ͬ��Э�顣
    //ͬʱ��ԭ��Э�鴫�͵�List����Map����ʽ�����ڳ������Ч�ʵĿ���
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
			String pattern = ".*����(.*)";
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
    //#Begin.....//·������Լ����·����ص��ж��㷨
    //���ȼ���
    private static double rad(double d)  
    {  
        return d * Math.PI / 180.0;  
    } 

    //��������֮��ľ��룬��λΪ��
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
        s = s * EARTH_RADIUS * 1000.0;  //��λ:M
        s = Math.round(s * 10000) / 10000;  
        return s;  
    }  

    //��������֮��ľ��룬�����ǰٶȵ�΢�� !~_~
    public static double MetersBetweenGeoPoints(GeoPoint geoPoint1, GeoPoint geoPoint2)  
    {  
    	double lat1 = geoPoint1.getLatitudeE6()/1000000.0;
    	double lng1 = geoPoint1.getLongitudeE6()/1000000.0;
    	double lat2 = geoPoint2.getLatitudeE6()/1000000.0;
    	double lng2 = geoPoint2.getLongitudeE6()/1000000.0;
    	return GetDistance(lat1, lng1, lat2, lng2);
    }  

    //���ﶨ��һ��ֱ��ʹ�þ�γ�ȵģ���Ϊ�˼̳�IOS�ĺ����÷����øĶ�̫��
    private class RTTGeoPoint
    {
    	public double lat;
    	public double lng;
    }

    //�ж�pnt�Ƿ����ɣ�p1, p2��������ɵ��߶η�Χ��
    //����������ͶӰ�㣬Ȼ���ж�ͶӰ���Ƿ����߶��ڣ�����ǣ��򷵻ؾ��룬���򷵻أ�1.0��
    //Note: ����ͶӰ�����߶����˵���Ŀǰ��������û���������
    //retproj ����������ʵ�������Ƿ��ص�ͶӰ��
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
      
      
      //ͶӰ���Ƿ����߶��ڣ�֮������ôд��Ϊ�˱��⸴�Ӹ������㣻
      if (p1.lng < p2.lng)//�������ж�
      {
          if ((dSPtX < p1.lng) || (dSPtX > p2.lng)) //�����߶��ڣ���û�������
          {
              return -1.0;
          }
      }
      else 
      {
          if ((dSPtX > p1.lng) || (dSPtX < p2.lng)) //�����߶��ڣ���û�������
          {
              return -1.0;
          }
      }
      
      if (p1.lat < p2.lat) //�������ж�
      {
          if ((dSPtY < p1.lat) || (dSPtY > p2.lat)) //�����߶��ڣ���û�������
          {
              return -1.0;
          }
      }
      else 
      {
          if ((dSPtY > p1.lat) || (dSPtY < p2.lat)) //�����߶��ڣ���û�������
          {
              return -1.0;
          }
      }
      
      double distance = GetDistance(pnt.lat, pnt.lng, dSPtY, dSPtX);
      return distance;
    };

    //�ýṹ�������ж�һ�����·���ϵĹ�ϵ��ʱ�򣬷�����̾��롢ͶӰ�㡢�Լ�ͶӰ����·���������ж�ӦIndex�ȣ�
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
    
    //�ٶȵ�΢�ȵ���֮���ת��......
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
    //�÷�����ȡLocationPoint��·��������roadPoints����������̾��룻�������pointCount�������size��Java����ʵ���Բ��ã���û�ģ���
    //���ؽṹ�ýṹ�������ж�һ�����·���ϵĹ�ϵ��ʱ�򣬷�����̾��롢ͶӰ�㡢�Լ�ͶӰ����·���������ж�ӦIndex�ȣ������ڵ��õĺ�����ʵ����
    //��Index��ָ��ͶӰ����·���е�Index�Լ�Index+1֮�䣻
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
          dist = MetersBetweenGeoPoints(LoctionPoint, roadPoints.get(i+1)); //��������ͶӰ��������Ʃ��͹����������ĵ�
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
    
   //·������жϣ����ӵ��·�Σ�segTraffic����roadPoints��ɵ�·���У��򷵻�true�����򷵻�false
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
       
       //�жϾ����Ƿ��غ�
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
//           Log.d(TAG, "û����ϵľ���");
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
    	   //�����Ѿ��ų����غϵ�����������ȶ��������ε�XY�ֱ��������Ȼ��ȡ�м�������Ϊ�غϣ�����������
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
    	   
//    	   String strLog = String.format("��Ͼ��� P1=%f,%f; P2=%f,%f", 
//    			   CommRectP1.getLongitudeE6()/1000000.0, CommRectP1.getLatitudeE6()/1000000.0,
//    			   CommRectP2.getLongitudeE6()/1000000.0, CommRectP2.getLatitudeE6()/1000000.0);
//    	   Log.d(TAG,strLog);

    	   GeoPoint comPoint1 = new GeoPoint(0,0);
    	   GeoPoint comPoint2 = new GeoPoint(0,0);
    	   
           
           double slope =  (trafficRectPoint2.getLatitudeE6() - trafficRectPoint1.getLatitudeE6())/(trafficRectPoint2.getLongitudeE6() - trafficRectPoint1.getLongitudeE6());
           
           if (slope > 0.0) //����б�ʣ�ȡ���������������(0,0)�ĵ�ͶԽǵ�; ��ͼ�������������Ͻ�Ϊԭ��; ע�⾭γ�Ⱥ�ֱ�����������;
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
            	   //���Ͻ�
            	   comPoint1.setLatitudeE6(CommRectP2.getLatitudeE6());
            	   comPoint1.setLongitudeE6(CommRectP1.getLongitudeE6());
            	   
            	   //���½�
            	   comPoint2.setLatitudeE6(CommRectP1.getLatitudeE6());
            	   comPoint2.setLongitudeE6(CommRectP2.getLongitudeE6());
               }
               else 
               {
            	   //���½�
            	   comPoint1.setLatitudeE6(CommRectP1.getLatitudeE6());
            	   comPoint1.setLongitudeE6(CommRectP2.getLongitudeE6());
            	   
            	   //���Ͻ�
            	   comPoint2.setLatitudeE6(CommRectP2.getLatitudeE6());
            	   comPoint2.setLongitudeE6(CommRectP1.getLongitudeE6());
               }
           }
           
           double cmbRange = MetersBetweenGeoPoints(comPoint1, comPoint2);
           if (cmbRange < 50.0) //����ת��ʱ·������ƫ��µ�С��ӵ����
           {
//        	   strLog = String.format("����̫�̣����������ξ���=%f\r\n P1=%f,%f; P2=%f,%f", 
//        			   cmbRange,
//        			   comPoint1.getLongitudeE6()/1000000.0, comPoint1.getLatitudeE6()/1000000.0,
//        			   comPoint2.getLongitudeE6()/1000000.0, comPoint2.getLatitudeE6()/1000000.0);
//        	   Log.d(TAG,strLog);
               return false;
           }
           else
           {
        	   
//        	   strLog = String.format("�����жϣ����� P1=%f,%f; P2=%f,%f", 
//        			   comPoint1.getLongitudeE6()/1000000.0, comPoint1.getLatitudeE6()/1000000.0,
//        			   comPoint2.getLongitudeE6()/1000000.0, comPoint2.getLatitudeE6()/1000000.0);
//        	   Log.d(TAG,strLog);
        	   
		        //�жϷ���
		        STPointLineDistInfo stPLDinfoC1 = new STPointLineDistInfo();
		        double distCP1 = getNearestDistanceOfRoad(comPoint1, roadPoints, stPLDinfoC1);
		         
		        STPointLineDistInfo stPLDinfoC2 = new STPointLineDistInfo();
		        double distCP2 = getNearestDistanceOfRoad(comPoint2, roadPoints, stPLDinfoC2);
		         		         
		        if ((distCP1 >= 0.0 && distCP1 <= 100.0) && (distCP2 >= 0.0 && distCP2 <= 100.0))
		        {
		            if (stPLDinfoC1.pointindex > stPLDinfoC2.pointindex) 
		            {
//		            	Log.d(TAG,"�����෴");
//			        	strLog = String.format("CP1, IDX=%d, Dist=%f; CP1, IDX=%d, Dist=%f", stPLDinfoC1.pointindex, distCP1, stPLDinfoC2.pointindex, distCP2);
//			        	Log.d(TAG,strLog);
		            	return false;
		            }
		            
		            //��ȷ��ϣ�������·�ε�����㣨����
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
//		            strLog = String.format("��Ͼ�����ӵ�µ㵽·���ľ���̫�󣬶��������� P1=%f  P2=%f", distCP1, distCP2); 
//		        	Log.d(TAG,strLog);
//		        	strLog = String.format("CP1, IDX=%d, Dist=%f; CP1, IDX=%d, Dist=%f", stPLDinfoC1.pointindex, distCP1, stPLDinfoC2.pointindex, distCP2);
//		        	Log.d(TAG,strLog);
		        	return false;
		        }
           }
           
       }
       return true;
   }
 //#End.....//·������Լ����·����ص��ж��㷨
}

