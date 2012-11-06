package com.luyun.easyway95;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
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
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.PoiOverlay;
import com.baidu.mapapi.Projection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

//public class SearchActivity extends Activity implements OnScrollListener {
public class SearchActivity extends MapActivity implements OnGestureListener {
	final static String TAG = "SearchActivity";
	private GestureDetector mGestureDetector;
//	private boolean isSearchrequested = true;
    private View loadMoreView;    
    private Button loadMoreButton;  
//    private TextView mTextView;
    private ImageButton mBtnSearch;
    private EditText mSearchKey;
    private ImageButton mBtnBookmark;
    private ListView mListView;
    private BMapManager mMapMan;
    private MapView mMapView = null;
    PoiAdapter poiAdapter = new PoiAdapter(new ArrayList<MKPoiInfo>());
    int mNumPages = 0;
    int mPageIndex = 0;
	private SharedPreferences mSP;
	private UserProfile mUserProfile;
    private static String mStrSuggestions[] = {};   
    private static LinkedList<String> mRecentQuery;
    private MKSearch mSearch = new MKSearch();;
    private Handler handler = new Handler(); 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE); 
	    setContentView(R.layout.search);
	    mGestureDetector  = new GestureDetector(this,(android.view.GestureDetector.OnGestureListener) this);
        mMapMan = ((Easyway95App)getApplicationContext()).mBMapMan;
        mMapMan.start();
        super.initMapActivity(mMapMan);
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.setBuiltInZoomControls(true);  //�����������õ����ſؼ�
        MapController mMapController = mMapView.getController();  // �õ�mMapView�Ŀ���Ȩ,�����������ƺ�����ƽ�ƺ�����
        GeoPoint point = new GeoPoint((int) (22.526292 * 1E6),
                (int) (113.910416 * 1E6));  //�ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢�� (�� * 1E6)
        mMapController.setCenter(point);  //���õ�ͼ���ĵ�
        mMapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onTouch");
				return mGestureDetector.onTouchEvent(event);
			}
			
		});

		mSP = getSharedPreferences("com.luyun.easyway95", MODE_PRIVATE);
		mUserProfile = new UserProfile(mSP);
		mRecentQuery = mUserProfile.getRecentQuery();

        mBtnSearch = (ImageButton)findViewById(R.id.search);
        mSearchKey = (EditText)findViewById(R.id.searchkey);
        mListView = (ListView) findViewById(R.id.listAddress);
        loadMoreView = getLayoutInflater().inflate(R.layout.loadmore, null);
//        mListView.setOnScrollListener(this);
        loadMoreButton = (Button)loadMoreView.findViewById(R.id.loadMoreButton);  
        loadMoreButton.setOnClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                loadMoreButton.setText("���ڼ�����...");   //���ð�ť����  
                handler.postDelayed(new Runnable() {  
                    @Override
                    public void run() {  
                        loadMoreData();  
//                        poiAdapter.notifyDataSetChanged();
                        loadMoreButton.setText("�鿴����...");  //�ָ���ť����
                    }  
                },2000);  
            }  
        });  
        
        mBtnSearch.setOnClickListener(new View.OnClickListener() {
        	@Override  
            public void onClick(View v) {  
    			String query = mSearchKey.getText().toString();
        		Log.d(TAG, "query: " + query);
        		mRecentQuery.remove(query);
        		mRecentQuery.addFirst(query);
        		if (mRecentQuery.size() >= Constants.MAX_RECENT_QUERY) {
        			mRecentQuery.removeLast();
        		}
        		mUserProfile.setRecentQuery(mRecentQuery);
        		mUserProfile.commitPreferences(mSP);
        		mSearch.poiSearchInCity("����", query);
            }  
        });
        
        mSearchKey.addTextChangedListener( new TextWatcher() {
        	@Override  
            public void onTextChanged(CharSequence s, int start, int before, int count) {  
        		mSearch.suggestionSearch( 
        				mSearchKey.getText().toString());
            }

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}  
        });
        
