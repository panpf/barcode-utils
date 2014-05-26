/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
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

package me.xiaopan.android.barcode;

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
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * 条码扫描器，默认的扫描区域为全屏如果你想指定扫描区域的话，就使用setScanAreaRectInPreview(Rect)方法设置
 */
public class BarcodeScanner{
	private boolean scanning;	//扫描中
	private boolean released;	//是否已经释放
	private boolean returnBitmap = true;	//当解码成功时是否返回位图
	private boolean debugMode;	//调试模式
	private boolean rotationBeforeDecodeOfLandscape;	//当横屏时在解码之前是否旋转90度
	private Rect scanAreaRectInPreview;	//扫码区域位置
	private String logTag = BarcodeScanner.class.getSimpleName();	//日志标签
	private Camera camera;	//相机
	private Context context;	//上下文
	private Camera.Size cameraPreviewSize;	//相机预览尺寸
	private DecodeThread decodeThread;	//解码线程
	private MultiFormatReader multiFormatReader;	//解码器
	private BarcodeScanCallback barcodeScanCallback;	//解码监听器
	private DecodeResultHandler decodeResultHandler;	//解码结果处理器
	private DecodePreviewCallback decodePreviewCallback;	//解码预览回调
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param hints Zxing解码器所需的一些信息，你可以在此指定解码格式、编码方式等信息，如果此参数为null，那么将解码所有支持的格式、并且编码方式默认为UFT-8
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, Map<DecodeHintType, Object> hints, BarcodeScanCallback barcodeScanCallback){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(createHints(hints));
		init(context, multiFormatReader, barcodeScanCallback);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param barcodeFormats 支持的格式
	 * @param charset 编码方式
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, BarcodeFormat[] barcodeFormats, String charset, BarcodeScanCallback barcodeScanCallback){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(createHints(barcodeFormats, charset));
		init(context, multiFormatReader, barcodeScanCallback);
	}
	
	/**
	 * 创建一个条码扫描器，默认编码方式为UTF-8
	 * @param context 上下文
	 * @param barcodeFormats 支持的格式
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, BarcodeFormat[] barcodeFormats, BarcodeScanCallback barcodeScanCallback){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(createHints(barcodeFormats, null));
		init(context, multiFormatReader, barcodeScanCallback);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param barcodeFormatGroups 支持的格式组
	 * @param charset 编码方式
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, BarcodeFormatGroup[] barcodeFormatGroups, String charset, BarcodeScanCallback barcodeScanCallback){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(createHints(barcodeFormatGroups, charset));
		init(context, multiFormatReader, barcodeScanCallback);
	}
	
	/**
	 * 创建一个条码扫描器，默认编码方式为UTF-8
	 * @param context 上下文
	 * @param barcodeFormatGroups 支持的格式组
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, BarcodeFormatGroup[] barcodeFormatGroups, BarcodeScanCallback barcodeScanCallback){
        this(context, barcodeFormatGroups, null, barcodeScanCallback);
	}
	
	/**
	 * 创建一个条码扫描器，默认支持全部格式，编码方式为UTF-8
	 * @param context 上下文
	 * @param barcodeScanCallback 解码监听器
	 */
	public BarcodeScanner(Context context, BarcodeScanCallback barcodeScanCallback){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(createHints(null));
		init(context, multiFormatReader, barcodeScanCallback);
	}
	
	/**
	 * 初始化
	 * @param context 上下文
	 * @param multiFormatReader 解码器
	 * @param barcodeScanCallback 扫描监听器
	 */
	private void init(Context context, MultiFormatReader multiFormatReader, BarcodeScanCallback barcodeScanCallback){
		this.context = context;
		this.multiFormatReader = multiFormatReader;
		this.barcodeScanCallback = barcodeScanCallback;
        this.decodeResultHandler = new DecodeResultHandler(this, barcodeScanCallback);
        this.decodePreviewCallback = new DecodePreviewCallback();
        this.decodeThread = new DecodeThread(this );
        this.decodeThread.start();
	}
	
	/**
	 * 设置Camera
	 * @param camera
	 */
	public void setCamera(Camera camera) {
		this.camera = camera;
		this.cameraPreviewSize = camera.getParameters().getPreviewSize();
	}

	/**
	 * 请求解码
	 */
	void requestDecode(){
		if(!released && scanning && camera != null){
			decodePreviewCallback.setBarcodeScanner(this);
			camera.setOneShotPreviewCallback(decodePreviewCallback);
		}
	}
	
	/**
	 * 启动扫描
     * @exception java.lang.IllegalStateException 已经释放了
	 */
	public void start(){
		if(!released){
			if(!scanning){
				scanning = true;
				requestDecode();
			}
		}else{
			throw new IllegalStateException("BarcodeScanner has been released.");
		}
	}
	
	/**
	 * 停止扫描
	 */
	public void stop(){
		if(!released && scanning){
            scanning = false;
		}
	}
	
	/**
	 * 释放，请务必在Activity的onDestory()中调用此方法来释放Decoder所拥用的线程
	 */
	public void release(){
		if(!released){
			released = true;
			if(scanning){
				scanning = false;
				camera = null;
				decodeThread.getDecodeHandler().sendQuitMessage();
			}
		}
	}
	
	/**
	 * 是否已经释放
	 * @return 是否已经释放
	 */
	public boolean isReleased() {
		return released;
	}

	/**
	 * 是否返回Bitmap，如果为false的话DecodeListener.onDecodeSuccess()中的bitmapByteArray参数将为null
	 * @return 是否返回Bitmap
	 */
	public boolean isReturnBitmap() {
		return returnBitmap;
	}
	
	/**
	 * 设置是否返回Bitmap，如果为false的话DecodeListener.onDecodeSuccess()中的bitmapByteArray参数将为null
	 * @param returnBitmap 是否返回Bitmap
	 */
	public void setReturnBitmap(boolean returnBitmap) {
		this.returnBitmap = returnBitmap;
	}
	
	/**
	 * 是否正在扫描中
	 * @return 是否正在扫描中
	 */
	public boolean isScanning() {
		return scanning;
	}

	/**
	 * 获取解码结果处理器
	 * @return 解码结果处理器
	 */
	DecodeResultHandler getDecodeResultHandler() {
		return decodeResultHandler;
	}

	/**
	 * 获取日志标签
	 * @return 日志标签
	 */
	public String getLogTag() {
		return logTag;
	}

	/**
	 * 设置日志标签
	 * @param logTag 日志标签
	 */
	public void setLogTag(String logTag) {
		this.logTag = logTag;
	}

	/**
	 * 是否是调试模式
	 * @return 是否是调试模式
	 */
	public boolean isDebugMode() {
		return debugMode;
	}

	/**
	 * 设置是否是调试模式
	 * @param debugMode 是否是调试模式
	 */
	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
	 * 设置扫描区域在预览图中的位置，默认是全屏
	 * @param scanAreaRectInPreview 扫描区域在预览图中的位置
	 */
	public void setScanAreaRectInPreview(Rect scanAreaRectInPreview) {
		this.scanAreaRectInPreview = scanAreaRectInPreview;
	}

	/**
	 * 是否是竖屏
	 * @return 是否是竖屏
	 */
	boolean isVertical(){
		return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}
	
	/**
	 * 当横屏时在解码之前是否旋转90度
	 * @return 当横屏时在解码之前是否旋转90度
	 */ 
	boolean isRotationBeforeDecodeOfLandscape() {
		return rotationBeforeDecodeOfLandscape;
	}
	
	/**
	 * 设置当横屏时在解码之前是否旋转90度
	 * @param rotationBeforeDecodeOfLandscape 当横屏时在解码之前是否旋转90度
	 */
	public void setRotationBeforeDecodeOfLandscape(boolean rotationBeforeDecodeOfLandscape) {
		this.rotationBeforeDecodeOfLandscape = rotationBeforeDecodeOfLandscape;
	}

	/**
	 * 获取扫码区域位置，默认是全屏
	 * @return 扫码区域位置
	 */
	Rect getScanAreaRectInPreview() {
		return scanAreaRectInPreview;
	}

	/**
	 * 获取相机预览尺寸
	 * @return 相机预览尺寸
	 */
	Camera.Size getCameraPreviewSize() {
		return cameraPreviewSize;
	}

	/**
	 * 获取解码器
	 * @return 解码器
	 */
	public MultiFormatReader getMultiFormatReader() {
		return multiFormatReader;
	}

	/**
	 * 设置解码器
	 * @param multiFormatReader 解码器
	 */
	public void setMultiFormatReader(MultiFormatReader multiFormatReader) {
		this.multiFormatReader = multiFormatReader;
	}
	
	/**
	 * 获取解码线程
	 * @return 解码线程
	 */
	DecodeThread getDecodeThread() {
		return decodeThread;
	}

	/**
	 * 生成解码配置集合
	 * @param hints 解码配置集合
	 * @return 一个新的解码配置集合
	 */
	private Map<DecodeHintType, Object> createHints(Map<DecodeHintType, Object> hints){
		if(hints == null){
			hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		}
		
		if(!hints.containsKey(DecodeHintType.POSSIBLE_FORMATS)){
			Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>(3);
			decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
			decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
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
					if(barcodeScanCallback != null){
						barcodeScanCallback.onFoundPossibleResultPoint(resultPoint);
					}
				}
			});
		}
		
		return hints;
	}

	/**
	 * 生成解码配置集合
     * @param barcodeFormats 支持的解码格式
     * @param charset 编码方式
	 * @return 解码配置集合
	 */
	private Map<DecodeHintType, Object> createHints(BarcodeFormat[] barcodeFormats, String charset){
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		
		if(barcodeFormats != null && barcodeFormats.length > 0){
			Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>(3);
			for(BarcodeFormat barcodeFormat : barcodeFormats){
				if(barcodeFormat != null){
					decodeFormats.add(barcodeFormat);
				}
			}
			if(!decodeFormats.isEmpty()){
				hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
			}
		}
		
		if(charset != null && !"".equals(charset)){
			hints.put(DecodeHintType.CHARACTER_SET, charset);
		}
		return createHints(hints);
	}

	/**
	 * 生成解码配置集合
     * @param barcodeFormatGroups 解码格式组数组
     * @param charset 编码方式
	 * @return 解码配置集合
	 */
	private Map<DecodeHintType, Object> createHints(BarcodeFormatGroup[] barcodeFormatGroups, String charset){
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		
		if(barcodeFormatGroups != null && barcodeFormatGroups.length > 0){
			Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>(3);
			for(BarcodeFormatGroup barcodeFormatGroup : barcodeFormatGroups){
				if(barcodeFormatGroup == BarcodeFormatGroup.PRODUCT_FORMATS){
					decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
				}else if(barcodeFormatGroup == BarcodeFormatGroup.ONE_D_FORMATS){
					decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
				}else if(barcodeFormatGroup == BarcodeFormatGroup.QR_CODE_FORMATS){
					decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
				}else if(barcodeFormatGroup == BarcodeFormatGroup.DATA_MATRIX_FORMATS){
					decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
				}
			}
			if(!decodeFormats.isEmpty()){
				hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
			}
		}
		
		if(charset != null && !"".equals(charset)){
			hints.put(DecodeHintType.CHARACTER_SET, charset);
		}
		return createHints(hints);
	}
	
	/**
	 * 条码扫描监听器
	 */
	public interface BarcodeScanCallback {
		/**
		 * 当找到可能的结果点
		 * @param resultPoint
		 */
		public void onFoundPossibleResultPoint(ResultPoint resultPoint);
		
		/**
		 * 当找到条码
		 * @param result
		 * @param bitmapByteArray
		 * @param scaleFactor
		 * @return 是否继续扫描
		 */
		public boolean onDecodeCallback(Result result, byte[] bitmapByteArray, float scaleFactor);
	}
}
