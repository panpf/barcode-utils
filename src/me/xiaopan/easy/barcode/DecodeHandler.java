/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.easy.barcode;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import me.xiaopan.easy.android.util.camera.CameraUtils;
import me.xiaopan.easy.java.util.SecondChronograph;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码处理器
 */
final class DecodeHandler extends Handler {
	public static final int MESSAGE_WHAT_DECODE = 231242; 
	public static final int MESSAGE_WHAT_QUIT = 231243; 
	private boolean running = true;
	private boolean isPortrait;	//是否是竖屏
	private Rect scanningAreaRect;	//扫描框相对于预览界面的矩形
	private Camera.Size cameraPreviewSize;	//相机预览尺寸
	private BarcodeDecoder barcodeDecoder;
	private MultiFormatReader multiFormatReader;
	private SecondChronograph secondChronograph;

	DecodeHandler(BarcodeDecoder barcodeDecoder, Map<DecodeHintType, Object> hints,  Camera.Size cameraPreviewSize, Rect scanningAreaRect, boolean isPortrait) {
		this.barcodeDecoder = barcodeDecoder;
		this.cameraPreviewSize = cameraPreviewSize;
		this.scanningAreaRect = scanningAreaRect;
		this.isPortrait = isPortrait;
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		secondChronograph = new SecondChronograph();
	}

	@Override
	public void handleMessage(Message message) {
		if (running) {
			switch (message.what) {
				case MESSAGE_WHAT_DECODE:
					decode((byte[]) message.obj);
					break;
				case MESSAGE_WHAT_QUIT:
					running = false;
					Looper.myLooper().quit();
					break;
			}
		}
	}

	/**
	 * 解码
	 * @param data
	 */
	private void decode(byte[] data) {
		secondChronograph.count();

		/* 初始化源数据，如果是竖屏的话就将源数据旋转90度 */
		int previewWidth = cameraPreviewSize.width;
		int previewHeight = cameraPreviewSize.height;
		if (isPortrait) {
			data = CameraUtils.yuvLandscapeToPortrait(data, previewWidth, previewHeight);
			previewWidth = previewWidth + previewHeight;
			previewHeight = previewWidth - previewHeight;
			previewWidth = previewWidth - previewHeight;
//			if(barcodeDecoder.isDebugMode()){
//				Log.d(barcodeDecoder.getLogTag(), "将源数据旋转90度耗时："+secondChronograph.count().getIntervalMillis()+"毫秒");
//			}
		}
		
		/* 解码 */
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, previewWidth, previewHeight, scanningAreaRect.left, scanningAreaRect.top, scanningAreaRect.width(), scanningAreaRect.height(), false);
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
		Result result = null;
		try {
			result = multiFormatReader.decodeWithState(binaryBitmap);
		} catch (ReaderException re) {
		} finally {
			multiFormatReader.reset();
		}

		/* 结果处理 */
		if (result != null) {
			byte[] bitmapData = null;
			float scaleFactor = 0.0f;
			if(barcodeDecoder.isReturnBitmap()){
				int[] pixels = source.renderThumbnail();
				int width = source.getThumbnailWidth();
				int height = source.getThumbnailHeight();
				Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
				
				bitmapData = out.toByteArray();
				scaleFactor = (float) width / source.getWidth();
			}
			if(!barcodeDecoder.isContinuousScanMode()){
				running = false;
			}
			if(barcodeDecoder.isDebugMode()){
				Log.d(barcodeDecoder.getLogTag(), "解码成功，耗时："+secondChronograph.count().getIntervalMillis()+"毫秒；条码："+result.getText());
			}
			barcodeDecoder.getDecodeResultHandler().sendSuccessMessage(result, bitmapData, scaleFactor);
		} else {
			if(barcodeDecoder.isDebugMode()){
				Log.w(barcodeDecoder.getLogTag(), "解码失败，耗时："+secondChronograph.count().getIntervalMillis()+"毫秒");
			}
			barcodeDecoder.getDecodeResultHandler().sendFailureMessage();
		}
	}
	
	/**
	 * 发送解码消息
	 * @param data
	 */
	public void sendDecodeMessage(byte[] data){
		obtainMessage(MESSAGE_WHAT_DECODE, data).sendToTarget();
	}
	
	/**
	 * 发送退出消息
	 */
	public void sendQuitMessage(){
		obtainMessage(MESSAGE_WHAT_QUIT).sendToTarget();
	}
}
