package com.luyun.easyway95;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKPoiInfo;

public class UserProfile {
	private static final String TAG = "UserProfile";
	private String msUserName;
	private String msUserPassword;
	private String msNickName;
	private String msEmail;
	private boolean mbTokenLogon = false;
	private boolean mbSessionLogon = false;
	private String msAuthToken;
	private String msSessionId;
	private String msProfileURL;
	private SharedPreferences mSP;
	
	MKPoiInfoHelper mHomeAddr;
	MKPoiInfoHelper mOfficeAddr;
	MKRouteHelper mRouteHome2Office;
	MKRouteHelper mRouteOffice2Home;
	
	public void setEmail(String email) {
		msEmail = email;
	}
	public String getEmail() {
		return msEmail;
	}
	public void setUserName(String name) {
		msUserName = name;
	}
	public String getUserName() {
		return msUserName;
	}
	public void setNickName(String nickname) {
		msNickName = nickname;
	}
	public String getNickName() {
		return msNickName;
	}
	public void setUserPassword(String password) {
		msUserPassword = password;
	}
	public String getUserPassword() {
		return msUserPassword;
	}
	public MKPoiInfoHelper getHomeAddr() {
		return mHomeAddr;
	}
	public String getHomeLatLng() {
		return mHomeAddr.getLatLng();
	}
	public void setHomeAddr(MKPoiInfoHelper mpi) {
		mHomeAddr = mpi;
	}
	public MKPoiInfoHelper getOfficeAddr() {
		return mOfficeAddr;
	}
	public void setOfficeAddr(MKPoiInfoHelper mpi) {
		mOfficeAddr = mpi;
	}
	public String getOfficeLatLng() {
		return mOfficeAddr.getLatLng();
	}
	
	void updateFields(JSONObject jsonObj) {
		try {
			msUserName = jsonObj.getString("name");
			msEmail = jsonObj.getString("email");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	UserProfile() {
		
	}
	
	UserProfile(SharedPreferences sp) {
		mSP = sp;
		msUserName = sp.getString("UserName", null);
		msEmail = sp.getString("Email", null);
		msSessionId = sp.getString("SessionId", null);
		mHomeAddr = new MKPoiInfoHelper(sp.getString("homeaddr", null));
		//mHomeAddr = new MKPoiInfoHelper("name=(aaaa), address=(bbbb), city=(cccc), phoneNum=(dddd), postCode=(ffff), pt.lat=(22.11), pt.lng=(130.11), ePoiType=(0), searchPlace=(ffff)");
		mOfficeAddr = new MKPoiInfoHelper(sp.getString("officeaddr", null));
	}
	
	public void commitPreferences(SharedPreferences sp) {
		SharedPreferences.Editor ed = sp.edit();
		ed.putString("UserName", msUserName);
		ed.putString("Email", msEmail);
		ed.putString("UserPassword", msUserPassword);
		ed.putString("homeaddr", mHomeAddr.toString());
		ed.putString("officeaddr", mOfficeAddr.toString());
		ed.commit();
	}
	
	public String toString() {
		String strResult = String.format("UserName=<%s>, Email=<%s>, Home=<%s>, Office=<%s>", msUserName, msEmail, 
								mHomeAddr.toString(), mOfficeAddr.toString());
		return strResult;
	}
	
	public void setAuthToken(String token) {
		msAuthToken = token;
		mbTokenLogon = true;
        msProfileURL = Constants.USERS_PROFILE_URL+"?auth_token="+msAuthToken;
	}
	
	public void setSessionId(String id) {
		if (mbTokenLogon == true) return; //∑¿÷π≥ÂÕª÷ÿ∏¥
		msSessionId = id;
		mbSessionLogon = true;
		msProfileURL = Constants.USERS_PROFILE_URL;
	}
	
	public boolean isUserLogon() {
		if (mbTokenLogon || mbSessionLogon) return true;
		return false;
	}
	
	public void getProfileFromSvr() {
        //ClientResource cr = new ClientResource(Constants.USERS_PROFILE_URL);
        ClientResource cr = new ClientResource(msProfileURL);
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
			            	updateFields(jsonObject);
			            	commitPreferences(mSP);
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

