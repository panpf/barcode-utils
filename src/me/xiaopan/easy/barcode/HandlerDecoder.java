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
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * 解码器
 */
public class HandlerDecoder{
	private boolean running = true;	//运行中
	private boolean returnBitmap = true;
	private boolean debugMode;
	private String logTag = HandlerDecoder.class.getSimpleName();
	private HandlerDecodeThread decodeThread;	//解码线程
	private ResultPointCallback resultPointCallback;
	private DecodeResultHandler decodeResultHandler;
	
	public HandlerDecoder(Context context, Camera.Size cameraPreviewSize, Rect scanningAreaRect, Map<DecodeHintType, Object> hints, DecodeListener decodeListener){
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
		
		hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, new ResultPointCallback() {
			@Override
			public void foundPossibleResultPoint(ResultPoint arg0) {
				if(resultPointCallback != null){
					resultPointCallback.foundPossibleResultPoint(arg0);
				}
			}
		});
		
		decodeResultHandler = new DecodeResultHandler(decodeListener);
		decodeThread = new HandlerDecodeThread(this, hints, cameraPreviewSize, scanningAreaRect, context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		decodeThread.start();
	}
	
	/**
	 * 解码
	 * @param data 源数据
	 */
	public void decode(byte[] data) {
		if(running){
//			decodeThread.tryDecode(sourceData);
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
	}
	
	/**
	 * 释放，请务必在Activity的onDestory()中调用此方法来释放Decoder所拥用的线程
	 */
	public void release(){
		pause();
	}
	
	public boolean isReturnBitmap() {
		return returnBitmap;
	}

	public void setReturnBitmap(boolean returnBitmap) {
		this.returnBitmap = returnBitmap;
	}

	public boolean isRunning() {
		return running;
	}

	public ResultPointCallback getResultPointCallback() {
		return resultPointCallback;
	}

	public void setResultPointCallback(ResultPointCallback resultPointCallback) {
		this.resultPointCallback = resultPointCallback;
	}
	
	public DecodeResultHandler getDecodeResultHandler() {
		return decodeResultHandler;
	}

	public String getLogTag() {
		return logTag;
	}

	public void setLogTag(String logTag) {
		this.logTag = logTag;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
}
