package com.luyun.easyway95;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.restlet.data.*;
import org.restlet.ext.json.JsonRepresentation;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MapView;
import com.google.protobuf.InvalidProtocolBufferException;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;

import android.app.Activity;
import android.app.ListActivity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;

public class TrafficsOfRoute extends ListActivity {
	// VERBOSE debug log is complied in but stripped at runtime
	private static final String TAG = "TrafficsOfRoute";
	private Easyway95App app;
	private LYNavigator mainActivity;
	
	//private ListView mListView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		app = (Easyway95App)this.getApplication();
		mainActivity = app.getMainActivity();

        SimpleAdapter adapter = new SimpleAdapter(this, getTrafficsOfRoute(), R.layout.vlist,
                new String[]{"road", "desc", "timestamp"},
                new int[]{R.id.road, R.id.desc, R.id.timestamp});
        setListAdapter(adapter);
    }
    
    private List<Map<String, Object>> getTrafficsOfRoute() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	
        Map<String, Object> map = null;
        MKRouteHelper drivingRoutes = mainActivity.mMapHelper.getDrivingRoutes();
        Log.d(TAG, "fetching data from driving routes!");
        if (drivingRoutes == null) {
        	Log.d(TAG, "no data before request driving routes!");
        	return list;
        }
        Log.d(TAG, "Driving routes not null. Fetching data from driving routes!");
        Map<String, DrivingRoadWithTraffic> roadTraffics = drivingRoutes.getRoadsWithTraffic();
        Iterator it = roadTraffics.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<String, DrivingRoadWithTraffic> entry = (Entry<String, DrivingRoadWithTraffic>) it.next();
        	DrivingRoadWithTraffic rt = (DrivingRoadWithTraffic) entry.getValue();
        	Map<Integer, ArrayList<GeoPoint>> matchedPoints = rt.getMatchedPoints();
        	if (matchedPoints == null || matchedPoints.size() == 0) continue;
        	
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
	        	//检查是否在matchedPoints的map里
	        	ArrayList<GeoPoint> tmpPoints = matchedPoints.get(i);
	        	if (tmpPoints == null) continue; //不在Map里，说明该段无拟合，可能是反方向，也可能是其它路况
        		map = new HashMap<String, Object>();
	        	map.put("road", entry.getKey());
	        	map.put("desc", segments.get(i).getDetails());
	        	int speed = segments.get(i).getSpeed();
	        	String strSpeed = Constants.TRAFFIC_JAM_LVL_HIGH;
	        	if (speed >= Constants.TRAFFIC_JAM_LVL_MIDDLE_SPD) strSpeed = Constants.TRAFFIC_JAM_LVL_LOW;
	        	if (speed < Constants.TRAFFIC_JAM_LVL_MIDDLE_SPD && speed >= Constants.TRAFFIC_JAM_LVL_HIGH_SPD) strSpeed = Constants.TRAFFIC_JAM_LVL_MIDDLE;
	        	String formatedStr = String.format("%d分钟前，%s", interval, strSpeed);
	            map.put("timestamp", formatedStr);
	        	list.add(map);
        	}
        }
    	return list;
    }

    private List<Map<String, Object>> getTrafficsOfRoute_for_test() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("road", "G1");
        map.put("desc", "google 1");
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put("road", "G2");
        map.put("desc", "google 2");
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put("road", "G3");
        map.put("desc", "google 3");
        list.add(map);
    	return list;
    }
}