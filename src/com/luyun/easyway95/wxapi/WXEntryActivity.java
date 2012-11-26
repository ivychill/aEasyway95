package com.luyun.easyway95.wxapi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luyun.easyway95.Constants;
import com.luyun.easyway95.R;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.GetMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.ShowMessageFromWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
	final static String TAG = "WXEntryActivity";
	private IWXAPI wxApi;
	private Bundle bundle;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_wx_entry);
        Log.d(TAG, "in onCreate");
		bundle = getIntent().getExtras();
		// 注册微信
		wxApi = WXAPIFactory.createWXAPI(this, Constants.WEIXIN_APP_ID, true);
		wxApi.registerApp(Constants.WEIXIN_APP_ID);
        wxApi.handleIntent(getIntent(), this);
        finish();
        /*
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Create the text view
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);

        // Set the text view as the activity layout
        setContentView(textView);
		*/
    }
    
    /*
	@Override
	public void onNewIntent(Intent intent) {
        Log.d(TAG, "in onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		bundle = intent.getExtras();
        wxApi.handleIntent(getIntent(), this);
	}
	*/
    
//    public void onDestroy()
//    {
//    	Log.d(TAG, "in onDestroy");
//    	super.onDestroy();
//    }

//    public void sendMessage(View view) {
//        // 发送信息到微信
//        WXTextObject textObj = new WXTextObject();
//        textObj.text = Constants.SHARE_MESSAGE;
//        WXMediaMessage msg = new WXMediaMessage();
//        msg.mediaObject = textObj;
//        msg.description = Constants.SHARE_MESSAGE;
//        
//        SendMessageToWX.Req req = new SendMessageToWX.Req();
//        req.transaction = String.valueOf(System.currentTimeMillis());
//        req.message = msg;
//        if (wxApi.getWXAppSupportAPI() >= 0x21020001) {
//        	req.scene = SendMessageToWX.Req.WXSceneTimeline;
////        	req.scene = SendMessageToWX.Req.WXSceneSession;
//            Log.d(TAG, "support friend circle");
//        }
//        else {
//        	req.scene = SendMessageToWX.Req.WXSceneSession;
//        }
//        
//        wxApi.sendReq(req);
//		finish();
//    }
    
    
	@Override
	public void onReq(BaseReq req) {
		// TODO Auto-generated method stub
        Log.d(TAG, "onReq");
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
	        Log.d(TAG, "goToGetMsg");
			goToGetMsg();		
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			goToShowMsg((ShowMessageFromWX.Req) req);
			break;
		default:
			break;
		}
	}

	@Override
	public void onResp(BaseResp resp) {
		// TODO Auto-generated method stub
		int result = 0;
        Log.d(TAG, "in onResp");
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			result = R.string.errcode_success;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			result = R.string.errcode_cancel;
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
			result = R.string.errcode_deny;
			break;
		default:
			result = R.string.errcode_unknown;
			break;
		}
		
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}
	
	private void goToGetMsg() {
		final EditText editor = new EditText(this);
		editor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		editor.setText(Constants.SHARE_MESSAGE);
		
		// 初始化一个WXTextObject对象
		WXTextObject textObj = new WXTextObject();
		textObj.text = Constants.SHARE_MESSAGE;

		// 用WXTextObject对象初始化一个WXMediaMessage对象
		WXMediaMessage msg = new WXMediaMessage(textObj);
		msg.description = Constants.SHARE_MESSAGE;
		
		// 构造一个Resp
		GetMessageFromWX.Resp resp = new GetMessageFromWX.Resp();
		// 将req的transaction设置到resp对象中，其中bundle为微信传递过来的intent所带的内容，通过getExtras方法获取
		final GetMessageFromWX.Req req = new GetMessageFromWX.Req(bundle);
		resp.transaction = req.transaction;
		resp.message = msg;
		
		// 调用api接口响应数据到微信
		wxApi.sendResp(resp);
		finish();
	}
	
	private void goToShowMsg(ShowMessageFromWX.Req showReq) {
		//none
	}
	
	public static AlertDialog showAlert(final Context context, final String title, final View view, final String ok, final String cancel, final DialogInterface.OnClickListener lOk,
			final DialogInterface.OnClickListener lCancel) {
		if (context instanceof Activity && ((Activity) context).isFinishing()) {
			return null;
		}

		final Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setView(view);
		builder.setPositiveButton(ok, lOk);
		builder.setNegativeButton(cancel, lCancel);
		// builder.setCancelable(false);
		final AlertDialog alert = builder.create();
		alert.show();
		return alert;
	}
}
