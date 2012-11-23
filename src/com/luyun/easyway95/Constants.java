package com.luyun.easyway95;

public class Constants {
	public static final String SHARED_SECRET="LYUN";
	public static final String RELEASE_VERSION = "2.0";
	public static final int POISEARCH=1000;
	
	public static final int ERROR=1001;
	public static final int FIRST_LOCATION=1002;
	
	public static final String ZMQ_QUITTING_CMD = "QT";
	public static final String ZMQ_RECONNECT_CMD = "RC";
	
	public static final int INTRODUCTION_PAGE_NUMBER = 5;
	
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
	public static final int DLG_TIME_OUT=5601;	//��Ϣ
	public static final int DLG_LAST_DURATION=12000;	//�Ի�����ڵ�ʱ��
	public static final int RESET_MAP_TIME_OUT=5701;	
	public static final int RESET_MAP_INTERVAL=60000;	
	public static final int PROMPT_WATCH_DOG=5801;	
	public static final int PROMPT_WATCH_DOG_INTERVAL=60000;	

	public static final int WAITING_TRACKEE_LOC=5500;
	public static final int GOT_TRACKEE_LOC=5501;

	public static final int BUSLINE_RESULT=6000;
	public static final int BUSLINE_DETAIL_RESULT=6001;	

	public static final int TRACKER_SERVER_PORT=8007; 
	public static final int TRACKEE_SERVER_PORT=8008;	
	public static final String TRACKER_SERVER_HOST="42.121.18.140";	
	public static final String USERS_PROFILE_URL="http://www.roadclouding.com/users/profile";
	public static final String PROMOTION_URL="http://www.roadclouding.com/commerce";
	public static final String FAQ_URL="http://www.roadclouding.com/faq";
	public static final String DOWNLOAD_URL="http://www.roadclouding.com/download/easyway.apk";	
//	public static final String DOWNLOAD_URL="market://search?q=pname:com.luyun.easyway95";
	//public static final String USERS_PROFILE_URL="http://172.16.0.33:3000/users/profile";
	
	//weibo by chenfeng
//	public static final String POST_WEIBO_URL="http://www.roadclouding.com/weibos/post";
	public static final String GET_WEIBO_URL="http://www.roadclouding.com/weibos/get";
	public static final String WEIBO_URL_OAUTH2="https://api.weibo.com";
	//·����ҳ
//	public static final String WEIBO_CONSUMER_KEY = "3480490775";
//	public static final String WEIBO_CONSUMER_SECRET = "876dd843606f5d99d86e716cc1c69264";
	//·��
	public static final String WEIBO_CONSUMER_KEY = "1443429908";
	public static final String WEIBO_CONSUMER_SECRET = "7c3e7ab52eaa67644b49d923a837c01c";
	public static final String WEIBO_REDIRECT_URL = "http://www.roadclouding.com/users/auth/weibo/callback";
	
	//wexin by chenfeng
//	public static final String WEIXIN_APP_ID = "wx9facda03786fc8af";	//����
	public static final String WEIXIN_APP_ID = "wxd69fbd18ca12e5f3";	//����
	public static final String SHARE_MESSAGE = "������·��95 http://www.roadclouding.com";
	
	public static final String POI_RETURN_KEY = "poiInfo";
//	public static final String HOME_RETURN_KEY = "homePoi";
//	public static final String OFFICE_RETURN_KEY = "homePoi";
	public static final int SETTING_REQUEST_CODE = 1887;
	public static final int ENDPOINT_REQUEST_CODE = 1888;
	public static final int HOME_REQUEST_CODE = 1889;
	public static final int OFFICE_REQUEST_CODE = 1890;
	public static final int MAX_RECENT_QUERY = 20;

//	public static final int TSS_SERVER_PORT=7001; 
	public static final int TSS_SERVER_PORT=6001; 
	public static final String TSS_DEV_HOST="172.16.0.100";	
//	public static final String TSS_PRO_HOST="www.roadclouding.com";	 
	public static final String TSS_PRO_HOST="www.roadclouding.com";	 
	//public static final String TSS_PRO_HOST="42.121.99.247";	
	//public static final String TSS_PRO_HOST="172.16.0.100";	
	
	public static final String TRAFFIC_UPDATE="TrafficUpdate";	
	public static final int TRAFFIC_UPDATE_CMD=9900;
	
	public static final int CHKPNT_OF_DISTANCE=10; //�������·����ܱ�·����ѯ����λKM
	public static final int CHKPNT_OF_TRAFFIC=1; //���ڼ���Ƿ�ӽ���һ��ӵ�µ㣬��λKM
	public static final double DISTANCE_OFF_ROAD=180.0; //��ʶ�Ƿ�ƫ�뺽��,��λM
	public static final double MIN_CHK_DISTANCE=20.0; //��ʶ�Ƿ�ƫ�뺽��,��λM
	public static final int INTERVAL_FORCE_UPDATE_LOCATION=120000; //����ǿ��λ�ø��£���λ���룬�������5��������
	public static final int TRAFFIC_LAST_DURATION=6; //·������ĵ�ʱ�䣬��λ����
	
	public static final double DOUBLE_1E6 = 1000000.0;
	
	public static final String ROAD_AHEAD="ǰ��"; //
	public static final String NO_TRAFFIC="��ӵ��"; //
	public static final String TRAFFIC_JAM_LVL_HIGH="����ӵ��"; //<6KM
	public static final int TRAFFIC_JAM_LVL_HIGH_SPD=6;
	public static final String TRAFFIC_JAM_LVL_MIDDLE="ӵ��"; //[6km, 20km)
	public static final int TRAFFIC_JAM_LVL_MIDDLE_SPD=20;
	public static final String TRAFFIC_JAM_LVL_LOW="�೵����"; //[20km, 
	
	public static final int TRAFFIC_POPUP=0x555;
	public static final int INTERNET_CONNECTION=0x666;
	
	public static final long INTERVAL_OF_TRAFFIC_REPORT = 5; //�ϱ�·�����ʱ��
	public static final int SHENZHEN_CITY_ID = 340; //�ٶȶ�������ID
	public static final int MAX_PUSH_LEN = 240; //�ٶȶ�������ID
}
