package com.luyun.easyway95;

import com.baidu.mapapi.GeoPoint;

public final class TrafficPoint {
	private String road;
	private String desc;
	private GeoPoint point;
	private int speed;
	private double distance;
	
	TrafficPoint(TrafficPoint pt) {
		if (pt == null) {
			return;
		}
		this.road = pt.getRoad();
		this.desc = pt.getDesc();
		this.point = pt.getPoint();
		this.speed = pt.getSpeed();
		this.distance = pt.getDistance();
	}
	TrafficPoint () {
		road = null;
		desc = null;
		point = null;
		distance = 0.0;
		speed = 0;
	}
	public void setSpeed(int spd) {
		speed = spd;
	}
	
	public int getSpeed() {
		return speed;
	}
	
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
