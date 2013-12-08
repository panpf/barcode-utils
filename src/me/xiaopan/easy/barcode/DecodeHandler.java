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

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码处理器
 */
class DecodeHandler extends Handler {
	public static final int MESSAGE_WHAT_DECODE = 231242; 
	public static final int MESSAGE_WHAT_QUIT = 231243;
	private DecodeThread decodeThread;
	private SecondChronograph secondChronograph;

	public DecodeHandler(DecodeThread decodeThread) {
		this.decodeThread = decodeThread;
		secondChronograph = new SecondChronograph();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
			case MESSAGE_WHAT_DECODE:
				if (decodeThread.getBarcodeDecoder().isRunning() && message.obj != null) {
					decode((byte[]) message.obj);
				}
				break;
			case MESSAGE_WHAT_QUIT:
				decodeThread.getBarcodeDecoder().pause();
				Looper.myLooper().quit();
				break;
		}
	}

	/**
	 * 解码
	 * @param data
	 */
	private void decode(byte[] data) {
		secondChronograph.count();

		/* 初始化源数据，如果是竖屏的话就将源数据旋转90度 */
		int previewWidth = decodeThread.getBarcodeDecoder().getCameraPreviewSize().width;
		int previewHeight = decodeThread.getBarcodeDecoder().getCameraPreviewSize().height;
		long rotateMillis = -1;
		if (decodeThread.getBarcodeDecoder().isPortrait()) {
			data = RequiredUtils.yuvLandscapeToPortrait(data, previewWidth, previewHeight);
			previewWidth = previewWidth + previewHeight;
			previewHeight = previewWidth - previewHeight;
			previewWidth = previewWidth - previewHeight;
			rotateMillis = secondChronograph.count().getIntervalMillis();
		}
		
		/* 解码 */
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, previewWidth, previewHeight, decodeThread.getBarcodeDecoder().getScanningAreaRect().left, decodeThread.getBarcodeDecoder().getScanningAreaRect().top, decodeThread.getBarcodeDecoder().getScanningAreaRect().width(), decodeThread.getBarcodeDecoder().getScanningAreaRect().height(), false);
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
		Result result = null;
		try {
			result = decodeThread.getBarcodeDecoder().getMultiFormatReader().decodeWithState(binaryBitmap);
		} catch (ReaderException re) {
		} finally {
			decodeThread.getBarcodeDecoder().getMultiFormatReader().reset();
		}

		/* 结果处理 */
		long decodeMillis = secondChronograph.count().getIntervalMillis();
		if (result != null) {
			byte[] bitmapData = null;
			float scaleFactor = 0.0f;
			long bitmapMillis = -1;
			if(decodeThread.getBarcodeDecoder().isReturnBitmap()){
				int[] pixels = source.renderThumbnail();
				int width = source.getThumbnailWidth();
				int height = source.getThumbnailHeight();
				Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
				
				bitmapData = out.toByteArray();
				scaleFactor = (float) width / source.getWidth();
				bitmapMillis = secondChronograph.count().getIntervalMillis();
			}
			if(!decodeThread.getBarcodeDecoder().isContinuousScanMode()){
				decodeThread.getBarcodeDecoder().pause();
			}
			if(decodeThread.getBarcodeDecoder().isDebugMode()){
				Log.d(decodeThread.getBarcodeDecoder().getLogTag(), "解码成功，耗时："+(rotateMillis + decodeMillis + bitmapMillis)+"毫秒"+(rotateMillis > 0?"；旋转耗时："+rotateMillis+"毫秒":"")+"；解码耗时："+decodeMillis+"毫秒"+(bitmapMillis > 0?"；图片处理耗时："+bitmapMillis+"毫秒":"")+"；条码："+result.getText());
			}
			decodeThread.getBarcodeDecoder().getDecodeResultHandler().sendSuccessMessage(result, bitmapData, scaleFactor);
		} else {
			if(decodeThread.getBarcodeDecoder().isDebugMode()){
				Log.w(decodeThread.getBarcodeDecoder().getLogTag(), "解码失败，耗时："+(rotateMillis + decodeMillis)+"毫秒"+(rotateMillis > 0?"；旋转耗时："+rotateMillis+"毫秒":"")+"；解码耗时："+decodeMillis+"毫秒");
			}
			decodeThread.getBarcodeDecoder().getDecodeResultHandler().sendFailureMessage();
		}
	}
	
	/**
	 * 发送解码消息
	 * @param data
	 */
	void sendDecodeMessage(byte[] data){
		obtainMessage(MESSAGE_WHAT_DECODE, data).sendToTarget();
	}
	
	/**
	 * 发送退出消息
	 */
	void sendQuitMessage(){
		obtainMessage(MESSAGE_WHAT_QUIT).sendToTarget();
	}
}
