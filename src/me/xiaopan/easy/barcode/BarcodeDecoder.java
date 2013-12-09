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
package me.xiaopan.easy.barcode;

import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * 条码解码器
 */
public class BarcodeDecoder{
	private boolean running = true;	//运行中
	private boolean isPortrait;	//是否是竖屏
	private boolean returnBitmap = true;	//当解码成功时是否返回位图
	private boolean debugMode;	//调试模式
	private Rect scanAreaInPreviewRect;	//扫码区域位置
	private String logTag = BarcodeDecoder.class.getSimpleName();	//日志标签
	private Camera camera;	//相机
	private Camera.Size cameraPreviewSize;	//相机预览尺寸
	private DecodeThread decodeThread;	//解码线程
	private DecodeListener decodeListener;	//解码监听器
	private MultiFormatReader multiFormatReader;	//解码器
	private DecodeResultHandler decodeResultHandler;	//解码结果处理器
	private DecodePreviewCallback decodePreviewCallback;	//解码预览回调
	
	/**
	 * 创建一个条码解码器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param hints Zxing解码器所需的一些信息，你可以在此指定解码格式、编码方式等信息，如果此参数为null，那么将解码所有支持的格式、并且编码方式默认为UFT-8
	 * @param decodeListener 解码监听器
	 */
	public BarcodeDecoder(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, Map<DecodeHintType, Object> hints, DecodeListener decodeListener){
		this.isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		this.cameraPreviewSize = cameraPreviewSize;
		this.scanAreaInPreviewRect = scanAreaInPreviewRect;
		this.decodeListener = decodeListener;
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(hints));
		decodeResultHandler = new DecodeResultHandler(this, decodeListener);
		decodePreviewCallback = new DecodePreviewCallback();
		decodeThread = new DecodeThread(this );
		decodeThread.start();
	}
	
	/**
	 * 设置Camera
	 * @param camera
	 */
	public void setCamera(Camera camera){
		if(this.camera != null){
			this.camera.release();
		}
		this.camera = camera;
		requestDecode();
	}
	
	/**
	 * 请求解码
	 */
	void requestDecode(){
		if(camera != null && running){
			decodePreviewCallback.setBarcodeDecoder(this);
			camera.setOneShotPreviewCallback(decodePreviewCallback);
		}
	}
	
	/**
	 * 解码
	 * @param data 源数据
	 */
	void decode(byte[] data) {
		if(running){
			decodeThread.getDecodeHandler().sendDecodeMessage(data);
		}
	}
	
	/**
	 * 暂停解码
	 */
	public void pause(){
		running = false;
	}
	
	/**
	 * 恢复解码
	 */
	public void resume(){
		running = true;
		requestDecode();
	}
	
	/**
	 * 释放，请务必在Activity的onDestory()中调用此方法来释放Decoder所拥用的线程
	 */
	public void release(){
		if(camera != null){
			camera = null;
		}
		pause();
		decodeThread.getDecodeHandler().sendQuitMessage();
	}
	
	/**
	 * 是否返回Bitmap，如果为false的话DecodeListener.onDecodeSuccess()中的bitmapByteArray参数将为null
	 * @return
	 */
	public boolean isReturnBitmap() {
		return returnBitmap;
	}

	/**
	 * 设置是否返回Bitmap，如果为false的话DecodeListener.onDecodeSuccess()中的bitmapByteArray参数将为null
	 * @param returnBitmap
	 */
	public void setReturnBitmap(boolean returnBitmap) {
		this.returnBitmap = returnBitmap;
	}
	
	/**
	 * 是否正在运行中
	 * @return
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * 获取解码结果处理器
	 * @return
	 */
	DecodeResultHandler getDecodeResultHandler() {
		return decodeResultHandler;
	}

	/**
	 * 获取日志标签
	 * @return
	 */
	public String getLogTag() {
		return logTag;
	}

	/**
	 * 设置日志标签
	 * @param logTag
	 */
	public void setLogTag(String logTag) {
		this.logTag = logTag;
	}

	/**
	 * 是否是调试模式
	 * @return
	 */
	public boolean isDebugMode() {
		return debugMode;
	}

	/**
	 * 设置是否是调试模式
	 * @param debugMode
	 */
	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
	
	/**
	 * 设置扫描区域在预览图中的位置
	 * @param scanAreaInPreviewRect
	 */
	public void setScanAreaInPreviewRect(Rect scanAreaInPreviewRect) {
		this.scanAreaInPreviewRect = scanAreaInPreviewRect;
	}

	/**
	 * 设置Camera预览尺寸
	 * @param cameraPreviewSize
	 */
	public void setCameraPreviewSize(Camera.Size cameraPreviewSize) {
		this.cameraPreviewSize = cameraPreviewSize;
	}
	
	/**
	 * 是否是横屏
	 * @return
	 */
	boolean isPortrait() {
		return isPortrait;
	}

	/**
	 * 获取扫码区域位置
	 * @return
	 */
	Rect getScanAreaInPreviewRect() {
		return scanAreaInPreviewRect;
	}

	/**
	 * 获取相机预览尺寸
	 * @return
	 */
	Camera.Size getCameraPreviewSize() {
		return cameraPreviewSize;
	}

	/**
	 * 获取解码器
	 * @return
	 */
	public MultiFormatReader getMultiFormatReader() {
		return multiFormatReader;
	}

	/**
	 * 设置解码器
	 * @param multiFormatReader
	 */
	public void setMultiFormatReader(MultiFormatReader multiFormatReader) {
		this.multiFormatReader = multiFormatReader;
	}

	/**
	 * 处理解码格式
	 * @param hints
	 * @return
	 */
	private Map<DecodeHintType, Object> handleHints(Map<DecodeHintType, Object> hints){
		if(hints == null){
			hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		}
		
		if(!hints.containsKey(DecodeHintType.POSSIBLE_FORMATS)){
			Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>(3);
			decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
			decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
			decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
			hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
		}
		
		if(!hints.containsKey(DecodeHintType.CHARACTER_SET)){
			hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
		}
		
		if(!hints.containsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK)){
			hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, new ResultPointCallback() {
				@Override
				public void foundPossibleResultPoint(ResultPoint resultPoint) {
					if(decodeListener != null){
						decodeListener.foundPossibleResultPoint(resultPoint);
					}
				}
			});
		}
		
		return hints;
	}
}
