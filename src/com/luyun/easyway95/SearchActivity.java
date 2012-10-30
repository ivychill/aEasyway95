package com.luyun.easyway95;

import java.io.Serializable;
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
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

//public class SearchActivity extends Activity implements OnScrollListener {
public class SearchActivity extends Activity {
	final static String TAG = "SearchActivity";
//	private boolean isSearchrequested = true;
    private View loadMoreView;    
    private Button loadMoreButton;  
//    private TextView mTextView;
//    private ArrayList<MKPoiInfo> mListPoi;
//    private ImageButton mBtnSearch;
//    private EditText mEditText;
    private ListView mListView;
    private BMapManager mMapMan;
    PoiAdapter poiAdapter = new PoiAdapter(new ArrayList<MKPoiInfo>());
    int mNumPages = 0;
    int mPageIndex = 0;
    
    private MKSearch mSearch = new MKSearch();;
    private Handler handler = new Handler(); 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.search);
        mListView = (ListView) findViewById(R.id.listAddress);
//        mTextView = (TextView) findViewById(R.id.textField);
//        mBtnSearch = (ImageButton)findViewById(R.id.search);
//        mEditText = (EditText)findViewById(R.id.query);
       
        loadMoreView = getLayoutInflater().inflate(R.layout.loadmore, null);  
        mListView.addFooterView(loadMoreView);    //设置列表底部视图 
	    mListView.setAdapter(poiAdapter);
	    mListView.setOnItemClickListener(poiAdapter); 
//        mListView.setOnScrollListener(this);
        loadMoreButton = (Button)loadMoreView.findViewById(R.id.loadMoreButton);  
        loadMoreButton.setOnClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                loadMoreButton.setText("正在加载中...");   //设置按钮文字  
                handler.postDelayed(new Runnable() {  
                    @Override
                    public void run() {  
                        loadMoreData();  
//                        poiAdapter.notifyDataSetChanged();
                        loadMoreButton.setText("查看更多...");  //恢复按钮文字
                    }  
                },2000);  
            }  
        });  
        
        mMapMan = ((Easyway95App)getApplicationContext()).mBMapMan;
        mMapMan.start();

        mSearch.init(mMapMan, new MKSearchListener(){
    		public void onGetPoiResult(MKPoiResult res, int type, int error) {
    			mNumPages = res.getNumPages();
    			Log.d (TAG, "mNumPages: " + mNumPages);
    			// 错误号可参考MKEvent中的定义
    			if (error != 0 || res == null) {
    				Toast.makeText(SearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_LONG).show();
    				return;
    			}
    			int numPois = res.getCurrentNumPois();
    			Log.d (TAG, "numPois: " + numPois);
    		    if (numPois > 0) {
    			    poiAdapter.add(res.getAllPoi());
    			    poiAdapter.notifyDataSetChanged(); 
    			    Log.d (TAG, "all pois: " + res.getAllPoi());
    			    mPageIndex = res.getPageIndex();
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
			@Override
			public void onGetRGCShareUrlResult(String arg0, int arg1) {
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
//        Log.d(TAG, "intent: " + intent.toString());
        if (intent.getExtras() != null) {
			String query = intent.getStringExtra(SearchManager.QUERY);
//    		Log.d(TAG, "query: " + query);
    		mSearch.poiSearchInCity("深圳", query);
        }
		
//		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//		}
    }
	
    /**  
     * 加载更多数据  
     */  
    private void loadMoreData(){
    	Log.d(TAG, "mPageIndex: " + mPageIndex + " mNumPages: " + mNumPages);
    	if (mPageIndex < mNumPages - 1) {
    		
    		mSearch.goToPoiPage(++mPageIndex);
    	}
    	else {
    		
    		mListView.removeFooterView(loadMoreView);  
            Toast.makeText(this, "数据全部加载完!", Toast.LENGTH_LONG).show();  
//    		loadMoreButton.setText("已达最后");
    	}
    }
    
//    @Override  
//    public void onScrollStateChanged(AbsListView view, int scrollState) {  
//        int itemsLastIndex = adapter.getCount()-1;  //数据集最后一项的索引    
//        int lastIndex = itemsLastIndex + 1;  
//        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE  
//                && visibleLastIndex == lastIndex) {  
//            // 如果是自动加载,可以在这里放置异步加载数据的代码  
//        }  
//    }  
//
//    @Override  
//    public void onScroll(AbsListView view, int firstVisibleItem,  
//            int visibleItemCount, int totalItemCount) {  
//        this.visibleItemCount = visibleItemCount;  
//        visibleLastIndex = firstVisibleItem + visibleItemCount - 1;  
//          
//        Log.e("========================= ","========================");  
//        Log.e("firstVisibleItem = ",firstVisibleItem+"");  
//        Log.e("visibleItemCount = ",visibleItemCount+"");  
//        Log.e("totalItemCount = ",totalItemCount+"");  
//        Log.e("========================= ","========================");  
//          
//        //如果所有的记录选项等于数据集的条数，则移除列表底部视图  
//        if(totalItemCount == datasize+1){  
//            mListView.removeFooterView(loadMoreView);  
//            Toast.makeText(this, "数据全部加载完!", Toast.LENGTH_LONG).show();  
//        }  
//    }
	
    class PoiAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private final List<MKPoiInfo> mPoiInfos;
        
        public PoiAdapter(List<MKPoiInfo> poiInfos) {
        	mPoiInfos = poiInfos;
        }
        
        public void add (List<MKPoiInfo> poiInfos) {
        	mPoiInfos.addAll(poiInfos);
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
//            LayoutInflater inflater = (LayoutInflater) SearchActivity.this.getSystemService(
//                    Context.LAYOUT_INFLATER_SERVICE);
//            TwoLineListItem item = (TwoLineListItem) inflater.inflate(
//                    android.R.layout.simple_list_item_2, parent, false);
            TwoLineListItem item = (TwoLineListItem) getLayoutInflater().inflate(
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
        	MKPoiInfo poiInfo = mPoiInfos.get(position);
        	MKPoiInfoSerialable poiInfoSerialable = new MKPoiInfoSerialable();
        	poiInfoSerialable.copyFrom(poiInfo);
        	
            Bundle bundle = new Bundle();
//            bundle.putString("key", "value");
            bundle.putSerializable(Constants.POI_RETURN_KEY, poiInfoSerialable);
			Log.d(TAG, "bundle: " + bundle);
            Intent intent = new Intent(SearchActivity.this, LYNavigator.class);  
//            intent.putExtra(Constants.POI_RETURN_KEY, poiInfoSerialable);
            intent.putExtras(bundle);
//    		sendBroadcast(intent);
            Log.d(TAG, "before setResult");
        	setResult(RESULT_OK, intent);
        	Log.d(TAG, "after setResult");
//            if (getParent() == null) {
//            	Log.d(TAG, "no parent");
//                setResult(Activity.RESULT_OK, intent);
//            } else {
//            	Log.d(TAG, "parent: " + getParent());
//                getParent().setResult(Activity.RESULT_OK, intent);
//            }
        	finish();
        }
    }
}
