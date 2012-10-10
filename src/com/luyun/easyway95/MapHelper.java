package com.luyun.easyway95;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import com.luyun.easyway95.MKRouteHelper.GeoPointHelper;
import com.luyun.easyway95.MKRouteHelper.STPointLineDistInfo;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYMsgType;
import com.luyun.easyway95.shared.TSSProtos.LYRetCode;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
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
		Date now = new Date();
		if (now.getHours()<=12) {
			mDestPoint = mainActivity.getOfficeAddr();
		}else {
			mDestPoint = mainActivity.getHomeAddr();
		}
	}
	
	MKRouteHelper getDrivingRoutes() {
		return mDrivingRoutes;
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
    	LYMsgType msgType = msg.getMsgType();
    	switch (msgType) {
    	case LY_RET_CODE:
    		if (msg.getRetCode() != LYRetCode.LY_SUCCESS) {
    			Log.e(TAG, "some errors happend, error code="+msg.getRetCode());
    		}
    		return;
    	case LY_TRAFFIC_PUB:
    		//与DrivingRoute进行拟合
    		if (mDrivingRoutes == null) {
    			//the only situation happened when traffic_pub arrives prior to driving routes instantiated, i.e, app reboots
    			//just ignore it
    			Log.e(TAG, "traffic_pub arrives before driving routes instantiated.");
    			return;
    		}
    		mDrivingRoutes.onTraffic(msg.getTrafficPub(), false);
    		break;
    	default:
    		Log.e(TAG, "Wrong branch here!");
    		break;
    	}
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
    	if (mDrivingRoutes == null) {
    		requestDrivingRoutes(mCurrentPoint, mDestPoint);
    		return;
    	}
    	
    	if (mDrivingRoutes.isOnRoute(mCurrentPoint)) {
    		Log.d(TAG, "on route");
    		TrafficPoint nextTrafficPoint = mDrivingRoutes.getTrafficPoint(mCurrentPoint);
    		//Log.d(TAG, nextTrafficPoint.toString());
			mainActivity.popupTrafficDialog(nextTrafficPoint);
			return;
    	} else {
    		//off road
    		requestDrivingRoutes(mCurrentPoint, mDestPoint);
    	}
//    	STPointLineDistInfo stPointLineDistInfo = mDrivingRoutes.new STPointLineDistInfo();
//    	double distanceOffRoad = 0.0;
//    	if (mDrivingRoutes != null) {
//    		distanceOffRoad = mDrivingRoutes.getNearestDistanceOfRoad(mCurrentPoint, mDrivingRoutes.getAllPoints2(), stPointLineDistInfo);
//    	} else { //use home-office route to check
//        	requestDrivingRoutes(mCurrentPoint, mDestPoint);
//    		return;
//    	}
//    	if (Math.abs(distanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { 
//    		//on road
//    		//从匹配后的拥堵点中找出下一个与当前距离
//    		STPointLineDistInfo newSTPointLineDistInfo = mDrivingRoutes.new STPointLineDistInfo();
//    		Log.d(TAG, stPointLineDistInfo.toString());
//    		ArrayList<GeoPoint> allMatchedPoints = mDrivingRoutes.getMatchedPoints();
//    		GeoPoint nextTrafficPoint = null;
//    		double newDistanceOffRoad = mDrivingRoutes.getNearestDistanceOfRoad(mCurrentPoint, allMatchedPoints, newSTPointLineDistInfo);
//    		if (Math.abs(newDistanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { //说明现在正在某段拥堵路中间
//    			if (newSTPointLineDistInfo.getPointindex() < allMatchedPoints.size()) {
//    				//找出该点所对应的路来,生成需要popup的消息文本
//    				//mainActivity.popupTrafficDialg();
//    			}
//    		} else {
//    			
//    		}
//    	} else {
//    		//off road
//    		requestDrivingRoutes(mCurrentPoint, mDestPoint);
//    	}
    }
    
    public void requestRoadsAround(Location currentLocation) {
    	//GeoPoint point = new GeoPoint((int)(currentLocation.getLatitude()*1E6), (int)(currentLocation.getLongitude()*1E6));
    	requestRoadsAround(GeoPointHelper.buildGeoPoint(currentLocation));
    }
    
    public void requestRoadsAround(GeoPoint currentPoint) {
    	Log.d(TAG, "in requestRoadsAround");
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
		Log.d(TAG, "enter requestDrivingRoutes, start="+startPoint.toString()+",end="+endPoint.toString());
		MKPlanNode start = new MKPlanNode();
		start.pt = startPoint;
		MKPlanNode end = new MKPlanNode();
		end.pt = endPoint;
		// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
		MKSearch mMKSearch = new MKSearch();
		mMKSearch.init(mBMapMan, new TrafficSearchListener(){
			@Override
			public void onGetDrivingRouteResult(MKDrivingRouteResult result, int iError) {
				Log.d(TAG, "enter onGetDrivingRouteResult");
			    if (result == null) {
			        return;
			    }
			    // 此处仅展示一个方案作为示例
				//Log.d(TAG, "route plan number " + result.getNumPlan());
				//Log.d(TAG, "route number " + result.getPlan(0).getNumRoutes());
				MKRoute route = result.getPlan(0).getRoute(0);
				//这里将原来的规划路径覆盖
				mDrivingRoutes = new MKRouteHelper(route);
			    mainActivity.resetMapView();
			    
			    //Log.d(TAG, "ArrayList<ArrayList<GeoPoint>> size..." + route.getArrayPoints().size());

			    //Iterator<ArrayList<GeoPoint>> itr = route.getArrayPoints().iterator();
		    	//int index = 0;
			    //while(itr.hasNext())
			    //{
			    	//ArrayList<GeoPoint> arrayPoint = itr.next();
			    	//Log.d(TAG, "ArrayList<GeoPoint> index..." + index++);
			    	//Log.d(TAG, "ArrayList<GeoPoint> size..." + arrayPoint.size());
			    	//Log.d(TAG, "ArrayList<GeoPoint> ..." + arrayPoint.toString());
			    //}

			    mTrafficSubscriber.SubTraffic(route);
			}
	    });
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		mMKSearch.drivingSearch(null, start, null, end);
	}
    
    public TrafficPoint getNextTrafficPoint() {
    	if (mDrivingRoutes != null)
    		return mDrivingRoutes.getTrafficPoint(mCurrentPoint);
    	return null;
    }
    
	double getLinearDistanceFromHere(GeoPoint pt) {
		return MKRouteHelper.getDistance(mCurrentPoint, pt);
	}
	
	String formatDistanceMsg(double distance) {
		String msg = null;
		if (distance<1000) {
			msg = String.format("距离约%d米", (int)(distance/1000));
		} else {
			msg = String.format("距离约%d千米", (int)(distance/1000*1000));			
		}
		return msg;
	}
}

