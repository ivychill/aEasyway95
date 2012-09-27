package com.luyun.easyway95;

public class Constants {
	public static final int POISEARCH=1000;
	
	public static final int ERROR=1001;
	public static final int FIRST_LOCATION=1002;
	
	public static final int ROUTE_START_SEARCH=2000;//路径规划起点搜索
	public static final int ROUTE_END_SEARCH=2001;//路径规划起点搜索
	public static final int ROUTE_SEARCH_RESULT=2002;//路径规划结果
	public static final int ROUTE_SEARCH_ERROR=2004;//路径规划起起始点搜索异常	
	public static final String POI_START_SEARCH="2005";//POI
	public static final String POI_SEARCH_RESULT="2006";//POI结果
	
	public static final int REOCODER_RESULT=3000;//地理编码结果
	public static final int DIALOG_LAYER=4000;
	public static final int POISEARCH_NEXT=5000;
	
	public static final int SYNTHESIZE_ONGOING=5500;	
	public static final int SYNTHESIZE_DONE=5501;	
	public static final int DLG_TIME_OUT=5601;	
	public static final int RESET_MAP_TIME_OUT=5601;	

	public static final int WAITING_TRACKEE_LOC=5500;
	public static final int GOT_TRACKEE_LOC=5501;

	public static final int BUSLINE_RESULT=6000;
	public static final int BUSLINE_DETAIL_RESULT=6001;	

	public static final int TRACKER_SERVER_PORT=8007; 
	public static final int TRACKEE_SERVER_PORT=8008;	
	public static final String TRACKER_SERVER_HOST="42.121.18.140";	
	public static final String USERS_PROFILE_URL="http://www.roadclouding.com/users/profile";	
	//public static final String USERS_PROFILE_URL="http://172.16.0.33:3000/users/profile";	

	public static final int TSS_SERVER_PORT=7001; 
	public static final String TSS_DEV_HOST="172.16.0.100";	
	//public static final String TSS_PRO_HOST="www.roadclouding.com";	
	public static final String TSS_PRO_HOST="172.16.0.100";	
	
	public static final String TRAFFIC_UPDATE="TrafficUpdate";	
	public static final int TRAFFIC_UPDATE_CMD=9900;	
	
	public static final int CHKPNT_OF_DISTANCE=10; //用于重新发起周边路况查询，单位KM
	public static final int CHKPNT_OF_TRAFFIC=1; //用于检查是否接近下一个拥堵点，单位KM
	public static final double DISTANCE_OFF_ROAD=180.0; //标识是否偏离航线,单位M
	public static final double MIN_CHK_DISTANCE=20.0; //标识是否偏离航线,单位M
	public static final int INTERVAL_FORCE_UPDATE_LOCATION=120000; //用于强制位置更新，单位毫秒，生产环境建议5分钟以上
	
	public static final double DOUBLE_1E6 = 1000000.0;
	
	public static final String NO_TRAFFIC_AHEAD="前方无拥堵"; //
	public static final String TRAFFIC_JAM_LVL_HIGH="基本不动"; //<6KM
	public static final String TRAFFIC_JAM_LVL_MIDDLE="重度拥堵"; //[6km, 15km)
	public static final String TRAFFIC_JAM_LVL_LOW="轻度拥堵"; //[15km, 
	
	public static final int TRAFFIC_POPUP=0x555;
	public static final int INTERNET_CONNECTION=0x666;
}
