package com.luyun.easyway95;

public class Constants {
	public static final int POISEARCH=1000;
	
	public static final int ERROR=1001;
	public static final int FIRST_LOCATION=1002;
	
	public static final int ROUTE_START_SEARCH=2000;//·���滮�������
	public static final int ROUTE_END_SEARCH=2001;//·���滮�������
	public static final int ROUTE_SEARCH_RESULT=2002;//·���滮���
	public static final int ROUTE_SEARCH_ERROR=2004;//·���滮����ʼ�������쳣	
	public static final String POI_START_SEARCH="2005";//POI
	public static final String POI_SEARCH_RESULT="2006";//POI���
	
	public static final int REOCODER_RESULT=3000;//���������
	public static final int DIALOG_LAYER=4000;
	public static final int POISEARCH_NEXT=5000;
	
	public static final int SYNTHESIZE_ONGOING=5500;	
	public static final int SYNTHESIZE_DONE=5501;	

	public static final int WAITING_TRACKEE_LOC=5500;
	public static final int GOT_TRACKEE_LOC=5501;

	public static final int BUSLINE_RESULT=6000;
	public static final int BUSLINE_DETAIL_RESULT=6001;	

	public static final int TRACKER_SERVER_PORT=8007; 
	public static final int TRACKEE_SERVER_PORT=8008;	
	public static final String TRACKER_SERVER_HOST="42.121.18.140";	
	public static final String USERS_PROFILE_URL="http://www.roadclouding.com/users/profile";	
	//public static final String USERS_PROFILE_URL="http://172.16.0.33:3000/users/profile";	

	public static final int TSS_SERVER_PORT=6001; 
	public static final String TSS_DEV_HOST="172.16.0.100";	
	public static final String TSS_PRO_HOST="42.121.18.140";	
	
	public static final String TRAFFIC_UPDATE="TrafficUpdate";	
	public static final int TRAFFIC_UPDATE_CMD=9900;	
	
	public static final int CHKPNT_OF_DISTANCE=10; //�������·����ܱ�·����ѯ����λKM
	public static final int CHKPNT_OF_TRAFFIC=1; //���ڼ���Ƿ�ӽ���һ��ӵ�µ㣬��λKM
	public static final double DISTANCE_OFF_ROAD=200.0; //��ʶ�Ƿ�ƫ�뺽��,��λM
}
