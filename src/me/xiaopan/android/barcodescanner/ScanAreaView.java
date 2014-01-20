/*
 * Copyright 2013 Peng fei Pan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.xiaopan.android.barcodescanner;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

/**
 * 扫描区域视图
 */
public class ScanAreaView extends View implements Runnable{
	private static final int MAX_RESULT_POINTS = 20;
	private static final int OPAQUE = 0xFF;	//不透明
	private int width;	//扫描框的宽
	private int height;	//扫描框的高
	private int laserLineColor = -65536;	//激光线的颜色，默认为红色
	private int laserLineSlidePace = 7;	//激光线滑动步伐
	private int laserLineHeight = 4;//激光线高度
	private int laserLineInSlideAvailableHeight;	//激光线在滑动的过程中扫描框可使用的高度
	private int laserLineLeft;	//激光线左边距
	private int laserLineTop;	//激光线顶边距（固定位置时使用）
	private int laserLineSlideTop;	//激光线在滑动过程中的顶边距（动态变化的）
	private int laserLineRight;	//激光线的右边距
	private int laserLineAlphaIndex;	//激光线透明度索引
	private int resultPointColor = -256;	//可疑点的颜色，默认为黄色
	private int refreshSpec = 30;	//两次刷新之间的间隔，单位毫秒
	private int[] laserLineAlphas = {40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 255, 240, 220, 200, 180, 160, 140, 120, 100, 80, 60};//激光线透明度变化表
	private float newResultPointRadius = 6.0f;	//新的可疑点半径
	private float oldResultPointRadius = 3.0f;	//旧的可疑点半径
	private boolean initialized;	//初始化位置信息
	private boolean closeSlideLaserLineMode;	//是否关闭滑动激光线模式
	private Rect resultBitmapDrawRect;	//结果图片绘制区域的位置
	private Paint paint;	//画笔
	private Bitmap resultBitmap;	//扫描结果图片
	private Handler handler;	//用来刷新的Handler
	private List<ResultPoint> possibleResultPoints;	//当前可疑点集合
	private List<ResultPoint> lastPossibleResultPoints;	//上次可疑点集合
	
	public ScanAreaView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public ScanAreaView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ScanAreaView(Context context) {
		super(context);
		init();
	}
	
