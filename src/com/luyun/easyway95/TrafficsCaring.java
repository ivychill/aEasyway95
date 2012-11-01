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
	private LYNavigator mainActivity;
	
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
    	
        HotRoadsWithTraffic hotRoadsWithTraffic = mainActivity.mMapHelper.getHotRoadsWithTraffic();
        if (hotRoadsWithTraffic != null) {
        	list.addAll(hotRoadsWithTraffic.getAllRoadsWithTrafficByList());
        }
        MKRouteHelper drivingRoutes = mainActivity.mMapHelper.getDrivingRoutes();
        if (drivingRoutes != null) {
        	List<Map<String, Object>> tmpList = drivingRoutes.getAllRoadsWithTrafficByList();
        	if (tmpList != null) {
        		for (int i=0; i<tmpList.size();i++) {
        			Map<String, Object> item = tmpList.get(i);
        			String road = (String) item.get("road");
        			if (road != null && !hotRoadsWithTraffic.hasRoad(road)) {
        				list.add(item);
        			}
        		}
        	}
        }
        //删除重复的，如果一条路既是规划路径，又是热点路况，就会重复
        
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