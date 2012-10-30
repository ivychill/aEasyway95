package com.luyun.easyway95;

import java.io.Serializable;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MKPoiInfo;

//public class MKPoiInfoSerialable extends MKPoiInfo implements Serializable {
public class MKPoiInfoSerialable implements Serializable {
//	MKPoiInfo mMKPoiInfo;
//	public MKPoiInfoSerialable(MKPoiInfo poiInfo) {
//		mMKPoiInfo = poiInfo;
//	}
	
	String address;
	String city;
	int ePoiType;
	String name;
	String phoneNum;
	String postCode;
	int latitudeE6;
	int longitudeE6;
	
	public void copyFrom (MKPoiInfo poiInfo) {
		address = poiInfo.address;
		city = poiInfo.city;
		ePoiType = poiInfo.ePoiType;
		name = poiInfo.name;
		phoneNum = poiInfo.phoneNum;
		postCode = poiInfo.postCode;
		latitudeE6 = poiInfo.pt.getLatitudeE6();
		longitudeE6 = poiInfo.pt.getLongitudeE6();
	}
}
