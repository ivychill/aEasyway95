package com.luyun.easyway95;

import java.util.Arrays;

import android.location.Location;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;

public class MapUtils {
    //#Begin.....//·������Լ����·����ص��ж��㷨
    //���ȼ���
    private static double rad(double d)  
    {  
        return d * Math.PI / 180.0;  
    } 

    //��������֮��ľ��룬��λΪ��
    public static double getDistance(Location loc1, Location loc2) {
    	return GetDistance(loc1.getLatitude(), loc1.getLongitude(), 
    			loc2.getLatitude(), loc2.getLongitude());
    }
    
    //��������֮��ľ��룬��λΪ��
    public static double getDistance(Location loc1, GeoPoint p2) {
    	return GetDistance(loc1.getLatitude(), loc1.getLongitude(), p2.getLatitudeE6()/Constants.DOUBLE_1E6, 
    			p2.getLongitudeE6()/Constants.DOUBLE_1E6);
    }
    
    //��������֮��ľ��룬��λΪ��
    public static double getDistance(GeoPoint p1, Location loc2) {
    	return GetDistance(p1.getLatitudeE6()/Constants.DOUBLE_1E6, p1.getLongitudeE6()/Constants.DOUBLE_1E6, 
    			loc2.getLatitude(), loc2.getLongitude());
    }
    
