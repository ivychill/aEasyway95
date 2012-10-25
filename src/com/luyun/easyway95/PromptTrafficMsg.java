package com.luyun.easyway95;

import java.util.ArrayList;

public class PromptTrafficMsg {
	private ArrayList<TrafficMsg> messages = null;
	
	/*
	 * 最多只保留三条
	 */
	void pushMsg(TrafficMsg msg) {
		if (messages == null) {
			messages = new ArrayList<TrafficMsg>();
		}
		messages.add(msg);
		while (messages.size()>3) {
			messages.remove(0);
		}
	}
	
	/*
	 * 将最后条信息Pop出来
	 */
	TrafficMsg popMsg() {
		if (messages == null || messages.size() == 0) {
			return null;
		}
		int size = messages.size();
		return messages.remove(size-1);
	}
	
	/*
	 * 这个类其实是TrafficPoint的一个字符串伴生类，开始是设计成一个类，后来发现不直观，改成两个类
	 * 蔡庆丰 20121025
	 */
	public class TrafficMsg {
		private String road;
		private String level; //"拥堵,严重拥堵,缓行"
		private String traffic; //
		
		TrafficMsg(String rd, String lvl, String trf) {
			road = rd;
			level = lvl;
			traffic = trf;
		}
		
		public void setRoad(String rd) {
			road = rd;
		}
		public void setLevel(String lvl) {
			level = lvl;
		}
		public void setTraffic(String trf) {
			traffic = trf;
		}
		
		String getTraffic() {
			return traffic;
		}
		
		public String getPaintedRoad() {
			String rd = "";
			String lvl = "";
			if (road != null) {
				rd = road;
			}
			if (level != null) {
				lvl = level;
			}
			return rd+lvl;
		}
		
		@Override
		public String toString() {
			String rd = "";
			String lvl = "";
			String trf = "";
			if (road != null) {
				rd = road;
			}
			if (level != null) {
				lvl = level;
			}
			if (traffic != null) {
				trf = traffic;
			}
			return rd+lvl+trf;
		}
	}

}
