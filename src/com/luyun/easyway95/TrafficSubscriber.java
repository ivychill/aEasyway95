package com.luyun.easyway95;

import com.baidu.mapapi.MKRoute;
import com.baidu.mapapi.MKStep;

public class TrafficSubscriber {
	void SubTraffic (MKRoute route) {
		int mNumStep = route.getNumSteps();
		for (int index = 0; index < mNumStep; index++){
			MKStep step = route.getStep(index);
		}
	}
}