    //��������֮��ľ��룬��λΪ��
    public static double getDistance(GeoPoint p1, GeoPoint p2) {
    	return GetDistance(p1.getLatitudeE6()/Constants.DOUBLE_1E6, p1.getLongitudeE6()/Constants.DOUBLE_1E6, 
    			p2.getLatitudeE6()/Constants.DOUBLE_1E6, p2.getLongitudeE6()/Constants.DOUBLE_1E6);
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
    	double lat1 = geoPoint1.getLatitudeE6()/Constants.DOUBLE_1E6;
    	double lng1 = geoPoint1.getLongitudeE6()/Constants.DOUBLE_1E6;
    	double lat2 = geoPoint2.getLatitudeE6()/Constants.DOUBLE_1E6;
    	double lng2 = geoPoint2.getLongitudeE6()/Constants.DOUBLE_1E6;
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
    public double GetNearLineDistance(GeoPoint locPoint,  GeoPoint lineP1, GeoPoint lineP2, GeoPoint retproj)
    {
      double a;    
      double b;    
      double c;
      
      RTTGeoPoint pnt = new RTTGeoPoint();
      RTTGeoPoint p1 = new RTTGeoPoint();
      RTTGeoPoint p2 = new RTTGeoPoint();
      
      pnt.lat = locPoint.getLatitudeE6()/Constants.DOUBLE_1E6;
      pnt.lng = locPoint.getLongitudeE6()/Constants.DOUBLE_1E6;
      p1.lat = lineP1.getLatitudeE6()/Constants.DOUBLE_1E6;
      p1.lng = lineP1.getLongitudeE6()/Constants.DOUBLE_1E6;
      p2.lat = lineP2.getLatitudeE6()/Constants.DOUBLE_1E6;
      p2.lng = lineP2.getLongitudeE6()/Constants.DOUBLE_1E6;

      a = (p2.lat-p1.lat);
      b = p1.lng-p2.lng;
      c = p1.lat*p2.lng-p1.lng*p2.lat;    
           
      double dSPtX = (b*b*pnt.lng - a*(b*pnt.lat + c))/(a*a + b*b);
      double dSPtY = (a*a*pnt.lat - b*(a*pnt.lng + c))/(a*a + b*b);
      
      
      if (retproj != null)
      {
          retproj.setLatitudeE6((int)(dSPtY*Constants.DOUBLE_1E6));
          retproj.setLongitudeE6((int)(dSPtX*Constants.DOUBLE_1E6));
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
		public GeoPoint getPoint() {
			GeoPoint pt = new GeoPoint((int)(projection.lat*1E6), (int)(projection.lng*1E6));
			return pt;
		}
    }
    
    //�ٶȵ�΢�ȵ���֮���ת��......
    private RTTGeoPoint GeoPoint2RttGeoPoint(GeoPoint point)
    {
    	RTTGeoPoint retPoint = new RTTGeoPoint();
    	retPoint.lat = point.getLatitudeE6()/Constants.DOUBLE_1E6;
    	retPoint.lng = point.getLongitudeE6()/Constants.DOUBLE_1E6;
    	return retPoint;
    }

    public static class GeoPointHelper {
    	private GeoPoint point;
	    private double distance;
	    private int pointindex;
		
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
    	GeoPointHelper(GeoPoint pt) {
    		point = pt;
    	}
    	public GeoPoint getPoint() {
    		return point;
    	}
    	
    	public static GeoPoint buildGeoPoint(Location loc) {
    		return new GeoPoint((int)(loc.getLatitude()*1E6), (int)(loc.getLongitude()*1E6));
    	}
    }
    
    /*
     * ����GeoPointHelper
     * 2012.09.26 ���������
     */
    public GeoPointHelper findClosestPoint(GeoPoint pt, java.util.ArrayList<GeoPoint> listPoints) {
    	GeoPointHelper pointHelper = null;
    	for (int i=0; i<listPoints.size(); i++) {
    		GeoPointHelper tmpPointHelper = new GeoPointHelper(listPoints.get(i));
    		double distance = getDistance(pt, listPoints.get(i));
    		tmpPointHelper.setDistance(distance);
    		tmpPointHelper.setPointindex(i);
    		if (pointHelper == null || tmpPointHelper.getDistance() < pointHelper.getDistance()) {
    			pointHelper = tmpPointHelper;
    		}
    	}
    	return pointHelper;
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
       
       //YSH_MODIFIED 2012-10-09 11:00
       //Ϊ�˱���·����ӵ���߶ζ��ǵ�ͼ�ϵ�ƽ�л��ߴ�ֱ��ʱ��΢С��ƫ������������ȷ��ϵ������������Ϊ����ӵ��·�ε�΢Сƫ��
       if ( (maxTrfRect.getLatitudeE6()-minTrfRect.getLatitudeE6()) <= 200)
       {
    	   int tmpCoor = minTrfRect.getLatitudeE6();
    	   minTrfRect.setLatitudeE6(tmpCoor-100);
    	   tmpCoor = maxTrfRect.getLatitudeE6();
    	   maxTrfRect.setLatitudeE6(tmpCoor+100);
       }
       if ( (maxTrfRect.getLongitudeE6()-minTrfRect.getLongitudeE6()) <= 200)
       {
    	   int tmpCoor = minTrfRect.getLongitudeE6();
    	   minTrfRect.setLongitudeE6(tmpCoor-100);
    	   tmpCoor = maxTrfRect.getLongitudeE6();
    	   maxTrfRect.setLongitudeE6(tmpCoor+100);
       }
       
       GeoPoint CommRectP1 = new GeoPoint(0,0);
       GeoPoint CommRectP2 = new GeoPoint(0,0);
       
       if ( (maxRectPoint.getLatitudeE6() < minTrfRect.getLatitudeE6()) || (minRectPoint.getLatitudeE6() > maxTrfRect.getLatitudeE6()) 
    		  || (maxRectPoint.getLongitudeE6() < minTrfRect.getLongitudeE6()) || (minRectPoint.getLongitudeE6() > maxTrfRect.getLongitudeE6()) )
       {
//           Log.d(TAG, "û����ϵľ���");
//           String strrectinfo = String.format("RectRd P1=%f,%f, P2=%f,%f; Traffic P1=%f,%f, P1=%f,%f", 
//        		   minRectPoint.getLatitudeE6()/Constants.DOUBLE_1E6, minRectPoint.getLongitudeE6()/Constants.DOUBLE_1E6, 
//        		   maxRectPoint.getLatitudeE6()/Constants.DOUBLE_1E6, maxRectPoint.getLongitudeE6()/Constants.DOUBLE_1E6,
//        		   trafficRectPoint1.getLatitudeE6()/Constants.DOUBLE_1E6, trafficRectPoint1.getLongitudeE6()/Constants.DOUBLE_1E6,
//        		   trafficRectPoint2.getLatitudeE6()/Constants.DOUBLE_1E6, trafficRectPoint2.getLongitudeE6()/Constants.DOUBLE_1E6);
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
//    			   CommRectP1.getLongitudeE6()/Constants.DOUBLE_1E6, CommRectP1.getLatitudeE6()/Constants.DOUBLE_1E6,
//    			   CommRectP2.getLongitudeE6()/Constants.DOUBLE_1E6, CommRectP2.getLatitudeE6()/Constants.DOUBLE_1E6);
//    	   Log.d(TAG,strLog);

    	   GeoPoint comPoint1 = new GeoPoint(0,0);
    	   GeoPoint comPoint2 = new GeoPoint(0,0);
    	   
           
      	 //YSH_MODIFIED 2012-10-09 11:00
           //����ǿ������ת��
           double slope =  ((double)(trafficRectPoint2.getLatitudeE6() - trafficRectPoint1.getLatitudeE6()))
        		   /((double)(trafficRectPoint2.getLongitudeE6() - trafficRectPoint1.getLongitudeE6()));
           
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
//        			   comPoint1.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint1.getLatitudeE6()/Constants.DOUBLE_1E6,
//        			   comPoint2.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint2.getLatitudeE6()/Constants.DOUBLE_1E6);
//        	   Log.d(TAG,strLog);
               return false;
           }
           else
           {
        	   
//        	   strLog = String.format("�����жϣ����� P1=%f,%f; P2=%f,%f", 
//        			   comPoint1.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint1.getLatitudeE6()/Constants.DOUBLE_1E6,
//        			   comPoint2.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint2.getLatitudeE6()/Constants.DOUBLE_1E6);
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
		            
	                //���ӵ��·�αȽ϶̣�������ֱ�ߵĶ˵�֮�䣻����Ҫ�ж�����ͶӰ�����ʼ�˵�ľ��룬ͨ������������ж��Ⱥ�˳�򣨷���
	                if (stPLDinfoC1.pointindex == stPLDinfoC2.pointindex) 
	                {
	                    GeoPoint machedProjPoint1 = new GeoPoint(0, 0);
	                    machedProjPoint1.setLatitudeE6((int) (stPLDinfoC1.projection.lat*1E6));
	                    machedProjPoint1.setLongitudeE6((int) (stPLDinfoC1.projection.lng*1E6));

	                    GeoPoint machedProjPoint2 = new GeoPoint(0, 0);
	                    machedProjPoint2.setLatitudeE6((int) (stPLDinfoC2.projection.lat*1E6));
	                    machedProjPoint2.setLongitudeE6((int) (stPLDinfoC2.projection.lng*1E6));
	                    
	                    double distancM1 = MetersBetweenGeoPoints(machedProjPoint1, roadPoints.get(stPLDinfoC1.pointindex));
	                    double distancM2 = MetersBetweenGeoPoints(machedProjPoint2, roadPoints.get(stPLDinfoC1.pointindex));

	                    if (distancM1 >= distancM2)
	                    {
	                        return false;
	                    }
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
