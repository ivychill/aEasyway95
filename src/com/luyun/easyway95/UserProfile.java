package com.luyun.easyway95;

import org.json.JSONObject;

public class UserProfile {
	private String msUserName;
	private String msUserPassword;
	private String msNickName;
	private String msEmail;
	
	public void setEmail(String email) {
		msEmail = email;
	}
	public void setUserName(String name) {
		msUserName = name;
	}
	public void setNickName(String nickname) {
		msNickName = nickname;
	}
	public void setUserPassword(String password) {
		msUserPassword = password;
	}
	
	UserProfile(JSONObject jsonObj) {
		try {
			msUserName = jsonObj.getString("name");
			msEmail = jsonObj.getString("email");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		String strResult = "";
		if (msUserName != null) strResult = "UserName: "+msUserName;
		if (msEmail != null) strResult = strResult + ";email: "+msEmail;
		return strResult;
	}
}

