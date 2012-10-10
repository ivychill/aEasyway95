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
import com.google.protobuf.InvalidProtocolBufferException;
import com.luyun.easyway95.MKRouteHelper.RoadTrafficHelper;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;
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

public class TrafficsCaring extends ListActivity {
	// VERBOSE debug log is complied in but stripped at runtime
	private static final String TAG = "ShowTraffics";
	private Easyway95App app;
	private MainActivity mainActivity;
	
	//private ListView mListView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		app = (Easyway95App)this.getApplication();
		mainActivity = app.getMainActivity();

        // Use an existing ListAdapter that will map an array
        // of strings to TextViews
        //setListAdapter(new ArrayAdapter<String>(this,
        //        android.R.layout.simple_list_item_1, getTrafficsOfRoute()));
        //getListView().setTextFilterEnabled(true);
        SimpleAdapter adapter = new SimpleAdapter(this, getTrafficsCaring(), R.layout.vlist,
                new String[]{"road","desc", "timestamp"},
                new int[]{R.id.road,R.id.desc, R.id.timestamp});
        setListAdapter(adapter);
    }
    
    private List<Map<String, Object>> getTrafficsCaring() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	
        Map<String, Object> map = null;
        MKRouteHelper drivingRoutes = mainActivity.mMapHelper.getDrivingRoutes();
        Log.d(TAG, "fetching data from driving routes!");
        if (drivingRoutes == null) {
        	Log.d(TAG, "no data before request driving routes!");
        	return list;
        }
        Log.d(TAG, "Driving routes not null. Fetching data from driving routes!");
        Map<String, RoadTrafficHelper> roadTraffics = drivingRoutes.getRoadsWithTraffic();
        Iterator it = roadTraffics.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<String, RoadTrafficHelper> entry = (Entry<String, RoadTrafficHelper>) it.next();
        	RoadTrafficHelper rt = (RoadTrafficHelper) entry.getValue();
        	ArrayList<LYSegmentTraffic> segments = rt.getSegments();
        	if (segments == null) continue;
        	Date now = new Date();
        	long nowTime = now.getTime()/1000;
        	for (int i=0; i<segments.size(); i++) {
	        	long time_stamp = segments.get(i).getTimestamp();
	        	long interval = (now.getTime()/1000 - time_stamp)/60;
	        	if (interval > Constants.TRAFFIC_LAST_DURATION) {
	        		rt.clearSegment(i);
	        		continue;
	        	}
        		map = new HashMap<String, Object>();
	        	map.put("road", entry.getKey());
	        	map.put("desc", segments.get(i).getDetails());
	        	int speed = segments.get(i).getSpeed();
	        	String strSpeed = Constants.TRAFFIC_JAM_LVL_HIGH;
	        	if (speed >= 15) strSpeed = Constants.TRAFFIC_JAM_LVL_MIDDLE;
	        	if (speed < 15 && speed >=6) strSpeed = Constants.TRAFFIC_JAM_LVL_LOW;
	        	String formatedStr = String.format("%d分钟前，%s", interval, strSpeed);
	            map.put("timestamp", formatedStr);
	        	list.add(map);
        	}
        }
    	return list;
    }
    
    private List<Map<String, Object>> getTrafficsCaring_for_test() {
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	Date now = new Date();
    	
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("road", "深南大道");
        map.put("desc", "南山路口到滨海路口拥堵，东向");
        map.put("timestamp", now.toLocaleString());
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put("road", "南山大道");
        map.put("desc", "南山路口到滨海路口拥堵，东向");
        map.put("timestamp", now.toLocaleString());
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put("road", "滨海大道");
        map.put("desc", "南山路口到滨海路口拥堵，东向");
        map.put("timestamp", now.toLocaleString());
        list.add(map);
    	return list;
    }

}