package com.luyun.easyway95;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import com.luyun.easyway95.MapUtils.GeoPointHelper;
import com.luyun.easyway95.MapUtils.STPointLineDistInfo;
import com.luyun.easyway95.shared.TSSProtos.LYCheckin;
import com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir;
import com.luyun.easyway95.shared.TSSProtos.LYMsgType;
import com.luyun.easyway95.shared.TSSProtos.LYOsType;
import com.luyun.easyway95.shared.TSSProtos.LYRetCode;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYSegment;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficPub;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficReport;

public class MapHelper {
	private static String TAG = "MapHelper";
	//�ٶ�MapAPI�Ĺ�����, ��Easyway95App��ͬ
	BMapManager mBMapMan;
	
	private LYNavigator mainActivity;
	private GeoPoint mCurrentPoint;
	private GeoPoint mDestPoint;
	private ArrayList<MKPoiInfo> mRoadsAround;
	//private SegmentTraffic mCurrentSegTraffic; //traffic received from tss, used to update view of traffic list and update "traffic line??"
	private TrafficSubscriber mTrafficSubscriber;
	
	private MKRouteHelper mDrivingRoutes;
	private HotRoadsWithTraffic mHotRoadsWithTraffic;
	
	//�ն��ϱ�
	private LYLocation mLocationSet;
	
	public MapHelper(LYNavigator act) {
		mainActivity = act;
		mBMapMan = ((Easyway95App)mainActivity.getApplication()).mBMapMan;
		//mCurrentLocation = new Location("����");
		mCurrentPoint = new GeoPoint((int) (22.551541 * 1E6),
                (int) (113.94750 * 1E6));  //�ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢�� (�� * 1E6)
		mTrafficSubscriber = new TrafficSubscriber(mainActivity);
//		Date now = new Date();
//		if (now.getHours()<=12) {
//			mDestPoint = mainActivity.getOfficeAddr();
//		}else {
//			mDestPoint = mainActivity.getHomeAddr();
//		}
		mDestPoint = mainActivity.getLastDestination();
		mHotRoadsWithTraffic = new HotRoadsWithTraffic();
		mLocationSet = new LYLocation();
	}
	
	MKRouteHelper getDrivingRoutes() {
		return mDrivingRoutes;
	}
	
