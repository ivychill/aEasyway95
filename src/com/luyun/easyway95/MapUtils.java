package com.luyun.easyway95;

import java.util.Arrays;

import android.location.Location;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;

public class MapUtils {
    //#Begin.....//路径拟合以及点和路径相关的判断算法
    //弧度计算
    private static double rad(double d)  
    {  
        return d * Math.PI / 180.0;  
    } 

    //返回两点之间的距离，单位为米
    public static double getDistance(Location loc1, Location loc2) {
    	return GetDistance(loc1.getLatitude(), loc1.getLongitude(), 
    			loc2.getLatitude(), loc2.getLongitude());
    }
    
    //返回两点之间的距离，单位为米
    public static double getDistance(Location loc1, GeoPoint p2) {
    	return GetDistance(loc1.getLatitude(), loc1.getLongitude(), p2.getLatitudeE6()/Constants.DOUBLE_1E6, 
    			p2.getLongitudeE6()/Constants.DOUBLE_1E6);
    }
    
    //返回两点之间的距离，单位为米
    public static double getDistance(GeoPoint p1, Location loc2) {
    	return GetDistance(p1.getLatitudeE6()/Constants.DOUBLE_1E6, p1.getLongitudeE6()/Constants.DOUBLE_1E6, 
    			loc2.getLatitude(), loc2.getLongitude());
    }
    
    //返回两点之间的距离，单位为米
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
        s = s * EARTH_RADIUS * 1000.0;  //单位:M
        s = Math.round(s * 10000) / 10000;  
        return s;  
    }  

    //返回两点之间的距离，输入是百度的微度 !~_~
    public static double MetersBetweenGeoPoints(GeoPoint geoPoint1, GeoPoint geoPoint2)  
    {  
    	double lat1 = geoPoint1.getLatitudeE6()/Constants.DOUBLE_1E6;
    	double lng1 = geoPoint1.getLongitudeE6()/Constants.DOUBLE_1E6;
    	double lat2 = geoPoint2.getLatitudeE6()/Constants.DOUBLE_1E6;
    	double lng2 = geoPoint2.getLongitudeE6()/Constants.DOUBLE_1E6;
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
		public GeoPoint getPoint() {
			GeoPoint pt = new GeoPoint((int)(projection.lat*1E6), (int)(projection.lng*1E6));
			return pt;
		}
    }
    
    //百度的微度到度之间的转换......
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
     * 返回GeoPointHelper
     * 2012.09.26 蔡庆丰增加
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
       
       //YSH_MODIFIED 2012-10-09 11:00
       //为了避免路径和拥堵线段都是地图上的平行或者垂直的时候，微小的偏差都会带来不能正确拟合的情况，这里人为增加拥堵路段的微小偏差
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
//           Log.d(TAG, "没有拟合的矩形");
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
//    			   CommRectP1.getLongitudeE6()/Constants.DOUBLE_1E6, CommRectP1.getLatitudeE6()/Constants.DOUBLE_1E6,
//    			   CommRectP2.getLongitudeE6()/Constants.DOUBLE_1E6, CommRectP2.getLatitudeE6()/Constants.DOUBLE_1E6);
//    	   Log.d(TAG,strLog);

    	   GeoPoint comPoint1 = new GeoPoint(0,0);
    	   GeoPoint comPoint2 = new GeoPoint(0,0);
    	   
           
      	 //YSH_MODIFIED 2012-10-09 11:00
           //加入强制类型转换
           double slope =  ((double)(trafficRectPoint2.getLatitudeE6() - trafficRectPoint1.getLatitudeE6()))
        		   /((double)(trafficRectPoint2.getLongitudeE6() - trafficRectPoint1.getLongitudeE6()));
           
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
//        			   comPoint1.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint1.getLatitudeE6()/Constants.DOUBLE_1E6,
//        			   comPoint2.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint2.getLatitudeE6()/Constants.DOUBLE_1E6);
//        	   Log.d(TAG,strLog);
               return false;
           }
           else
           {
        	   
//        	   strLog = String.format("方向判断，矩形 P1=%f,%f; P2=%f,%f", 
//        			   comPoint1.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint1.getLatitudeE6()/Constants.DOUBLE_1E6,
//        			   comPoint2.getLongitudeE6()/Constants.DOUBLE_1E6, comPoint2.getLatitudeE6()/Constants.DOUBLE_1E6);
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
		            
	                //如果拥堵路段比较短，在两个直线的端点之间；则需要判断两个投影点和起始端点的距离，通过这个距离来判断先后顺序（方向）
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
