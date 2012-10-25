package com.luyun.easyway95;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SearchableActivity extends Activity {
	final static String TAG = "SearchableActivity";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
//	    setContentView(R.layout.search);

	    // Get the intent, verify the action and get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
//	      doMySearch(query);
	    }
	}
	
    
	@Override
	public void onNewIntent(Intent intent) {
		Log.d (TAG, "enter onNewIntent");
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		Log.d (TAG, "enter handleIntent");
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			Log.d (TAG, "enter handleIntent, QUERY");
			String query = intent.getStringExtra(SearchManager.QUERY);
			//      doMySearch(query);
		}
    }
}