	private void init(){
		paint = new Paint();
		handler = new Handler();
		possibleResultPoints = new ArrayList<ResultPoint>(5);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		/* 初始化 */
		if(!initialized){
			initialized = true;
			width = getWidth();
			height = getHeight();
			laserLineInSlideAvailableHeight = (height - laserLineHeight);
			laserLineLeft = getPaddingLeft();
			laserLineTop = (height/2) - (laserLineHeight / 2);
			laserLineRight = width - getPaddingRight();
			resultBitmapDrawRect = new Rect(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
		}
		
		if (resultBitmap != null) {
			/* 在扫描框中绘制结果图片 */
			paint.setAlpha(OPAQUE);
			canvas.drawBitmap(resultBitmap, null, resultBitmapDrawRect, paint);
		} else {
			/* 绘制激光线 */
			paint.setColor(getLaserLineColor());	//设置画笔颜色为红色
			paint.setAlpha(getLaserLineAlphas()[laserLineAlphaIndex]);	//设置透明度（透明度的值根据索引从透明度变换表中取）
			laserLineAlphaIndex = (laserLineAlphaIndex + 1) % getLaserLineAlphas().length;	//更新激光线透明度索引
			if(closeSlideLaserLineMode){
				canvas.drawRect(laserLineLeft, laserLineTop, laserLineRight, laserLineTop + laserLineHeight, paint);
			}else{
				canvas.drawRect(laserLineLeft, laserLineSlideTop, laserLineRight, laserLineSlideTop + laserLineHeight, paint);
				laserLineSlideTop = (laserLineSlideTop + laserLineSlidePace) % laserLineInSlideAvailableHeight;
			}
			
			/* 绘制可疑点 */
			List<ResultPoint> currentPossible = possibleResultPoints;
			List<ResultPoint> currentLast = lastPossibleResultPoints;
			paint.setColor(resultPointColor);
			paint.setShader(null);
			if (currentPossible.isEmpty()) {
				lastPossibleResultPoints = null;
			} else {
				possibleResultPoints = new ArrayList<ResultPoint>(5);
				lastPossibleResultPoints = currentPossible;
				paint.setAlpha(OPAQUE);
				synchronized (currentPossible) {
					for (ResultPoint point : currentPossible) {
						canvas.drawCircle(point.getX(), point.getY(), newResultPointRadius, paint);
					}
				}
			}
			if (currentLast != null) {
				paint.setAlpha(OPAQUE / 2);
				synchronized (currentLast) {
					for (ResultPoint point : currentLast) {
						canvas.drawCircle(point.getX(), point.getY(), oldResultPointRadius, paint);
					}
				}
			}
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(resultBitmap != null && !resultBitmap.isRecycled()){
			resultBitmap.recycle();
		}
	}

	/**
	 * 添加可疑点
	 * @param resultPoint
	 */
	public void addResultPoint(ResultPoint resultPoint) {
		List<ResultPoint> resultPoints = possibleResultPoints;
	    synchronized (resultPoints) {
	      resultPoints.add(resultPoint);
	      int size = resultPoints.size();
	      if (size > MAX_RESULT_POINTS) {
	        resultPoints.subList(0, size - MAX_RESULT_POINTS / 2).clear();
	      }
	    }
	}
	
	/**
	 * 绘制结果图片
	 */
	public void drawResultBitmap(Bitmap barcode) {
		if(resultBitmap != null && !resultBitmap.isRecycled()){
			resultBitmap.recycle();
		}
		resultBitmap = barcode;
		invalidate();
	}
	
	@Override
	public void run() {
		resultBitmap = null;
		invalidate();
		handler.postDelayed(this, refreshSpec);
	}
	
	/**
	 * 开始刷新（移动并闪烁激光线以及显示可疑点）
	 */
	public void startRefresh(){
		handler.removeCallbacks(this);
		handler.post(this);
	}
	
	/**
	 * 停止刷新
	 */
	public void stopRefresh(){
		handler.removeCallbacks(this);
	}

	/**
	 * 判断是否关闭滑动激光线模式
	 * @return 是否关闭滑动激光线模式
	 */
	public boolean isCloseSlideLaserLineMode() {
		return closeSlideLaserLineMode;
	}

	/**
	 * 设置是否关闭滑动激光线模式
	 * @param closeSlideLaserLineMode 是否关闭滑动激光线模式
	 */
	public void setCloseSlideLaserLineMode(boolean closeSlideLaserLineMode) {
		this.closeSlideLaserLineMode = closeSlideLaserLineMode;
	}

	/**
	 * 获取可疑点颜色
	 * @return 可疑点颜色
	 */
	public int getResultPointColor() {
		return resultPointColor;
	}

	/**
	 * 设置可疑点颜色
	 * @param resultPointColor 可疑点颜色
	 */
	public void setResultPointColor(int resultPointColor) {
		this.resultPointColor = resultPointColor;
	}

	/**
	 * 获取激光线颜色
	 * @return 激光线颜色
	 */
	public int getLaserLineColor() {
		return laserLineColor;
	}

	/**
	 * 设置激光线颜色
	 * @param resultPointColor 激光线颜色
	 */
	public void setLaserLineColor(int laserLineColor) {
		this.laserLineColor = laserLineColor;
	}

	/**
	 * 获取新的可疑点的半径
	 * @return 新的可疑点的半径
	 */
	public float getNewResultPointRadius() {
		return newResultPointRadius;
	}

	/**
	 * 设置新的可疑点的半径
	 * @param newResultPointRadius 新的可疑点的半径
	 */
	public void setNewResultPointRadius(float newResultPointRadius) {
		this.newResultPointRadius = newResultPointRadius;
	}

	/**
	 * 获取旧的可疑点的半径
	 * @return 旧的可疑点的半径
	 */
	public float getOldResultPointRadius() {
		return oldResultPointRadius;
	}

	/**
	 * 设置旧的可疑点的半径
	 * @param oldResultPointRadius 旧的可疑点的半径
	 */
	public void setOldResultPointRadius(float oldResultPointRadius) {
		this.oldResultPointRadius = oldResultPointRadius;
	}

	/**
	 * 获取激光线透明度变化表
	 * @return 激光线透明度变化表
	 */
	public int[] getLaserLineAlphas() {
		return laserLineAlphas;
	}

	/**
	 * 设置激光线透明度变化表
	 * @param laserLineAlphas 激光线透明度变化表
	 */
	public void setLaserLineAlphas(int[] laserLineAlphas) {
		this.laserLineAlphas = laserLineAlphas;
	}

	/**
	 * 获取激光线滑动步伐
	 * @return 激光线滑动步伐
	 */
	public int getLaserLineSlidePace() {
		return laserLineSlidePace;
	}

	/**
	 * 设置激光线滑动步伐
	 * @param laserLineSlidePace 激光线滑动步伐
	 */
	public void setLaserLineSlidePace(int laserLineSlidePace) {
		this.laserLineSlidePace = laserLineSlidePace;
	}

	/**
	 * 获取激光线高度
	 * @return 激光线高度
	 */
	public int getLaserLineHeight() {
		return laserLineHeight;
	}

	/**
	 * 设置激光线高度
	 * @param laserLineHeight 激光线高度
	 */
	public void setLaserLineHeight(int laserLineHeight) {
		this.laserLineHeight = laserLineHeight;
		initialized = false;
	}

	/**
	 * 获取刷新间隔时间
	 * @return 刷新间隔时间，单位毫秒
	 */
	public int getRefreshSpec() {
		return refreshSpec;
	}

	/**
	 * 设置刷新间隔时间
	 * @param refreshSpec 刷新间隔时间，单位毫秒
	 */
	public void setRefreshSpec(int refreshSpec) {
		this.refreshSpec = refreshSpec;
	}
}