//        mSearchKey.setOnTouchListener( new OnTouchListener() {
        mSearchKey.setOnFocusChangeListener ( new OnFocusChangeListener() {
			@Override
			public void onFocusChange (View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onTouch"); 
				ArrayAdapter<String> recentQueryAdapter = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.simple_list_item_1, mRecentQuery);
				mListView.setAdapter(recentQueryAdapter);
				mListView.setOnItemClickListener( new AdapterView.OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			        	String query = mRecentQuery.toArray(new String[0])[position];
			    		Log.d(TAG, "query: " + query);
			        	mSearchKey.setText(query);
		        		mRecentQuery.remove(query);
		        		mRecentQuery.addFirst(query);
		        		if (mRecentQuery.size() >= Constants.MAX_RECENT_QUERY) {
		        			mRecentQuery.removeLast();
		        		}
		        		mUserProfile.setRecentQuery(mRecentQuery);
		        		mUserProfile.commitPreferences(mSP);
			    		mSearch.poiSearchInCity("����", query);
					}
				}); 
			}
        });

        mSearch.init(mMapMan, new MKSearchListener(){
    		public void onGetPoiResult(MKPoiResult res, int type, int error) {
    			// ����ſɲο�MKEvent�еĶ���
    			if (error != 0 || res == null) {
    				Toast.makeText(SearchActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
    				return;
    			}
    			mNumPages = res.getNumPages();
    			Log.d (TAG, "mNumPages: " + mNumPages);
    			int numPois = res.getCurrentNumPois();
//    			Log.d (TAG, "numPois: " + numPois);
    		    if (numPois > 0) {
        	        mListView.addFooterView(loadMoreView);    //�����б�ײ���ͼ 
    			    poiAdapter.add(res.getAllPoi());
    			    poiAdapter.notifyDataSetChanged();
        		    mListView.setAdapter(poiAdapter);
        		    mListView.setOnItemClickListener(poiAdapter); 
//    			    Log.d (TAG, "all pois: " + res.getAllPoi());
    			    mPageIndex = res.getPageIndex();
    		    } else if (res.getCityListNum() > 0) {
    		    	String strInfo = "��";
    		    	for (int i = 0; i < res.getCityListNum(); i++) {
    		    		strInfo += res.getCityListInfo(i).city;
    		    		strInfo += ",";
    		    	}
    		    	strInfo += "�ҵ����";
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
				if (error != 0) {
					String str = String.format("����ţ�%d", error);
					Toast.makeText(SearchActivity.this, str, Toast.LENGTH_LONG).show();
					return;
				}

//				mMapView.getController().animateTo(res.geoPt);
//				String strInfo = String.format("%s γ�ȣ�%f ���ȣ�%f\r\n", res.strAddr, res.geoPt.getLatitudeE6()/1e6, 
//							res.geoPt.getLongitudeE6()/1e6);
//				Toast.makeText(SearchActivity.this, strInfo, Toast.LENGTH_LONG).show();
//				Log.d(TAG, strInfo);
		        final MKPoiInfoHelper mpi = new MKPoiInfoHelper(res);
//		        mSearchKey.setText(res.strAddr);
    			new AlertDialog.Builder(SearchActivity.this) 
 			       .setTitle("���õ�ַ")
			       .setMessage(String.format("����Ŀ���ַΪ%s��?", res.strAddr))
			       .setPositiveButton("ȷ��", 
			    	    new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int which) {
			                    Bundle bundle = new Bundle();
//			                    bundle.putString("key", "value");
			                    bundle.putSerializable(Constants.POI_RETURN_KEY, mpi);
			        			Log.d(TAG, "bundle: " + bundle);
			                    Intent intent = new Intent();  
//			                    intent.putExtra(Constants.POI_RETURN_KEY, poiInfoSerialable);
			                    intent.putExtras(bundle);
//			            		sendBroadcast(intent);
			                	setResult(RESULT_OK, intent);
			                	finish();
			                }
			            })
			        .setNegativeButton("ȡ��", null)
			        .show();    			
    		}
    		public void onGetBusDetailResult(MKBusLineResult result, int iError) {
    		}
    		@Override
    		public void onGetSuggestionResult(MKSuggestionResult res, int arg1) {
    			// TODO Auto-generated method stub
				if (arg1 != 0 || res == null) {
					Toast.makeText(SearchActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
					return;
				}
				int nSize = res.getSuggestionNum();
				mStrSuggestions = new String[nSize];

				for (int i = 0; i < nSize; i++) {
					mStrSuggestions[i] = res.getSuggestion(i).city + res.getSuggestion(i).key;
				}

				SuggestionAdapter suggestionAdapter = new SuggestionAdapter(SearchActivity.this, android.R.layout.simple_list_item_1,mStrSuggestions);
				mListView.setAdapter(suggestionAdapter);
				mListView.setOnItemClickListener(suggestionAdapter); 
//				ArrayAdapter<String> suggestionString
//					= new ArrayAdapter<String>(SearchActivity.this, android.R.layout.simple_list_item_1,mStrSuggestions);
//				mSuggestionList.setAdapter(suggestionString);
//				Toast.makeText(SearchActivity.this, "suggestion callback", Toast.LENGTH_LONG).show();
    		}    		

			@Override
			public void onGetRGCShareUrlResult(String arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
        });

//	    handleIntent(getIntent());
	}
    
	@Override
	public void onNewIntent(Intent intent) {
		Log.d (TAG, "enter onNewIntent");
		setIntent(intent);
//		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
//        Log.d(TAG, "intent: " + intent.toString());
        if (intent.getExtras() != null) {
			String query = intent.getStringExtra(SearchManager.QUERY);
//    		Log.d(TAG, "query: " + query);
    		mRecentQuery.remove(query);
    		mRecentQuery.addFirst(query);
    		if (mRecentQuery.size() >= Constants.MAX_RECENT_QUERY) {
    			mRecentQuery.removeLast();
    		}
    		mUserProfile.setRecentQuery(mRecentQuery);
    		mUserProfile.commitPreferences(mSP);
    		mSearch.poiSearchInCity("����", query);
        }
    }
	
    /**  
     * ���ظ�������  
     */  
    private void loadMoreData(){
    	Log.d(TAG, "mPageIndex: " + mPageIndex + " mNumPages: " + mNumPages);
    	if (mPageIndex < mNumPages - 1) {
    		mSearch.goToPoiPage(++mPageIndex);
    	}
    	else {
    		mListView.removeFooterView(loadMoreView);  
            Toast.makeText(this, "����ȫ��������!", Toast.LENGTH_LONG).show();  
//    		loadMoreButton.setText("�Ѵ����");
    	}
    }
    
//    @Override  
//    public void onScrollStateChanged(AbsListView view, int scrollState) {  
//        int itemsLastIndex = adapter.getCount()-1;  //���ݼ����һ�������    
//        int lastIndex = itemsLastIndex + 1;  
//        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE  
//                && visibleLastIndex == lastIndex) {  
//            // ������Զ�����,��������������첽�������ݵĴ���  
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
//        //������еļ�¼ѡ��������ݼ������������Ƴ��б�ײ���ͼ  
//        if(totalItemCount == datasize+1){  
//            mListView.removeFooterView(loadMoreView);  
//            Toast.makeText(this, "����ȫ��������!", Toast.LENGTH_LONG).show();  
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
        	MKPoiInfoHelper poiInfoSerialable = new MKPoiInfoHelper();
        	poiInfoSerialable.copyFrom(poiInfo);
        	
            Bundle bundle = new Bundle();
//            bundle.putString("key", "value");
            bundle.putSerializable(Constants.POI_RETURN_KEY, poiInfoSerialable);
			Log.d(TAG, "bundle: " + bundle);
            Intent intent = new Intent();  
//            intent.putExtra(Constants.POI_RETURN_KEY, poiInfoSerialable);
            intent.putExtras(bundle);
//    		sendBroadcast(intent);
        	setResult(RESULT_OK, intent);
        	finish();
        }
    }
    
    class SuggestionAdapter extends ArrayAdapter<String> implements AdapterView.OnItemClickListener {
        public SuggestionAdapter(Context context, int textViewResourceId,
				String[] objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}
        
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	String query = mStrSuggestions[position];
    		Log.d(TAG, "query: " + query);
        	mSearchKey.setText(query);
    		mRecentQuery.remove(query);
    		mRecentQuery.addFirst(query);
    		if (mRecentQuery.size() >= Constants.MAX_RECENT_QUERY) {
    			mRecentQuery.removeLast();
    		}
    		mUserProfile.setRecentQuery(mRecentQuery);
    		mUserProfile.commitPreferences(mSP);
    		mSearch.poiSearchInCity("����", query);
        }
    }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	//��������OnGestureListener����
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
//		if (isScale) {
//			Log.d(TAG, "scaling map, not long press");
//			return;
//		}
		//Log.d(TAG, "in onLongPress,"+e.toString());
        float x = e.getX();  
        float y = e.getY();  
        Projection pj = mMapView.getProjection();
        GeoPoint pointTapped = pj.fromPixels((int) x, (int) y);
    	
		Log.d(TAG, "in onLongPress");
    	//����һ�η��������룬��ȡλ��������Ϣ
    	mSearch.reverseGeocode(pointTapped);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
}
