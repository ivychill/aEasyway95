package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.location.Location;
import android.util.Log;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.RouteOverlay;
import com.luyun.easyway95.shared.TSSProtos.SegmentTraffic;

public class MapHelper {
	private static String TAG = "MapHelper";
	//百度MapAPI的管理类, 与Easyway95App相同
	BMapManager mBMapMan;
	
	private MainActivity mainActivity;
	public Location mCurrentLocation;
	private SegmentTraffic mCurrentSegTraffic; //traffic received from tss, used to update view of traffic list and update "traffic line??"
	private TrafficSubscriber mTrafficSubscriber;
	
	//private DrivingRoutes;
	
	public MapHelper(MainActivity act) {
		mainActivity = act;
		mBMapMan = ((Easyway95App)mainActivity.getApplication()).mBMapMan;
		mCurrentLocation = new Location("深圳");
		mTrafficSubscriber = new TrafficSubscriber(mainActivity);
	}
	
	SegmentTraffic getSegTraffic() {
		return null;
	}
	
	/*public boolean createTrafficPolylineInfo(SegmentTraffic segTraffic, RoadInfo roadInfo) {
		return true;
	}
	
	public class RoadInfo {
		private String name;
		private List<PointHelper> points;
	}
	
	public class PointHelper {
		private int stepIndex;
		private int pointIndex;
		private GeoPoint coordinate;
	}*/

    public void onMsg(com.luyun.easyway95.shared.TSSProtos.Package msg) {
    	Log.d(TAG, "in onMsg");
    	//process commands here!
    	//case failure:
    	//case traffic_update:
    		//match segment traffic and driving routes
    		//update views
    		//
    }
    
    public void onLocationChanged(Location location) {
    	Log.d(TAG, "in onLocationChanged");
		//update marker
		//request a new driving route to baidu in case off road
		//popup a prompt when distance between current location with traffic < 2KM
    	mCurrentLocation = location;

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
    
}

