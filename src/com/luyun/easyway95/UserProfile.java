package com.luyun.easyway95;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKPoiInfo;
import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;

public class UserProfile implements Serializable{
	private static final String TAG = "UserProfile";
	private String msUserName;
	private String msUserPassword;
	private String msNickName;
	private String msEmail;
	
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
	public void setHomeAddr(MKPoiInfoHelper mpi) {
		mHomeAddr = mpi;
	}
	public MKPoiInfoHelper getOfficeAddr() {
		return mOfficeAddr;
	}
	public void setOfficeAddr(MKPoiInfoHelper mpi) {
		mOfficeAddr = mpi;
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
		msUserName = sp.getString("UserName", null);
		msEmail = sp.getString("Email", null);
		msUserPassword = sp.getString("UserPassword", null);
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
		String strResult = new String().format("UserName=<%s>, Email=<%s>, Home=<%s>, Office=<%s>", msUserName, msEmail, 
								mHomeAddr.toString(), mOfficeAddr.toString());
		return strResult;
	}
	
    public class MKStepHelper implements Serializable{
    	MKStepHelper (MKStep mks) {
    		
    	}
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
			String foramtedString = new String().format(
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
			p = Pattern.compile("pt.lat=\\((\\d*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
			m = p.matcher(strMPI);
			if (m.find()) {
				//Log.d(TAG, m.group(1));
				if (this.pt == null) {
					this.pt = new GeoPoint((int) (22.551541 * 1E6), (int) (113.94750 * 1E6));
				}
				this.pt.setLatitudeE6(Integer.parseInt(m.group(1)));
			}
			p = Pattern.compile("pt.lng=\\((\\d*?)\\)", Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE);
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
	}
}

