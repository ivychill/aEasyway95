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
	
	public class MKPoiInfoHelper implements Serializable{ // helper for MKPoiInfo to be conveyed on air
		private String name;
		private String address;
		private String city;
		private String phoneNum;
		private String postCode;
		private GeoPoint pt;
		private int ePoiType;
		//private String searchKey;
		private String searchPlace;
		
		@Override
		public String toString() {
			String foramtedString = String.format(
					"name=(%s), address=(%s), city=(%s), phoneNum=(%s), postCode=(%s), pt.lat=(%d), pt.lng=(%d), ePoiType=(%d), searchPlace=(%s)", 
					name, address, city, phoneNum, postCode, pt.getLatitudeE6(), pt.getLongitudeE6(), ePoiType, searchPlace);
			return foramtedString;
		}
		
		MKPoiInfoHelper(MKPoiInfo mpi) {
			this.name = mpi.name;
			this.address = mpi.address;
			this.city = mpi.city;
			this.phoneNum = mpi.phoneNum;
			this.postCode = mpi.postCode;
			this.pt = mpi.pt;
			this.ePoiType = mpi.ePoiType;
		}

		MKPoiInfoHelper(MKAddrInfo mdi) {
			//对返回的结果进行正则表达式的处理
			//返回的结果形式为：广东省深圳市宝安区公园路1号
			String addr = mdi.strAddr;
			//首先获取省的信息
			String tmpStrings[] = addr.split("省");
			String province = "";
			String addrWithCity = addr;
			if (tmpStrings.length >= 2) {
				province = tmpStrings[0]+"省";
				addrWithCity = addr.replaceAll(province, "");
			}
			//然后获取市的信息
			tmpStrings = addrWithCity.split("市");
			String city = "";
			String addrWithDist = addrWithCity;
			if (tmpStrings.length >= 2) {
				city = tmpStrings[0]+"市";
				addrWithDist = addrWithCity.replaceAll(city, "");
			}
			//然后获取区的信息
			tmpStrings = addrWithDist.split("区");
			String dist = "";
			String addrWithStreet = addrWithDist;
			if (tmpStrings.length >= 2) {
				dist = tmpStrings[0]+"区";
				addrWithStreet = addrWithStreet.replaceAll(dist, "");
			}
			
			this.name = addrWithStreet;
			this.address = "";
			this.city = city;
			this.phoneNum = "";
			this.postCode = "";
			this.pt = mdi.geoPt;
			//this.ePoiType = ;
		}

		MKPoiInfoHelper() {
			this.name = "";
			this.address = "";
			this.city = "";
			this.phoneNum = "";
			this.postCode = "";
			this.pt = new GeoPoint((int) (22.551541 * 1E6), (int) (113.94750 * 1E6));
			this.ePoiType = 0;
			this.searchPlace = "";
		}
		
		//constructor for formatedString (result of toString)
		MKPoiInfoHelper(String strMPI) {
			this.name = "";
			this.address = "";
			this.city = "";
			this.phoneNum = "";
			this.postCode = "";
			this.pt = new GeoPoint((int) (22.551541 * 1E6), (int) (113.94750 * 1E6));
			this.ePoiType = 0;
			this.searchPlace = "";
			
			if (strMPI == null) return;
			//正则表达式的非贪婪匹配 http://www.wasw100.com/java/java.util.regex/Greedy.html
			Pattern p = Pattern.compile("name=\\((.*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			Matcher m = p.matcher(strMPI);
			if (m.find()) {
				this.name = m.group(1);
			}
			p = Pattern.compile("address=\\((.*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				this.address = m.group(1);
				//Log.d(TAG, m.group(1));
			}
			p = Pattern.compile("city=\\((.*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				this.city = m.group(1);
				//Log.d(TAG, m.group(1));
			}
			p = Pattern.compile("phoneNum=\\((.*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				this.phoneNum = m.group(1);
				//Log.d(TAG, m.group(1));
			}
			p = Pattern.compile("postCode=\\((.*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				//Log.d(TAG, m.group(1));
				this.postCode = m.group(1);
			}
			p = Pattern.compile("ePoiType=\\((\\d*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				//Log.d(TAG, m.group(1));
				this.ePoiType = Integer.parseInt(m.group(1));
			}
			p = Pattern.compile("pt\\.lat=\\((\\d*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				//Log.d(TAG, m.group(1));
				if (this.pt == null) {
					this.pt = new GeoPoint((int) (22.551541 * 1E6), (int) (113.94750 * 1E6));
				}
				this.pt.setLatitudeE6(Integer.parseInt(m.group(1)));
			}
			p = Pattern.compile("pt\\.lng=\\((\\d*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				//Log.d(TAG, m.group(1));
				if (this.pt == null) {
					this.pt = new GeoPoint((int) (22.551541 * 1E6), (int) (113.94750 * 1E6));
				}
				this.pt.setLongitudeE6(Integer.parseInt(m.group(1)));
			}
		}
		
		public String getName() {
			return this.name;
		}
		public void setName(String nm) {
			this.name = nm;
		}
		public String getAddress() {
			return this.address;
		}
		public void setAddress(String addr) {
			this.address = addr;
		}
		public String getCity() {
			return this.city;
		}
		public void setCity(String city) {
			this.city = city;
		}
		public String getPhoneNum() {
			return this.phoneNum;
		}
		public void setPhoneNum(String pn) {
			this.phoneNum = pn;
		}
		public String getPostCode() {
			return this.postCode;
		}
		public void setPostCode(String pc) {
			this.postCode = pc;
		}
		public int getEPoiType() {
			return this.ePoiType;
		}
		public void setEPoiType(int pt) {
			this.ePoiType = pt;
		}
		public GeoPoint getPt() {
			return this.pt;
		}
		public void setPt(GeoPoint pt) {
			this.pt = pt;
		}
		public void setSearchPlace(String sp) {
			this.searchPlace = sp;
		}
		public String getSearchPlace() {
			return this.searchPlace;
		}
		public String getLatLng() {
			String foramtedString = String.format(
					"(%d, %d)", 
					pt.getLatitudeE6(), pt.getLongitudeE6());
			return foramtedString;
		}
	}
}

