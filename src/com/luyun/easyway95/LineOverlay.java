package com.luyun.easyway95;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.baidu.mapapi.*;


public class LineOverlay extends ItemizedOverlay<OverlayItem> {

	public List<OverlayItem> mGeoList = new ArrayList<OverlayItem>();
	private Drawable marker;
	private Context mContext;

	public LineOverlay(Drawable marker, Context context, ArrayList<GeoPoint> GeoPoints) {
		super(boundCenterBottom(marker));

		this.marker = marker;
		this.mContext = context;

		// 用给定的经纬度构造GeoPoint，单位是微度 (度 * 1E6)
		for (int i=0; i<GeoPoints.size(); i++) {
			GeoPoint gp = GeoPoints.get(i);
			mGeoList.add(new OverlayItem(gp, "matchedPoint", "matchedPoint"));
		}
		populate();  //createItem(int)方法构造item。一旦有了数据，在调用其它方法前，首先调用这个方法
	}

	public void updateOverlay()
	{
		populate();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		// Projection接口用于屏幕像素坐标和经纬度坐标之间的变换
		Projection projection = mapView.getProjection(); 
		Point startPoint = null;
		for (int index = size() - 1; index >= 0; index--) { // 遍历mGeoList
			OverlayItem overLayItem = getItem(index); // 得到给定索引的item

			String title = overLayItem.getTitle();
			// 把经纬度变换到相对于MapView左上角的屏幕像素坐标
			Point point = projection.toPixels(overLayItem.getPoint(), null); 

			// 可在此处添加您的绘制代码
			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setStrokeWidth(5);
			//canvas.drawText(title, point.x-30, point.y, paintText); // 绘制文本
			if (startPoint != null) {
				canvas.drawLine(startPoint.x, startPoint.y, point.x, point.y, paint);
			}
			startPoint = point;
		}

		super.draw(canvas, mapView, shadow);
		//调整一个drawable边界，使得（0，0）是这个drawable底部最后一行中心的一个像素
		boundCenterBottom(marker);
	}

	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return mGeoList.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return mGeoList.size();
	}
	@Override
	// 处理当点击事件
	protected boolean onTap(int i) {
		setFocus(mGeoList.get(i));
		// 更新气泡位置,并使之显示
//		GeoPoint pt = mGeoList.get(i).getPoint();
//		LineOverlay.mMapView.updateViewLayout( LineOverlay.mPopView,
//                new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
//                		pt, MapView.LayoutParams.BOTTOM_CENTER));
//		LineOverlay.mPopView.setVisibility(View.VISIBLE);
//		Toast.makeText(this.mContext, mGeoList.get(i).getSnippet(),
//				Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public boolean onTap(GeoPoint arg0, MapView arg1) {
		// TODO Auto-generated method stub
		// 消去弹出的气泡
		//LineOverlay.mPopView.setVisibility(View.GONE);
		return super.onTap(arg0, arg1);
	}
}

