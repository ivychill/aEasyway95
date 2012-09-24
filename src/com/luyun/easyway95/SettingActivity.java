package com.luyun.easyway95;

import java.io.IOException;
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
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

public class SettingActivity extends TabActivity {
	// VERBOSE debug log is complied in but stripped at runtime
	private static final String TAG = "SettingActivity";
	private String msAuthToken;
	private String msSessionId;
	private boolean mbTokenLogon = false;
	private boolean mbSessionLogon = false;
	private UserProfile mUserProfile;
	private TabHost myTabhost;
	private Easyway95App app;
	private SharedPreferences mSP;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		app = (Easyway95App)this.getApplication();
		
		//register to Application
		app.setSettingActivity(this);
		
		//setContentView(R.layout.setting);
		msAuthToken = retrieveAuthToken();
		msSessionId = retrieveSessionToken();
		//Log.d(TAG, "kkkk");
		String resURL = "";
		if (msAuthToken != null) { //use token
			Log.d(TAG, msAuthToken);
			mbTokenLogon = true;
	        resURL = Constants.USERS_PROFILE_URL+"?auth_token="+msAuthToken;
		}else if (msSessionId != null) {
			Log.d(TAG, msSessionId);
			mbSessionLogon = true;
	        resURL = Constants.USERS_PROFILE_URL;
		}
		//Log.d(TAG, "ffff");
        myTabhost=this.getTabHost();       
        LayoutInflater.from(this).inflate(R.layout.setting, myTabhost.getTabContentView(), true);
        myTabhost.setBackgroundColor(Color.argb(150, 22, 70, 150));
        
        myTabhost.addTab(myTabhost.newTabSpec("Ahead")
                .setIndicator("途径路况",
                        getResources().getDrawable(R.drawable.trafficbtnlogo))
                        .setContent(R.id.linearLayout_blue));
        myTabhost.addTab(myTabhost.newTabSpec("More")
                .setIndicator("可能还关注",
                        getResources().getDrawable(R.drawable.trafficbtnlogo))
                        .setContent(R.id.linearLayout_green));      
        myTabhost.addTab(myTabhost.newTabSpec("Setting")
                .setIndicator("设置",
                        getResources().getDrawable(R.drawable.trafficbtnlogo))
                        .setContent(R.id.setting_layout));

        //query SharedPreferences
		mSP = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mUserProfile = new UserProfile(mSP);
		//Log.d(TAG, mUserProfile.toString());
		//set text views
		TextView txtUserName = (TextView)findViewById(R.id.username);
		txtUserName.setText(mUserProfile.getUserName());
		TextView txtHome = (TextView)findViewById(R.id.homeaddr);
		txtHome.setText(mUserProfile.getHomeAddr().getName());
		TextView txtOffice = (TextView)findViewById(R.id.officeaddr);
		txtOffice.setText(mUserProfile.getOfficeAddr().getName());
		
