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
 * 条码扫描器
 */
public class BarcodeScanner{
	private boolean scanning;	//扫描中
	private boolean released;	//是否已经释放
	private boolean returnBitmap = true;	//当解码成功时是否返回位图
	private boolean debugMode;	//调试模式
	private boolean rotationBeforeDecode;	//在解码之前是否旋转90度
	private Rect scanAreaInPreviewRect;	//扫码区域位置
	private String logTag = BarcodeScanner.class.getSimpleName();	//日志标签
	private Camera camera;	//相机
	private Context context;	//上下文
	private Camera.Size cameraPreviewSize;	//相机预览尺寸
	private DecodeThread decodeThread;	//解码线程
	private MultiFormatReader multiFormatReader;	//解码器
	private BarcodeScanListener barcodeScanListener;	//解码监听器
	private DecodeResultHandler decodeResultHandler;	//解码结果处理器
	private DecodePreviewCallback decodePreviewCallback;	//解码预览回调
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param hints Zxing解码器所需的一些信息，你可以在此指定解码格式、编码方式等信息，如果此参数为null，那么将解码所有支持的格式、并且编码方式默认为UFT-8
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, Map<DecodeHintType, Object> hints, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(hints));
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param barcodeFormats 支持的格式
	 * @param charset 编码方式
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, BarcodeFormat[] barcodeFormats, String charset, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(barcodeFormats, charset));
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param barcodeFormats 支持的格式
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, BarcodeFormat[] barcodeFormats, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(barcodeFormats, null));
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param barcodeFormatGroups 支持的格式组
	 * @param charset 编码方式
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, BarcodeFormatGroup[] barcodeFormatGroups, String charset, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(barcodeFormatGroups, charset));
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 创建一个条码扫描器
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param barcodeFormatGroups 支持的格式组
	 * @param charset 编码方式
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, BarcodeFormatGroup[] barcodeFormatGroups, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints(barcodeFormatGroups, null));
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 创建一个条码扫描器，默认支持全部格式
	 * @param context 上下文
	 * @param cameraPreviewSize 相机预览尺寸
	 * @param scanAreaInPreviewRect 扫描区在预览图中的位置
	 * @param barcodeScanListener 解码监听器
	 */
	public BarcodeScanner(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, BarcodeScanListener barcodeScanListener){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(handleHints());
		init(context, cameraPreviewSize, scanAreaInPreviewRect, multiFormatReader, barcodeScanListener);
	}
	
	/**
	 * 初始化
	 * @param context
	 * @param cameraPreviewSize
	 * @param scanAreaInPreviewRect
	 * @param multiFormatReader
	 * @param barcodeScanListener
	 */
	private void init(Context context, Camera.Size cameraPreviewSize, Rect scanAreaInPreviewRect, MultiFormatReader multiFormatReader, BarcodeScanListener barcodeScanListener){
		this.context = context;
		this.cameraPreviewSize = cameraPreviewSize;
		this.scanAreaInPreviewRect = scanAreaInPreviewRect;
		this.multiFormatReader = multiFormatReader;
		this.barcodeScanListener = barcodeScanListener;
		decodeResultHandler = new DecodeResultHandler(this, barcodeScanListener);
		decodePreviewCallback = new DecodePreviewCallback();
		decodeThread = new DecodeThread(this );
		decodeThread.start();
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
	 */
	public void start(Camera camera){
		if(!released){
			if(!scanning){
				this.camera = camera;
				scanning = true;
				requestDecode();
				if(barcodeScanListener != null){
					barcodeScanListener.onStartScan();
				}
			}
		}else{
			throw new IllegalStateException("BarcodeScanner has been released.");
		}
	}
	
	/**
	 * 停止扫描
	 */
	public void stop(){
		if(!released){
			if(scanning){
				scanning = false;
				camera = null;
				if(barcodeScanListener != null){
					barcodeScanListener.onStopScan();
				}
			}
		}else{
			throw new IllegalStateException("BarcodeScanner has been released.");
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
			if(barcodeScanListener != null){
				barcodeScanListener.onRelease();
			}
		}
	}
	
	/**
	 * 是否已经释放
	 * @return
	 */
	public boolean isReleased() {
		return released;
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
	 * 是否正在扫描中
	 * @return
	 */
	public boolean isScanning() {
		return scanning;
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
	 * 是否是竖屏
	 * @return
	 */
	boolean isVertical(){
		return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}
	
	/**
	 * 在解码之前是否旋转90度
	 * @return
	 */ 
	boolean isRotationBeforeDecode() {
		return rotationBeforeDecode;
	}
	
	/**
	 * 设置在解码之前是否旋转90度
	 * @param rotationBeforeDecode
	 */
	public void setRotationBeforeDecode(boolean rotationBeforeDecode) {
		this.rotationBeforeDecode = rotationBeforeDecode;
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
	 * 获取解码线程
	 * @return
	 */
	DecodeThread getDecodeThread() {
		return decodeThread;
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
					if(barcodeScanListener != null){
						barcodeScanListener.onFoundPossibleResultPoint(resultPoint);
					}
				}
			});
		}
		
		return hints;
	}

	/**
	 * 处理解码配置
	 * @param hints
	 * @return
	 */
	private Map<DecodeHintType, Object> handleHints(BarcodeFormat[] barcodeFormats, String charset){
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
		return handleHints(hints);
	}

	/**
	 * 处理解码配置
	 * @param hints
	 * @return
	 */
	private Map<DecodeHintType, Object> handleHints(BarcodeFormatGroup[] barcodeFormatGroups, String charset){
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
		return handleHints(hints);
	}

	/**
	 * 处理解码格式
	 */
	private Map<DecodeHintType, Object> handleHints(){
		return handleHints(null);
	}
}
