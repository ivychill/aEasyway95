package com.luyun.easyway95.weibo;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import com.luyun.easyway95.Constants;
import com.luyun.easyway95.LYNavigator;
import com.weibo.net.AccessToken;
import com.weibo.net.DialogError;
import com.weibo.net.Oauth2AccessTokenHeader;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

public class WBEntry {
	private LYNavigator mainActivity;
	private static final String TAG = "WBEntry";
	private boolean mbSessionLogon = false;
	private boolean mbWeiboLogon = false;
	CookieSyncManager mCookieSyncManager;
    CookieManager mCookieManager;
    Weibo m_weibo;
    WeiboToken m_weiboToken;
    boolean m_tokenValid = false;
	
	private String msSessionId;
	
	public WBEntry (LYNavigator activity) {
		mainActivity = activity;
		mCookieSyncManager = CookieSyncManager.createInstance(mainActivity);
	    mCookieManager = CookieManager.getInstance();
	    m_weibo = Weibo.getInstance();
	    m_weibo.setupConsumerConfig(Constants.WEIBO_CONSUMER_KEY, Constants.WEIBO_CONSUMER_SECRET);
	    m_weibo.setRedirectUrl(Constants.WEIBO_REDIRECT_URL);
    	m_weiboToken = new WeiboToken();
	}
	
	public boolean sendWeibo() {
		retrieveSessionToken();
        ClientResource cr = new ClientResource(Constants.GET_WEIBO_URL);
        if (mbSessionLogon) {
	        Series<Cookie> cookies = cr.getCookies();
	        cookies.add("_roadclouding_session", msSessionId);
	        cr.setCookies(cookies);
	        Log.d(TAG, cookies.toString());
        }
        
//	    String cookie = mCookieManager.getCookie(Constants.WEIBO_URL_OAUTH2);
//	    if (cookie != null) {
//	    	Log.d(TAG, "weibo cookie: " + cookie);
//	    }

		// Set the callback object invoked when the response is received.
		cr.setOnResponse(new Uniform() {
		    public void handle(Request request, Response response) {
		    	m_tokenValid = m_weiboToken.tokenValid(response);
		    	Log.d(TAG, "response from m_tokenValid: " + m_tokenValid);
		    	if (m_tokenValid)
		    	{
    		        Utility.setAuthorization(new Oauth2AccessTokenHeader());
    				AccessToken accessToken = new AccessToken(m_weiboToken.getAccessToken(), Constants.WEIBO_CONSUMER_SECRET);
    				accessToken.setExpiresIn(m_weiboToken.getExpiresAt());
    				m_weibo.setAccessToken(accessToken);
    		        share2weibo();
		    	}
		    	else
    			{
    				Log.w(TAG, "access token expires");
//    				new AlertDialog.Builder(mainActivity)//Context
//    				.setTitle("无效新浪微博")
//    				.setIcon(android.R.drawable.ic_dialog_alert)//图标
//    				.setMessage("请用新浪微博帐号登录")
//    				.setPositiveButton("确定", null).show();//显示
    			}
			}
		});

		cr.get(MediaType.APPLICATION_JSON);
		Log.d(TAG, "return m_tokenValid: " + m_tokenValid);
		return m_tokenValid;
	}
	
	//从roadclouding server获得的weibo的access token
	class WeiboToken
	{
		String m_accessToken;
		int m_expiresAt;		//in seconds
		boolean m_willExpire;
		
		public String getAccessToken()
		{
			return m_accessToken;
		}
		
		public int getExpiresAt()
		{
			return m_expiresAt;
		}
		//能得到有效值的情况下返回true，其它都返回false
	    public boolean tokenValid(Response response)
	    {
	        try {
	        	JsonRepresentation rep = new JsonRepresentation(response.getEntity());
	        	try {
	                JSONObject jsonObject = rep.getJsonObject();
	                if (jsonObject != null) {
	                	Log.d(TAG, jsonObject.toString());
	                	m_accessToken = jsonObject.getString("visit_token");
	                	m_expiresAt = jsonObject.getInt("expires_at");
	            		m_willExpire = jsonObject.getBoolean("will_expire");
	            		Date date = new Date();
	            		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            		Log.d(TAG, "now: " + dateFormat.format(date));
	            		String expiresAsText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(m_expiresAt * 1000L));
	            		Log.d(TAG, "expires: " + expiresAsText);
	            		int now = (int)(date.getTime())/1000;
	            		return !(m_willExpire && now >= m_expiresAt);
	                }
	                else
	                {
	                	return false;
	                }
	        	}catch(Exception e) {
	        		e.printStackTrace();
	        	}
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			return false;
	    }

	}
	
	//如果用户没有通过roadclouding server登录，则需用调用此函数从客户端登录
	public void authorize()
	{
		m_weibo.authorize(mainActivity, new AuthDialogListener());
	}
	
	class AuthDialogListener implements WeiboDialogListener {

		@Override
		public void onComplete(Bundle values) {
			Log.d(TAG, "on authorize complete:" + values);
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			Log.d(TAG, "access_token : " + token + "  expires_in: " + expires_in);
			AccessToken accessToken = new AccessToken(token, Constants.WEIBO_CONSUMER_SECRET);
			accessToken.setExpiresIn(expires_in);
			m_weibo.setAccessToken(accessToken);
			share2weibo();
		}

		@Override
		public void onError(DialogError e) {
			Toast.makeText(mainActivity.getApplicationContext(),
					"Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCancel() {
			Toast.makeText(mainActivity.getApplicationContext(), "Auth cancel",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			Toast.makeText(mainActivity.getApplicationContext(),
					"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
					.show();
		}
	}
    
	private void share2weibo()
	{
	    File file = Environment.getExternalStorageDirectory();
	    String sdPath = file.getAbsolutePath();
	    // 请保证SD卡根目录下有这张图片文件
	    String picPath = sdPath + "/" + "logo.jpg";
	    File picFile = new File(picPath);
	    if (!picFile.exists()) {
	        //Toast.makeText(WBEntryActivity.this, "图片" + picPath + "不存在！", Toast.LENGTH_SHORT).show();
	        picPath = null;
	    }

	    try {
//	        share2weibo(Constants.SHARE_MESSAGE, picPath);
	        m_weibo.share2weibo(mainActivity, m_weibo.getAccessToken().getToken(), m_weibo.getAccessToken().getSecret(), Constants.SHARE_MESSAGE, picPath);
	    } catch (WeiboException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } finally {

	    }
	}
    
//    private void share2weibo(String content, String picPath) throws WeiboException {
//        Weibo weibo = Weibo.getInstance();
//        weibo.share2weibo(mainActivity, weibo.getAccessToken().getToken(), weibo.getAccessToken().getSecret(), content, picPath);
//    }

	/** get the stored session tokens cookies */
	private void retrieveSessionToken() {

	    String cookie = mCookieManager.getCookie(Constants.GET_WEIBO_URL);
	    if (cookie != null) {
	    	Log.d(TAG, cookie);
	    	Pattern reg = Pattern.compile("_roadclouding_session=(.+)(;*)");
	    	Matcher mch = reg.matcher(cookie);
	    	if (mch.find()) {
	    		String s0 = mch.group(0);
	    		String s1 = mch.group(1);
	    		mbSessionLogon = true;
	    		msSessionId = s1;
	    		return;
	    	}
	    }
	}
	
}
