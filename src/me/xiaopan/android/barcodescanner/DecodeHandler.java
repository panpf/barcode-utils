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

package me.xiaopan.android.barcodescanner;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码处理器
 */
class DecodeHandler extends Handler {
	public static final int MESSAGE_WHAT_DECODE = 231242; 
	public static final int MESSAGE_WHAT_QUIT = 231243;
	private BarcodeScanner barcodeScanner;
	private SecondChronograph secondChronograph;
	private Rect scanAreaInPreviewRect;

	public DecodeHandler(BarcodeScanner barcodeScanner) {
		this.barcodeScanner = barcodeScanner;
		secondChronograph = new SecondChronograph();
		scanAreaInPreviewRect = new Rect();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
			case MESSAGE_WHAT_DECODE:
				if (barcodeScanner.isScanning() && message.obj != null) {
					decode((byte[]) message.obj);
				}
				break;
			case MESSAGE_WHAT_QUIT:
				barcodeScanner.stop();
				Looper.myLooper().quit();
				break;
		}
	}

	/**
	 * 解码
	 * @param data 数据
	 */
	private void decode(byte[] data) {
		secondChronograph.count();

		/* 初始化源数据，如果是竖屏的话就将源数据旋转90度 */
		int previewWidth = barcodeScanner.getCameraPreviewSize().width;
		int previewHeight = barcodeScanner.getCameraPreviewSize().height;

        //如果当前是横屏
		long rotateMillis = -1;
        if(!barcodeScanner.isVertical()){
            if(barcodeScanner.getScanAreaRectInPreview() != null){
            	scanAreaInPreviewRect.set(barcodeScanner.getScanAreaRectInPreview());
            }else{
            	scanAreaInPreviewRect.set(0, 0, previewWidth, previewHeight);
            }
            if(barcodeScanner.isRotationBeforeDecodeOfLandscape()){    //如果需要强制旋转90度的话
                data = RequiredUtils.yuvLandscapeToPortrait(data, previewWidth, previewHeight); //旋转源数据

                /* 旋转坐标区 */
                int left = scanAreaInPreviewRect.left;
                int top = scanAreaInPreviewRect.top;
                int right = scanAreaInPreviewRect.right;
                int bottom = scanAreaInPreviewRect.bottom;
                scanAreaInPreviewRect.left = previewHeight - bottom;
                scanAreaInPreviewRect.top = left;
                scanAreaInPreviewRect.right = previewHeight - top;
                scanAreaInPreviewRect.bottom = right;

                /* 旋转宽高 */
                previewWidth = previewWidth + previewHeight;
                previewHeight = previewWidth - previewHeight;
                previewWidth = previewWidth - previewHeight;

                rotateMillis = secondChronograph.count().getIntervalMillis();
            }
        }else{
            data = RequiredUtils.yuvLandscapeToPortrait(data, previewWidth, previewHeight); //旋转源数据

            /* 旋转宽高 */
            previewWidth = previewWidth + previewHeight;
            previewHeight = previewWidth - previewHeight;
            previewWidth = previewWidth - previewHeight;

            if(barcodeScanner.getScanAreaRectInPreview() != null){
            	scanAreaInPreviewRect.set(barcodeScanner.getScanAreaRectInPreview());
            }else{
            	scanAreaInPreviewRect.set(0, 0, previewWidth, previewHeight);
            }
            rotateMillis = secondChronograph.count().getIntervalMillis();
        }

		/* 解码 */
        Result result = null;
        PlanarYUVLuminanceSource source = null;;
    	MultiFormatReader multiFormatReader = barcodeScanner.getMultiFormatReader();
    	try {
    		source = new PlanarYUVLuminanceSource(data, previewWidth, previewHeight, scanAreaInPreviewRect.left, scanAreaInPreviewRect.top, scanAreaInPreviewRect.width(), scanAreaInPreviewRect.height(), false);
    		result = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
    	} catch (Throwable throwable) {
    		throwable.printStackTrace();
    	}
    	multiFormatReader.reset();

		/* 结果处理 */
		long decodeMillis = secondChronograph.count().getIntervalMillis();
		if (result != null) {
			byte[] bitmapData = null;
			float scaleFactor = 0.0f;
			long bitmapMillis = -1;
			if(barcodeScanner.isReturnBitmap()){
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
			if(barcodeScanner.isDebugMode()){
				Log.d(barcodeScanner.getLogTag(), "解码成功，耗时："+(rotateMillis + decodeMillis + bitmapMillis)+"毫秒"+(rotateMillis > 0?"；旋转耗时："+rotateMillis+"毫秒":"")+"；解码耗时："+decodeMillis+"毫秒"+(bitmapMillis > 0?"；图片处理耗时："+bitmapMillis+"毫秒":"")+"；条码："+result.getText());
			}
			barcodeScanner.getDecodeResultHandler().sendSuccessMessage(result, bitmapData, scaleFactor);
		} else {
			if(barcodeScanner.isDebugMode()){
				Log.w(barcodeScanner.getLogTag(), "解码失败，耗时："+(rotateMillis + decodeMillis)+"毫秒"+(rotateMillis > 0?"；旋转耗时："+rotateMillis+"毫秒":"")+"；解码耗时："+decodeMillis+"毫秒");
			}
			barcodeScanner.getDecodeResultHandler().sendFailureMessage();
		}
	}
	
	/**
	 * 发送解码消息
	 * @param data 数据
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
