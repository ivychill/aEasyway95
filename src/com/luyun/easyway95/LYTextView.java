package com.luyun.easyway95;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

public class LYTextView extends TextView {
	
	public LYTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		}
	
	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
	    if(focused)
	        super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}

	@Override
	public void onWindowFocusChanged(boolean focused) {
	    if(focused)
	        super.onWindowFocusChanged(focused);
	}


	@Override
	public boolean isFocused() {
	    return true;
	}
}
