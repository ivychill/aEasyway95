package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPoiInfo;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.PoiOverlay;
import com.luyun.easyway95.SettingActivity.AddrType;
import com.luyun.easyway95.SettingActivity.LongTap;
import com.luyun.easyway95.UserProfile.MKPoiInfoHelper;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class SearchActivity extends Activity {
	final static String TAG = "SearchActivity";
	private boolean isSearchrequested = true;
//    private TextView mTextView;
//    private ArrayList<MKPoiInfo> mListPoi;
//    private ImageButton mBtnSearch;
//    private EditText mEditText;
    private ListView mListView;
    private BMapManager mMapMan;
    
	MKSearch mSearch = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.search);
        mListView = (ListView) findViewById(R.id.listAddress);
//        mTextView = (TextView) findViewById(R.id.textField);
//        mBtnSearch = (ImageButton)findViewById(R.id.search);
//        mEditText = (EditText)findViewById(R.id.query);
        
        mMapMan = ((Easyway95App)getApplicationContext()).mBMapMan;
        mMapMan.start();
		mSearch = new MKSearch();
        mSearch.init(mMapMan, new MKSearchListener(){
    		public void onGetPoiResult(MKPoiResult res, int type, int error) {
    			Log.d (TAG, "enter onGetPoiResult");
    			// 错误号可参考MKEvent中的定义
    			if (error != 0 || res == null) {
    				Toast.makeText(SearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_LONG).show();
    				return;
    			}
    			int numPois = res.getNumPois();
    			Log.d (TAG, "numPois: " + numPois);
    		    if (numPois > 0) {
    			    PoiAdapter poiAdapter = new PoiAdapter(res.getAllPoi());
    			    mListView.setAdapter(poiAdapter);
    			    mListView.setOnItemClickListener(poiAdapter);
    		    } else if (res.getCityListNum() > 0) {
    		    	String strInfo = "在";
    		    	for (int i = 0; i < res.getCityListNum(); i++) {
    		    		strInfo += res.getCityListInfo(i).city;
    		    		strInfo += ",";
    		    	}
    		    	strInfo += "找到结果";
    				Toast.makeText(SearchActivity.this, strInfo, Toast.LENGTH_LONG).show();
    		    }
    		}
    		public void onGetDrivingRouteResult(MKDrivingRouteResult res,
    				int error) {
    		}
    		public void onGetTransitRouteResult(MKTransitRouteResult res,
    				int error) {
    		}
    		public void onGetWalkingRouteResult(MKWalkingRouteResult res,
    				int error) {
    		}
    		public void onGetAddrResult(MKAddrInfo res, int error) {
    			// TODO Auto-generated method stub
    			Log.d (TAG, "enter onGetAddrResult");
    		}
    		public void onGetBusDetailResult(MKBusLineResult result, int iError) {
    		}
    		@Override
    		public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
    			// TODO Auto-generated method stub
    		}
    		
        });
        
//        mBtnSearch.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		String query = mEditText.getText().toString();
//        		Log.d(TAG, "query: " + query);
//        		mSearch.poiSearchInCity("深圳", query);
//        	}
//        });
        
//	    if (isSearchrequested)
//	    {
//	        onSearchRequested();
//	    }
//	    else
//	    {
	        handleIntent(getIntent());
//	    }
	}
    
	@Override
	public void onNewIntent(Intent intent) {
		Log.d (TAG, "enter onNewIntent");
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
        Log.d(TAG, "intent: " + intent.toString());
        if (intent.getExtras() != null) {
            Log.d(TAG, intent.getExtras().keySet().toString());
        }
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
    		Log.d(TAG, "query: " + query);
    		mSearch.poiSearchInCity("深圳", query);
		}
    }
	
	void doSearch(String query)
	{
		Log.d(TAG, "query: " + query);
		mSearch.poiSearchInCity("深圳", query);
	}
	
    class PoiAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private final List<MKPoiInfo> mPoiInfos;
        private final LayoutInflater mInflater;

        public PoiAdapter(List<MKPoiInfo> poiInfos) {
        	mPoiInfos = poiInfos;
            mInflater = (LayoutInflater) SearchActivity.this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mPoiInfos.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TwoLineListItem view = (convertView != null) ? (TwoLineListItem) convertView :
                    createView(parent);
            bindView(view, mPoiInfos.get(position));
            return view;
        }

        private TwoLineListItem createView(ViewGroup parent) {
            TwoLineListItem item = (TwoLineListItem) mInflater.inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            item.getText2().setSingleLine();
            item.getText2().setEllipsize(TextUtils.TruncateAt.END);
            return item;
        }

        private void bindView(TwoLineListItem view, MKPoiInfo poiInfo) {
            view.getText1().setText(poiInfo.name);
            view.getText2().setText(poiInfo.address);
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //TODO: setMarker
        }
    }
}
