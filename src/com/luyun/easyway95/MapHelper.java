package com.luyun.easyway95;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKPoiInfo;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKStep;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.PoiOverlay;
import com.baidu.mapapi.RouteOverlay;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;

public class MapHelper {
	private static String TAG = "MapHelper";
	//百度MapAPI的管理类, 与Easyway95App相同
	BMapManager mBMapMan;
	
	private MainActivity mainActivity;
	public GeoPoint mCurrentPoint;
	public GeoPoint mDestPoint;
	private ArrayList<MKPoiInfo> mRoadsAround;
	//private SegmentTraffic mCurrentSegTraffic; //traffic received from tss, used to update view of traffic list and update "traffic line??"
	private TrafficSubscriber mTrafficSubscriber;
	
	private MKRouteHelper mDrivingRoutes;
	
	//private DrivingRoutes;
	
	public MapHelper(MainActivity act) {
		mainActivity = act;
		mBMapMan = ((Easyway95App)mainActivity.getApplication()).mBMapMan;
		//mCurrentLocation = new Location("深圳");
		mCurrentPoint = new GeoPoint((int) (22.551541 * 1E6),
                (int) (113.94750 * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
		mTrafficSubscriber = new TrafficSubscriber(mainActivity);
	}
	
	/*SegmentTraffic getSegTraffic() {
		return null;
	}
	
	public boolean createTrafficPolylineInfo(SegmentTraffic segTraffic, RoadInfo roadInfo) {
		return true;
	}
	
	public class RoadInfo {
		private String name;
		private List<PointHelper> points;
	}*/
	

    /*
     * 核心功能：收到TSS发的路况信息，进行处理。
     * pacakge:msg, msg=[{name:r1, segments:[{startPoi:{lat, lng}, endPoi:{lat, lng}}, ...]}, ...]
     * 客户端要有四张表：
     * listDrivingRoutes: 运动过程中如果有偏离航线的情况发生则更新，否则用于接收路况。对于每条路，新的时间点将覆盖以前的路况。
     * listRoadsInsight:周边的路况，也在运动过程中不断更新，每隔10分钟刷新一次，或者10KM更新一次
     * listHotRoads：由系统设置，一般一个城市共享相同的信息，并且可以从服务器上生成图片传下来，类似于交通播报的概略图
     * listFavoriteRoads：个性化设置，在用户设置完自己的家庭和办公室地址之后，自动将规划好的路径存到用户的preferences和profile里，并且上报到服务器
     * 
     * 登录用户和非登录用户的处理：
     * 非登录用户：通过与TSS的消息将这四张表上报，用户标识是DEVICEID@deviceid.android.roadclouding，这个标识也将用来标识ZMQ的ID
     * 登录用户：用户标记为username@deviceid.ios.roadclouding，这个标识也用来标识ZMQ的ID
     * 与TSS之间是否登录，每条命令都要通过携带Token，标识客户端是否登录
     * TSS需要生成一张表，保存当前激活用户的Token，2小时之内如果没有收到任何消息则将这个Token和其对应的Profile删除，新的Token来的时候，TSS到服务器上查询Profile
     * Roadclouding和TSS需要一个机制，确定该Token是否有效，以及明确TSS是否直接存取数据库？
     * 如果登录的话，要将用户名上报，由服务器从Profile中查询其listFavoriteRoads，其用户名要做一个变换：username@roadclouding.com
     * 否则TSS等待客户端上报
     * 
     * 视图更新说明（从服务器器收到的路况只需要两类，一类是订阅1对多的路况，一类是问答模式路况，适用于客户端稳定和非稳定两种状态）
     * 每收到一次订阅路况，首先进行规划路径和路况拟合处理，匹配的结果发到listDrivingRoutes，并更新气泡
     * 然后进行HotRoads和FavoriteRoads匹配处理，分发到不同的表（容器）中，一条路可能同时在两个以上的容器里
     * 周边路况是一问一答，非订阅模式。收到周边路况时，将原来由这个表所对应的气泡全部更新
     */
	public void onMsg(LYMsgOnAir msg) {
    	Log.d(TAG, "in onMsg");
    	//process commands here!
    	//case failure:
    	//case traffic_update:
    		//match segment traffic and driving routes
    		//update views
    		//
    	//if (msg.getMsgType() == )
    }
    
	/*
	 * 位置更新时，首先判断是否偏离航线？YES则重新规划路径
	 * 与上次位置是否有10KM？YES则向百度发起周边路的POI查询，关键字：路，距离10KM
	 * 与即将途径的路况距离是否<1KM，YES则弹出警示，不断刷新距离，能否将周边不堵的路显示出来？
	 */
    public void onLocationChanged(Location location) {
    	Log.d(TAG, "in onLocationChanged");
		//update marker
		//request a new driving route to baidu in case off road
		//popup a prompt when distance between current location with traffic < 10KM
    	mCurrentPoint = GeoPointHelper.buildGeoPoint(location);
    	
    	STPointLineDistInfo stPointLineDistInfo = new STPointLineDistInfo();
    	double distanceOffRoad = 0.0;
    	if (mDrivingRoutes != null) {
    		distanceOffRoad = getNearestDistanceOfRoad(mCurrentPoint, mDrivingRoutes.getAllPoints(), stPointLineDistInfo);
    	} else { //use home-office route to check
    		return;
    	}
    	if (Math.abs(distanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { 
    		//on road
    		//从匹配后的拥堵点中找出下一个与当前距离
    	} else {
    		//off road
    		requestDrivingRoutes(mCurrentPoint, mDestPoint);
    	}
    }
    
    public void requestRoadsAround(Location currentLocation) {
    	//GeoPoint point = new GeoPoint((int)(currentLocation.getLatitude()*1E6), (int)(currentLocation.getLongitude()*1E6));
    	requestRoadsAround(GeoPointHelper.buildGeoPoint(currentLocation));
    }
    
    public void requestRoadsAround(GeoPoint currentPoint) {
    	Log.d(TAG, "in requestRoadsInsight");
        // 初始化搜索模块，注册事件监听
    	MKSearch mkSearch = new MKSearch();
    	mkSearch.init(mBMapMan, new MKSearchListener(){
			public void onGetPoiResult(MKPoiResult res, int type, int error) {
				// 错误号可参考MKEvent中的定义
				if (error != 0 || res == null) {
					Log.d(TAG, "no result found.");
					return;
				}

			    if (res.getCurrentNumPois() > 0) {
					//poiOverlay.setData(res.getAllPoi());
			    	Log.d(TAG, "found a lot of roads around. size="+res.getCurrentNumPois());
			    	mRoadsAround = res.getAllPoi();
			    } else if (res.getCityListNum() > 0) { //do nothing here
			    }
			}
			public void onGetDrivingRouteResult(MKDrivingRouteResult res,
					int error) {
			}
			public void onGetTransitRouteResult(MKTransitRouteResult res,
					int error) {
			}
			public void onGetWalkingRouteResult(MKWalkingRouteResult res,
					int error) {
			}
			public void onGetAddrResult(MKAddrInfo res, int error) {
			}
			public void onGetBusDetailResult(MKBusLineResult result, int iError) {
			}
			@Override
			public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
				// TODO Auto-generated method stub
			}
			
        });
    	mkSearch.poiSearchNearBy("路", currentPoint, Constants.CHKPNT_OF_DISTANCE*1000);
    }
    
    public void requestDrivingRoutes(Location startLoc, Location endLoc) {
    	GeoPoint startPoint = new GeoPoint((int)(startLoc.getLatitude()*1E6), (int)(startLoc.getLongitude()*1E6));
    	GeoPoint endPoint = new GeoPoint((int)(endLoc.getLatitude()*1E6), (int)(endLoc.getLongitude()*1E6));
    	requestDrivingRoutes(startPoint, endPoint);
    }
    
    public void requestDrivingRoutes(GeoPoint startPoint, GeoPoint endPoint) {
		//Log.d(TAG, "enter onClick");
		MKPlanNode start = new MKPlanNode();
		start.pt = startPoint;
		MKPlanNode end = new MKPlanNode();
		end.pt = endPoint;
		// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
		MKSearch mMKSearch = new MKSearch();
		mMKSearch.init(mBMapMan, new TrafficSearchListener(){
			@Override
			public void onGetDrivingRouteResult(MKDrivingRouteResult result, int iError) {
				//Log.d(TAG, "enter onGetDrivingRouteResult");
			    if (result == null) {
			        return;
			    }
			    RouteOverlay routeOverlay = new RouteOverlay(mainActivity, mainActivity.mMapView);
			    // 此处仅展示一个方案作为示例
				//Log.d(TAG, "route plan number " + result.getNumPlan());
				//Log.d(TAG, "route number " + result.getPlan(0).getNumRoutes());
				MKRoute route = result.getPlan(0).getRoute(0);
				//这里将原来的规划路径覆盖
				mDrivingRoutes = new MKRouteHelper(route);
				
				routeOverlay.setData(route);
				mainActivity.mMapView.getOverlays().add(routeOverlay);
				mainActivity.mMapView.invalidate();  //刷新地图
			    
			    Log.d(TAG, "ArrayList<ArrayList<GeoPoint>> size..." + route.getArrayPoints().size());

			    Iterator<ArrayList<GeoPoint>> itr = route.getArrayPoints().iterator();
		    	int index = 0;
			    while(itr.hasNext())
			    {
			    	ArrayList<GeoPoint> arrayPoint = itr.next();
			    	Log.d(TAG, "ArrayList<GeoPoint> index..." + index++);
			    	Log.d(TAG, "ArrayList<GeoPoint> size..." + arrayPoint.size());
			    	Log.d(TAG, "ArrayList<GeoPoint> ..." + arrayPoint.toString());
			    }

			    mTrafficSubscriber.SubTraffic(route);
			}
	    });
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		mMKSearch.drivingSearch(null, start, null, end);
	}
    
  //#Begin.....//路径拟合以及点和路径相关的判断算法
    //弧度计算
    private static double rad(double d)  
    {  
        return d * Math.PI / 180.0;  
    } 

    //返回两点之间的距离，单位为米
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
    private class STPointLineDistInfo
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
    public double getNearestDistanceOfRoad(Location location, java.util.ArrayList<GeoPoint> roadPoints, STPointLineDistInfo retPLDInfo) {
    	return getNearestDistanceOfRoad(GeoPointHelper.buildGeoPoint(location), roadPoints, retPLDInfo);
    }
    
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