	HotRoadsWithTraffic getHotRoadsWithTraffic() {
		return mHotRoadsWithTraffic;
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
     * ���Ĺ��ܣ��յ�TSS����·����Ϣ�����д���
     * pacakge:msg, msg=[{name:r1, segments:[{startPoi:{lat, lng}, endPoi:{lat, lng}}, ...]}, ...]
     * �ͻ���Ҫ���ĸ���ͬ��������
     * DrivingRoutes: �˶������������ƫ�뺽�ߵ������������£��������ڽ���·��������ÿ��·���µ�ʱ��㽫������ǰ��·����
     * RoadsInsight:�ܱߵ�·����Ҳ���˶������в��ϸ��£�ÿ��10����ˢ��һ�Σ�����10KM����һ��
     * HotRoadsWithTraffic����ϵͳ���ã�һ��һ�����й�����ͬ����Ϣ�����ҿ��Դӷ�����������ͼƬ�������������ڽ�ͨ�����ĸ���ͼ
     * FavoriteRoads�����Ի����ã����û��������Լ��ļ�ͥ�Ͱ칫�ҵ�ַ֮���Զ����滮�õ�·���浽�û���preferences��profile������ϱ���������
     * 
     * ��¼�û��ͷǵ�¼�û��Ĵ���
     * �ǵ�¼�û���ͨ����TSS����Ϣ�������ű��ϱ����û���ʶ��DEVICEID@deviceid.android.roadclouding�������ʶҲ��������ʶZMQ��ID
     * ��¼�û����û����Ϊusername@deviceid.ios.roadclouding�������ʶҲ������ʶZMQ��ID
     * ��TSS֮���Ƿ��¼��ÿ�����Ҫͨ��Я��Token����ʶ�ͻ����Ƿ��¼
     * TSS��Ҫ����һ�ű����浱ǰ�����û���Token��2Сʱ֮�����û���յ��κ���Ϣ�����Token�����Ӧ��Profileɾ�����µ�Token����ʱ��TSS���������ϲ�ѯProfile
     * Roadclouding��TSS��Ҫһ�����ƣ�ȷ����Token�Ƿ���Ч���Լ���ȷTSS�Ƿ�ֱ�Ӵ�ȡ���ݿ⣿
     * �����¼�Ļ���Ҫ���û����ϱ����ɷ�������Profile�в�ѯ��listFavoriteRoads�����û���Ҫ��һ���任��username@roadclouding.com
     * ����TSS�ȴ��ͻ����ϱ�
     * 
     * ��ͼ����˵�����ӷ��������յ���·��ֻ��Ҫ���࣬һ���Ƕ���1�Զ��·����һ�����ʴ�ģʽ·���������ڿͻ����ȶ��ͷ��ȶ�����״̬��
     * ÿ�յ�һ�ζ���·�������Ƚ��й滮·����·����ϴ���ƥ��Ľ������listDrivingRoutes������������
     * Ȼ�����HotRoads��FavoriteRoadsƥ�䴦���ַ�����ͬ�ı��������У�һ��·����ͬʱ���������ϵ�������
     * �ܱ�·����һ��һ�𣬷Ƕ���ģʽ���յ��ܱ�·��ʱ����ԭ�������������Ӧ������ȫ������
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
    	case LY_CHECKIN:
    		String strUrl;
    		LYCheckin ci = msg.getCheckin();
    		Log.d(TAG, ci.toString());
    		int major = ci.getLyMajorRelease();
    		int minor = ci.getLyMinorRelease();
    		if (ci.hasDownloadUrl()) {
    			strUrl = ci.getDownloadUrl();
    			Log.d(TAG, "download url from server: " + strUrl);
    		} else {
    			strUrl = Constants.DOWNLOAD_URL;
    		}
    		if (major > mainActivity.getMajorRelease()) {
    			mainActivity.onSoftwareUpgrade(major, minor, strUrl, true);
    		} else if (minor > mainActivity.getMinorRelease()) {
    			mainActivity.onSoftwareUpgrade(major, minor, strUrl, false);
    		}
    		return;
    	case LY_TRAFFIC_PUB:
    		LYTrafficPub trafficPub = msg.getTrafficPub();
    		if (trafficPub == null) {
    			Log.i(TAG, "wrong message!");
    			return;
    		}
    		int routeId = trafficPub.getRouteId();
    		if (routeId <= 255 && routeId >=128) {
    			Log.d(TAG, "processing hotroads");
    			mHotRoadsWithTraffic.onTraffic(trafficPub);
    			return;
    		}
    		//��DrivingRoute�������
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
	 * λ�ø���ʱ�������ж��Ƿ�ƫ�뺽�ߣ�YES�����¹滮·��
	 * ���ϴ�λ���Ƿ���10KM��YES����ٶȷ����ܱ�·��POI��ѯ���ؼ��֣�·������10KM
	 * �뼴��;����·�������Ƿ�<1KM��YES�򵯳���ʾ������ˢ�¾��룬�ܷ��ܱ߲��µ�·��ʾ������
	 */
    public void onLocationChanged(Location location) {
    	Log.d(TAG, "in MapHelper::onLocationChanged");
    	mLocationSet.onLocationChanged(location);
    	LYTrafficReport tr = mLocationSet.genTraffic();
    	if (tr != null) {
    		byte[] payload = tr.toByteArray();
    		
        	LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.newBuilder()
    				.setVersion(1)
    				.setFromParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_CLIENT)
    				.setToParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_TC)
    				.setSndId(mainActivity.getDeviceID())
    				.setMsgType(com.luyun.easyway95.shared.TSSProtos.LYMsgType.LY_TRAFFIC_REPORT)
    				.setMsgId(2000)
    				.setTimestamp(System.currentTimeMillis()/1000)
    				.setTrafficReport(tr)
    				.build();
        	Log.d(TAG, msg.toString());
        	byte[] data = msg.toByteArray();
        	mainActivity.sendMsgToSvr(data);
    	}
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
    		TrafficPoint nextTrafficPoint = mDrivingRoutes.getNextTrafficPoint(mCurrentPoint);
    		//Log.d(TAG, nextTrafficPoint.toString());
    		mainActivity.genTrafficMsg(nextTrafficPoint);
    		//mainActivity.mTrafficPoint = nextTrafficPoint;
			//mainActivity.promptTraffic();
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
//    		//��ƥ����ӵ�µ����ҳ���һ���뵱ǰ����
//    		STPointLineDistInfo newSTPointLineDistInfo = mDrivingRoutes.new STPointLineDistInfo();
//    		Log.d(TAG, stPointLineDistInfo.toString());
//    		ArrayList<GeoPoint> allMatchedPoints = mDrivingRoutes.getMatchedPoints();
//    		GeoPoint nextTrafficPoint = null;
//    		double newDistanceOffRoad = mDrivingRoutes.getNearestDistanceOfRoad(mCurrentPoint, allMatchedPoints, newSTPointLineDistInfo);
//    		if (Math.abs(newDistanceOffRoad) < Constants.DISTANCE_OFF_ROAD) { //˵����������ĳ��ӵ��·�м�
//    			if (newSTPointLineDistInfo.getPointindex() < allMatchedPoints.size()) {
//    				//�ҳ��õ�����Ӧ��·��,������Ҫpopup����Ϣ�ı�
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
        // ��ʼ������ģ�飬ע���¼�����
    	MKSearch mkSearch = new MKSearch();
    	mkSearch.init(mBMapMan, new MKSearchListener(){
			public void onGetPoiResult(MKPoiResult res, int type, int error) {
				// ����ſɲο�MKEvent�еĶ���
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
			@Override
			public void onGetRGCShareUrlResult(String arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
        });
    	mkSearch.poiSearchNearBy("·", currentPoint, Constants.CHKPNT_OF_DISTANCE*1000);
    }
    
    public void requestDrivingRoutes(Location startLoc, Location endLoc) {
    	GeoPoint startPoint = new GeoPoint((int)(startLoc.getLatitude()*1E6), (int)(startLoc.getLongitude()*1E6));
    	GeoPoint endPoint = new GeoPoint((int)(endLoc.getLatitude()*1E6), (int)(endLoc.getLongitude()*1E6));
    	requestDrivingRoutes(startPoint, endPoint);
    }
    
    /*
     * 20121106 ����һ����װ��Ϊ�˱����ϴ�Ŀ�ĵ�ַ�������
     */
    public void requestDrivingRoutes(GeoPoint startPoint, MKPoiInfoHelper endPoi) {
    	mainActivity.setLastDestination(endPoi);
    	requestDrivingRoutes(startPoint, endPoi.getPt());
    }

    
    private void requestDrivingRoutes(GeoPoint startPoint, GeoPoint endPoint) {
		Log.d(TAG, "enter requestDrivingRoutes, start="+startPoint.toString()+",end="+endPoint.toString());
		mDestPoint = endPoint;
		
		MKPlanNode start = new MKPlanNode();
		start.pt = startPoint;
		MKPlanNode end = new MKPlanNode();
		end.pt = endPoint;
		// ���üݳ�·���������ԣ�ʱ�����ȡ��������ٻ�������
		MKSearch mMKSearch = new MKSearch();
		mMKSearch.init(mBMapMan, new MKSearchListener(){
			@Override
			public void onGetDrivingRouteResult(MKDrivingRouteResult result, int iError) {
				Log.d(TAG, "enter onGetDrivingRouteResult");
			    if (result == null) {
			        return;
			    }
			    // �˴���չʾһ��������Ϊʾ��
				//Log.d(TAG, "route plan number " + result.getNumPlan());
				//Log.d(TAG, "route number " + result.getPlan(0).getNumRoutes());
				MKRoute route = result.getPlan(0).getRoute(0);
				//���ｫԭ���Ĺ滮·������
				mDrivingRoutes = new MKRouteHelper(route, mainActivity.mMapUtils);
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

			@Override
			public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetTransitRouteResult(MKTransitRouteResult arg0,
					int arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetWalkingRouteResult(MKWalkingRouteResult arg0,
					int arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onGetRGCShareUrlResult(String arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
	    });
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		mMKSearch.drivingSearch(null, start, null, end);
	}
    
    public TrafficPoint getNextTrafficPoint() {
    	if (mDrivingRoutes != null)
    		return mDrivingRoutes.getNextTrafficPoint(mCurrentPoint);
    	return null;
    }
    
    public ArrayList<TrafficPoint> getAllTrafficPointsAhead() {
    	if (mDrivingRoutes != null)
    		return mDrivingRoutes.getAllTrafficPointsAhead(mCurrentPoint);
    	return null;
    }
    
	double getLinearDistanceFromHere(GeoPoint pt) {
		return mainActivity.mMapUtils.getDistance(mCurrentPoint, pt);
	}
	
	public GeoPoint getCurrentPoint() {
		return mCurrentPoint;
	}
	
	String formatDistanceMsg(double distance) {
		String msg = null;
		if (distance<1000) {
			msg = String.format("����Լ%d��", (int)(distance/100)*100);
		} else {
			msg = String.format("����Լ%#.1fǧ��", (float)(distance/1000));			
		}
		return msg;
	}
	
	public void checkIn() {
		LYCheckin ci = com.luyun.easyway95.shared.TSSProtos.LYCheckin.newBuilder()
				.setDeviceModel(android.os.Build.MODEL)
				.setOsType(LYOsType.LY_ANDROID)
				.setOsVersion(android.os.Build.VERSION.SDK)
				.setLyMajorRelease(mainActivity.getMajorRelease())
				.setLyMinorRelease(mainActivity.getMinorRelease())
				.build();
		byte[] payload = ci.toByteArray();
		
    	LYMsgOnAir msg = com.luyun.easyway95.shared.TSSProtos.LYMsgOnAir.newBuilder()
				.setVersion(1)
				.setFromParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_CLIENT)
				.setToParty(com.luyun.easyway95.shared.TSSProtos.LYParty.LY_TSS)
				.setSndId(mainActivity.getDeviceID())
				.setMsgType(com.luyun.easyway95.shared.TSSProtos.LYMsgType.LY_CHECKIN)
				.setMsgId(2000)
				.setTimestamp(System.currentTimeMillis()/1000)
				.setCheckin(ci)
				.build();
    	
    	Log.d(TAG, msg.toString());
    	byte[] data = msg.toByteArray();
    	mainActivity.sendMsgToSvr(data);
	}
	
}

