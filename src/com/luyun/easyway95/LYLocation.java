package com.luyun.easyway95;

import java.util.ArrayList;

import com.luyun.easyway95.shared.TSSProtos.LYCoordinate;
import com.luyun.easyway95.shared.TSSProtos.LYSamplePoint;
import com.luyun.easyway95.shared.TSSProtos.LYTrafficReport;

import android.location.Location;
import android.util.Log;

/*
 * ���ദ��λ���ϱ�
 */
public class LYLocation {
	private static String TAG = "LYLocation";
	private ArrayList<LYSamplePoint> mLocationOnTheWay;
	private LYLine mAnchorLine;
	private long lastCommit = 0;
	/*
	 * �����ٶȡ�б�ʵı仯
	 * ����ٶ����ϴα��ֲ��䣨�����+-5%����б�ʱ���һ�£���ֱ������ϴε�
	 * ��������һ���µĵ�
	 * �����������ͨ�ģ���ÿ��5���ӷ�һ��λ����Ϣ
	 * ���򱣴�������ֱ���´��������ӳɹ���һ���Է��ͻ�ѹ��λ����Ϣ��������
	 */
	public void onLocationChanged(Location loc) {
		Log.d(TAG, "in LYLocation::onLocationChanged");
		long timeNow = System.currentTimeMillis()/1000;
		LYCoordinate coor = LYCoordinate.newBuilder()
				.setLat(loc.getLatitude())
				.setLng(loc.getLongitude())
				.build();
		LYSamplePoint sp = LYSamplePoint.newBuilder()
//				.setTimestamp(loc.getTime()) //Location.getTime���ص�ȫ����0
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
	 * ����·���ϱ�
	 * ��AnchorLine֮ǰ�����е㶼����ȥ
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
		private long startTime; //�����޷���֤���Location��timeStamp����Ϊ��ȡϵͳ��TimeStamp
		private Location end=null;
		private long endTime; //�����޷���֤���Location��timeStamp����Ϊ��ȡϵͳ��TimeStamp
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
		 * �ϲ�б����ͬ���ٶ���ͬ�������ߣ��Լ��ٲɼ���
		 * ����true������Ҫ�ϲ�ʱ
		 * ����false�������ϲ�ʱ
		 */
		public boolean mergeWithLine(LYLine line) {
			if (line == null) return false;
			//�ж��Ƿ���β����
			if (!isNextToMe(line)) return false;
			if (this.end != null) { 
				//��ֹ��0��
				if (Math.abs(this.slope - line.slope)>(double)(this.slope)*0.05) return false;
				if (Math.abs(this.avg_speed - line.avg_speed)>(double)(this.avg_speed)*0.05) {
					return false;
				} else {
					//���������٣������ٶ���0����ʱ��Ҫ��֤����ÿ��5���Ӳɼ�һ���㣬��˳���5���ӣ��Ͳ��ϲ��ˡ�
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
