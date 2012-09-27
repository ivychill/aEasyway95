package com.luyun.easyway95;

import com.baidu.mapapi.GeoPoint;

public final class TrafficPoint {
	private String road;
	private String desc;
	private GeoPoint point;
	private double distance;
	
	public void setRoad(String rd) {
		road = rd;
	}
	public void setDesc(String str) {
		desc = str;
	}
	public String getRoad(){
		return road;
	}
	public String getDesc() {
		return desc;
	}
	public void setPoint(GeoPoint pt) {
		point = pt;
	}
	public GeoPoint getPoint() {
		return point;
	}
	public void setDistance(double dist) {
		distance = dist;
	}
	public double getDistance() {
		return distance;
	}
}
