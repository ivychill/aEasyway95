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

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;

public class SettingActivity extends Activity {
	// VERBOSE debug log is complied in but stripped at runtime
	private static final String TAG = "SettingActivity";
	private String msAuthToken;
	private String msSessionId;
	private boolean mbTokenLogon = false;
	private boolean mbSessionLogon = false;
	private UserProfile mUserProfile;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		
		setContentView(R.layout.setting);
		msAuthToken = retrieveAuthToken();
		msSessionId = retrieveSessionToken();
		Log.d(TAG, "kkkk");
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
		Log.d(TAG, "ffff");
		if (mbTokenLogon || mbSessionLogon) {
	        Button btn = (Button)findViewById(R.id.button1);
	        btn.setText("ÒÑµÇÂ¼");
	        
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
				            JSONObject object = rep.getJsonObject();
				            if (object != null) {
				            	Log.d(TAG, object.toString());
				            	mUserProfile = new UserProfile(object);
				            	Log.d(TAG, mUserProfile.toString());
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
}