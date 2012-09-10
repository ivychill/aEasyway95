package com.luyun.easyway95;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	// VERBOSE debug log is complied in but stripped at runtime
	private static final String TAG = "LoginActivity";

	private static final int INTERNET_DIALOG = 0;
	private static final int LOGIN_DIALOG = 1;
	private static final int ERROR_DIALOG = 2;
	private static final String LOGIN_URL = "http://2.easilocation.appspot.com/signIn";
	
	private State mState;
	private EditText username;
	private EditText password;
	private SharedPreferences mSharedPreferences;
	
	private AlertDialog internetDialog;
	private ProgressDialog loginDialog;
	private AlertDialog errorDialog;
	
	private WebView mwvLogin;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
		
		setContentView(R.layout.login);
		
		CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
	    CookieManager cookieManager = CookieManager.getInstance();
	    String cookie = cookieManager.getCookie(Constants.USERS_PROFILE_URL);
	    if (cookie != null)
	    	Log.d(TAG, cookie);

		mwvLogin = (WebView)findViewById(R.id.wv);
		mwvLogin.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				LoginActivity.this.setProgress(newProgress*100);
			}
		});
		
		mwvLogin.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(LoginActivity.this, "Sorry!"+description, Toast.LENGTH_SHORT).show();
			}

			public void onPageFinished(WebView webView, String url){
				Log.d(TAG, "finished loading "+url+"ddddd");
				String strProfile = "http://www.roadclouding.com/users/edit";
				if (url.equals(strProfile)) {
					Log.d(TAG, "Finished login!");
					LoginActivity.this.finish();
				}
			}  
		});
		
		mwvLogin.loadUrl(Constants.USERS_PROFILE_URL);
	}
	
	/**
	 * @return boolean return true if the application can access the internet
	 */
	private boolean isConnectedToInternet() {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		if (info == null) {
			return false;
		} else if (!info.isConnected()) {
			return false;
		} else if (info.getType() != ConnectivityManager.TYPE_WIFI) {
			return false;
		} 
		return true;
	}
	
	/** The state of this application (preferences....) */
	private static class State {
		public boolean mWifiConnection = false;
	}
}