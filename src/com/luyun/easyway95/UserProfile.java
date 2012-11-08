package com.luyun.easyway95;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
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
	MKPoiInfoHelper mLastDestination;
	
	LinkedList<String> mRecentQuery;
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
	public void setLastDestination(MKPoiInfoHelper mpi) {
		mLastDestination = mpi;
	}
	public void setAndCommitLastDestination(SharedPreferences sp, MKPoiInfoHelper mpi) {
		mLastDestination = mpi;
		SharedPreferences.Editor ed = sp.edit();
		ed.putString("lastdest", mLastDestination.toString());
		ed.commit();
	}
	public MKPoiInfoHelper getLastDestination() {
		return mLastDestination;
	}
	public String getOfficeLatLng() {
		return mOfficeAddr.getLatLng();
	}
	public LinkedList<String> getRecentQuery() {
		return mRecentQuery;
	}
	public void setRecentQuery(LinkedList<String> recentQuery) {
		mRecentQuery = recentQuery;
	}
	public void addRecentQuery(String query) {
		mRecentQuery.remove(query);
		mRecentQuery.addFirst(query);
		if (mRecentQuery.size() >= Constants.MAX_RECENT_QUERY) {
			mRecentQuery.removeLast();
		}
		setRecentQuery(mRecentQuery);
		commitPreferences(mSP);
	}
	void updateFields(JSONObject jsonObj) {
		try {
			msUserName = jsonObj.getString("name");
			msEmail = jsonObj.getString("email");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	UserProfile() {
//		
//	}
	
	UserProfile(SharedPreferences sp) {
		mSP = sp;
		msUserName = sp.getString("UserName", null);
		msEmail = sp.getString("Email", null);
		msSessionId = sp.getString("SessionId", null);
		String poiShekou = String.format(
				"name=(蛇口港), address=(广东省深圳市南山区), city=(深圳), phoneNum=(), postCode=(), pt.lat=(22481722), pt.lng=(113919781), ePoiType=(0), searchPlace=()");
		mHomeAddr = new MKPoiInfoHelper(sp.getString("homeaddr", poiShekou));
		//mHomeAddr = new MKPoiInfoHelper("name=(aaaa), address=(bbbb), city=(cccc), phoneNum=(dddd), postCode=(ffff), pt.lat=(22.11), pt.lng=(130.11), ePoiType=(0), searchPlace=(ffff)");
		//设置缺省office地址为华为总部
		String poiHuawei = String.format(
				"name=(华为总部), address=(b654路;b666路;b667路;m342路空调;机场7线空调), city=(深圳), phoneNum=(), postCode=(), pt.lat=(22661034), pt.lng=(114064093), ePoiType=(1), searchPlace=()");
		mOfficeAddr = new MKPoiInfoHelper(sp.getString("officeaddr", poiHuawei));
		mLastDestination = new MKPoiInfoHelper(sp.getString("lastdest", poiHuawei));
		mRecentQuery = new LinkedList();
		int index = 0;
		String recentQuery;
		while ((recentQuery = sp.getString("RecentQuery"+index, null)) != null) {
			mRecentQuery.addLast(recentQuery);
			index++;
		}
		Log.d(TAG, "mRecentQuery: " + mRecentQuery);
	}
	
	public void commitPreferences(SharedPreferences sp) {
		SharedPreferences.Editor ed = sp.edit();
		ed.putString("UserName", msUserName);
		ed.putString("Email", msEmail);
		ed.putString("UserPassword", msUserPassword);
		ed.putString("homeaddr", mHomeAddr.toString());
		ed.putString("officeaddr", mOfficeAddr.toString());
		ed.putString("lastdest", mLastDestination.toString());
		for (int index = 0; index < mRecentQuery.size(); index++) {
			ed.putString("RecentQuery"+index, mRecentQuery.toArray(new String[0])[index]);
		}
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
		if (mbTokenLogon == true) return; //防止冲突重复
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

