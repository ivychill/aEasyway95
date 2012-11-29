package com.luyun.easyway95;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TabHost;

public class ShowTraffics extends TabActivity {
	// VERBOSE debug log is complied in but stripped at runtime
	//private static final String TAG = "ShowTraffics";
	private TabHost myTabhost;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		
        myTabhost=this.getTabHost();       
        LayoutInflater.from(this).inflate(R.layout.showtraffic, myTabhost.getTabContentView(), true);
        myTabhost.setBackgroundColor(Color.argb(150, 22, 70, 150));
        
        myTabhost.addTab(myTabhost.newTabSpec("TrafficsOfRoute")
                .setIndicator("途经路况",
                        getResources().getDrawable(R.drawable.slow_speed))
                        .setContent(new Intent(this, TrafficsOfRoute.class)));
        myTabhost.addTab(myTabhost.newTabSpec("TrafficsCaring")
                .setIndicator("可能还关注",
                        getResources().getDrawable(R.drawable.slow_speed))
                        .setContent(new Intent(this, TrafficsCaring.class)));
        
        Intent intent = getIntent();
        if(intent.getBooleanExtra("cronpub", false)){
        	myTabhost.setCurrentTabByTag("TrafficsCaring");	
        }
		
	}

}