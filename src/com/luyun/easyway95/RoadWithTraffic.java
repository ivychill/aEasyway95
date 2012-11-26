package com.luyun.easyway95;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.baidu.mapapi.GeoPoint;
import com.luyun.easyway95.shared.TSSProtos.LYRoadTraffic;
import com.luyun.easyway95.shared.TSSProtos.LYSegmentTraffic;

/*
 * 用于接收从TSS发过来的RoadTraffic
 * 本类的实例用于接收HotRoad、FavoriteRoad
 * 本类有一个直接派生类：DrivingRoadWithTraffic：用于匹配DrivingRoute和RoadTraffic
 */
public class RoadWithTraffic {
	private String road;
	private Timestamp timestamp; //timestamp when last traffic received
	private String href;
	private ArrayList<LYSegmentTraffic> mSegmentTraffic; //received from air
	private boolean needClearSegment = false;
	
	long getTimestamp() {
		return this.timestamp.getTime();
	}
	
	public void setRoad(String rd) {
		road = rd;
	}
	
	public String getRoad() {
		return road;
	}
	
	public void setHref(String rid) {
		href = rid;
	}
	
	public String getHref() {
		return href;
	}
	
	ArrayList<LYSegmentTraffic> getSegments() {
    	refreshSegments();
		return mSegmentTraffic;
	}
	
	void refreshSegments() {
    	long nowTime = System.currentTimeMillis()/1000;
		if (needClearSegment == false) return;
		Iterator it=mSegmentTraffic.iterator();
		while (it.hasNext()) {
			LYSegmentTraffic st = (LYSegmentTraffic)it.next();
			if (st == null || ((nowTime-st.getTimestamp())/60)>Constants.TRAFFIC_LAST_DURATION) it.remove();
		}
		return;
	}
	
	void clearSegment(int i) {
		if (i < 0 || i > mSegmentTraffic.size()-1) return; //do nothing
		mSegmentTraffic.set(i, null);
		needClearSegment = true;
	}
	
	public RoadWithTraffic buildSegmentsFromAir(LYRoadTraffic rt) {
		this.road = new String(rt.getRoad());
		this.timestamp = new Timestamp(rt.getTimestamp());
		this.href = new String(rt.getHref());
		this.mSegmentTraffic = new ArrayList<LYSegmentTraffic>(rt.getSegmentTrafficsList());
		return this;
	}
	
}
