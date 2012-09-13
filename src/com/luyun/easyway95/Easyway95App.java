package com.luyun.easyway95;

import android.app.Activity;
import android.app.Application;

public class Easyway95App extends Application {
	private Activity mainActivity;
	
	public void setMainActivity(Activity act) {
		mainActivity = act;
	}
	public Activity getMainActivity() {
		return mainActivity;
	}
}