        Button btnSethome = (Button)findViewById(R.id.sethome);
        btnSethome.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		Intent i = new Intent(SettingActivity.this, PoiSearch.class);
    			TextView txtHome = (TextView)findViewById(R.id.homeaddr);
    			Log.d(TAG, txtHome.getText().toString());
           		i.putExtra("search_key", txtHome.getText().toString());
           		i.putExtra("search_place", "home");
           	    startActivity(i);
        	}
        });
        Button btnSetoffice = (Button)findViewById(R.id.setoffice);
        btnSetoffice.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//start login activity
        		Intent i = new Intent(SettingActivity.this, PoiSearch.class);
    			TextView txtOffice = (TextView)findViewById(R.id.officeaddr);
    			Log.d(TAG, txtOffice.getText().toString());
           		i.putExtra("search_key", txtOffice.getText().toString());
           		i.putExtra("search_place", "office");
        		startActivity(i);
        	}
        });
        
		if (mbTokenLogon || mbSessionLogon) {
	        Button btn = (Button)findViewById(R.id.login);
	        btn.setText("登   出");
	        
	        //ClientResource cr = new ClientResource(Constants.USERS_PROFILE_URL);
	        ClientResource cr = new ClientResource(resURL);
	        if (mbSessionLogon) {
		        Series<Cookie> cookies = cr.getCookies();
		        //Cookie ck = cr.getCookies().getFirst("auth_token");
		        //cookies.add("auth_token", msAuthToken);
		        cookies.add("_roadclouding_session", msSessionId);
		        cr.setCookies(cookies);
		        Log.d(TAG, cookies.toString());
	        }
			// Set the callback object invoked when the response is received.
			cr.setOnResponse(new Uniform() {
			    public void handle(Request request, Response response) {
			        // Get the representation as an JsonRepresentation
			        try {
			        	JsonRepresentation rep = new JsonRepresentation(response.getEntity());

				        // Displays the properties and values.
			        	try {
				            JSONObject jsonObject = rep.getJsonObject();
				            if (jsonObject != null) {
				            	Log.d(TAG, jsonObject.toString());
				            	mUserProfile.updateFields(jsonObject);
				            	mUserProfile.commitPreferences(mSP);
				            	//update view
				        		TextView txtUserName = (TextView)findViewById(R.id.username);
				        		txtUserName.setText(mUserProfile.getUserName());
				            	//Log.d(TAG, mUserProfile.toString());
				            }
			        	}catch(Exception e) {
			        		e.printStackTrace();
			        	}
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    }
			});

			// Indicates the client preferences and let the server handle
			// the best representation with content negotiation.
			cr.get(MediaType.APPLICATION_JSON);
			//UserProfileResource upr = cr.wrap(UserProfileResource.class);
			//mUserProfile = upr.retrieve();
			//Log.d(TAG, cr.get(MediaType.APPLICATION_XHTML).toString());
			//String strProfile = cr.get(MediaType.APPLICATION_JSON).getText();
				//JSONTokener jsonParser = new JSONTokener(strProfile);
				//JSONObject jsonObj = new JSONObject(strProfile).getJSONObject();
			//Log.d(TAG, mUserProfile.toString());
		}
		
	}

	/**
	 * @return boolean return true if the application can access the internet
	 */
	private boolean isConnectedToInternet() {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		if (info == null) {
			return false;
		} else if (!info.isConnected()) {
			return false;
		} else if (info.getType() != ConnectivityManager.TYPE_WIFI) {
			return false;
		} 
		return true;
	}
	
	/** get the stored cookies */
	private String retrieveAuthToken() {
		Log.d(TAG, "in retrieveAuthToken");
		CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
	    CookieManager cookieManager = CookieManager.getInstance();
	    String cookie = cookieManager.getCookie(Constants.USERS_PROFILE_URL);
	    if (cookie != null) {
	    	Log.d(TAG, cookie);
	    	Pattern reg = Pattern.compile("auth_token=(.+)(;*)");
	    	Matcher mch = reg.matcher(cookie);
	    	if (mch.find()) {
	    		String s0 = mch.group(0);
	    		String s1 = mch.group(1);
	    		return s1;
	    	}
	    }
	    return null;
	}

	/** get the stored session tokens cookies */
	private String retrieveSessionToken() {
		CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
	    CookieManager cookieManager = CookieManager.getInstance();
	    String cookie = cookieManager.getCookie(Constants.USERS_PROFILE_URL);
	    if (cookie != null) {
	    	Log.d(TAG, cookie);
	    	Pattern reg = Pattern.compile("_roadclouding_session=(.+)(;*)");
	    	Matcher mch = reg.matcher(cookie);
	    	if (mch.find()) {
	    		String s0 = mch.group(0);
	    		String s1 = mch.group(1);
	    		return s1;
	    	}
	    }
	    return null;
	}
	
	/** The state of this application (preferences....) */
	private static class State {
		public boolean mWifiConnection = false;
	}
	
	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
            try {
            	MKPoiInfoHelper mpi = (MKPoiInfoHelper) msg.getData().getSerializable(Constants.POI_SEARCH_RESULT);
            	Log.d(TAG, "get result from PoiSearch."+mpi.toString());
            	if (mpi.getSearchPlace().equals("home")) {
            		//set home addr
            		mUserProfile.setHomeAddr(mpi);
             	} else if (mpi.getSearchPlace().equals("office")) {
            		mUserProfile.setOfficeAddr(mpi);
             	}
            	Log.d(TAG, mUserProfile.toString());
            	mUserProfile.commitPreferences(mSP);
            	//update view
        		TextView txtHome = (TextView)findViewById(R.id.homeaddr);
        		txtHome.setText(mUserProfile.getHomeAddr().getName());
        		TextView txtOffice = (TextView)findViewById(R.id.officeaddr);
        		txtOffice.setText(mUserProfile.getOfficeAddr().getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    };
}