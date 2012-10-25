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
    Weibo m_weibo;
	
	private String msSessionId;
	
	public WBEntry (LYNavigator activity) {
		mainActivity = activity;
	    m_weibo = Weibo.getInstance();
	    m_weibo.setupConsumerConfig(Constants.WEIBO_CONSUMER_KEY, Constants.WEIBO_CONSUMER_SECRET);
	    m_weibo.setRedirectUrl(Constants.WEIBO_REDIRECT_URL);
	}
	
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
}