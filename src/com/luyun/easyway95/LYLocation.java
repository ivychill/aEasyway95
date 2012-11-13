package com.luyun.easyway95;

import java.util.ArrayList;

import com.luyun.easyway95.shared.TSSProtos.LYCoordinate;
import com.luyun.easyway95.shared.TSSProtos.LYSamplePoint;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficReport;

import android.location.Location;
import android.util.Log;

/*
 * 本类处理位置上报
 */
public class LYLocation {
	private static String TAG = "LYLocation";
	private ArrayList<LYSamplePoint> mLocationOnTheWay;
	private LYLine mAnchorLine;
	private long lastCommit = 0;
	/*
	 * 处理速度、斜率的变化
	 * 如果速度与上次保持不变（误差在+-5%）、斜率保持一致，可直接替代上次点
	 * 否则增加一个新的点
	 * 如果网络是连通的，则每隔5分钟发一次位置信息
	 * 否则保存起来，直到下次网络连接成功，一次性发送积压的位置信息给服务器
	 */
	public void onLocationChanged(Location loc) {
		Log.d(TAG, "in LYLocation::onLocationChanged");
		long timeNow = System.currentTimeMillis()/1000;
		LYCoordinate coor = LYCoordinate.newBuilder()
				.setLat(loc.getLatitude())
				.setLng(loc.getLongitude())
				.build();
		LYSamplePoint sp = LYSamplePoint.newBuilder()
//				.setTimestamp(loc.getTime()) //Location.getTime返回的全都是0
				.setTimestamp(timeNow)
				.setCourse(loc.getBearing())
				.setSpCoordinate(coor)
				.setAltitude(loc.getAltitude())
				.build();
		if (mAnchorLine == null) {
			mAnchorLine = new LYLine(loc, timeNow, null, 0);
			mLocationOnTheWay = new ArrayList<LYSamplePoint>();
			mLocationOnTheWay.add(sp);
			mLocationOnTheWay.add(null);
			return;
		}
		LYLine tmpLine = new LYLine(mAnchorLine.end, mAnchorLine.endTime, loc, timeNow);
		boolean merged = mAnchorLine.mergeWithLine(tmpLine);
		if (merged) {
			int locSize = mLocationOnTheWay.size();
			mLocationOnTheWay.set(locSize-1, sp);
			Log.d(TAG, "merged");
			return;
		}
		mLocationOnTheWay.add(sp);
		mAnchorLine = tmpLine;
	}
	
	/*
	 * 产生路况上报
	 * 将AnchorLine之前的所有点都报上去
	 */
	public LYTrafficReport genTraffic() {
		Log.d(TAG, "in genTraffic");
		if (mLocationOnTheWay == null) return null;
		int locSize = mLocationOnTheWay.size();
		if (locSize < 3) return null;
		long timeNow = System.currentTimeMillis()/1000;
		if (timeNow-lastCommit<Constants.INTERVAL_OF_TRAFFIC_REPORT) return null;
		LYTrafficReport.Builder builder = LYTrafficReport.newBuilder();
		for (int i=0; i<locSize-2; i++) {
			builder.addPoints(mLocationOnTheWay.get(i));
		}
		LYSamplePoint sp1 = mLocationOnTheWay.get(locSize-2);
		LYSamplePoint sp2 = mLocationOnTheWay.get(locSize-1);
		mLocationOnTheWay = new ArrayList<LYSamplePoint>();
		mLocationOnTheWay.add(sp1);
		mLocationOnTheWay.add(sp2);
		lastCommit = timeNow;
		return builder.build();
	}
	
	public class LYLine {
		private Location start=null;
		private long startTime; //由于无法保证获得Location的timeStamp，改为获取系统的TimeStamp
		private Location end=null;
		private long endTime; //由于无法保证获得Location的timeStamp，改为获取系统的TimeStamp
		private double slope=0.0;
		private double avg_speed=0.0; 
		private double length=0.0;
		private double duration=0.0;
		
		LYLine(Location st, Location ed) {
			start = st;
			end = ed;
			updateLine();
		}
		
		LYLine(Location st, long st_ts, Location ed, long end_ts) {
			start = st;
			end = ed;
			startTime = st_ts;
			endTime = end_ts;
			updateLine();
		}
		
		public boolean isNextToMe(LYLine line) {
			if (line == null) return false; 
			if (line.start == null && this.end != null) return false;
			if (line.start != null && this.end == null) return false;
			if (line.start == null && this.end == null) return true;
			
			//if (this.end.getTime() != line.start.getTime()) return false;
			if (this.endTime != line.startTime) return false;
	        double distance = MapUtils.GetDistance(this.end.getLatitude(), this.end.getLongitude(), line.start.getLatitude(), line.start.getLongitude());
			if (Math.abs(distance) > 1.0) return false;
			return true;
		}
		
		/*
		 * 合并斜率相同、速度相同的两条线，以减少采集量
		 * 返回true，当需要合并时
		 * 返回false，当不合并时
		 */
		public boolean mergeWithLine(LYLine line) {
			if (line == null) return false;
			//判断是否首尾相连
			if (!isNextToMe(line)) return false;
			if (this.end != null) { 
				//防止被0除
				if (Math.abs(this.slope - line.slope)>(double)(this.slope)*0.05) return false;
				if (Math.abs(this.avg_speed - line.avg_speed)>(double)(this.avg_speed)*0.05) {
					return false;
				} else {
					//当绝对匀速（包括速度是0）的时候，要保证至少每隔5分钟采集一个点，因此超过5分钟，就不合并了。
					if (Math.abs(line.endTime-this.startTime)>300000) return false;
				}
			}
			end = line.end;
			updateLine();
			return true;
		}
		
		public void updateLine() {
			if (start == null || end == null) return;
	        slope =  ((double)(end.getLatitude() - start.getLatitude()))
	        		   /((double)(end.getLongitude() - start.getLongitude()));
	        length = MapUtils.GetDistance(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
	        duration = (double)((endTime - startTime)/1000);
	        avg_speed = length/duration;
		}
		
		public void setStart(Location loc) {
			start = loc;
		}
		
		public void setEnd(Location loc) {
			end = loc;
		}
		
		public Location getStart() {
			return start;
		}
		
		public Location getEnd() {
			return end;
		}
		
		public void setSpeed(double spd) {
			avg_speed = spd;
		}
		
		public void setSlope(double slp) {
			slope = slp;
		}
		
		public double getSlope() {
			return slope;
		}
		
		public double getSpeed() {
			return avg_speed;
		}
		
		public double getLength() {
			return length;
		}
	}

}
