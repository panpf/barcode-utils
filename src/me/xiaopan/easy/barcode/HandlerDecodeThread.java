package me.xiaopan.easy.barcode;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Looper;

import com.google.zxing.DecodeHintType;

/**
 * 解码线程
 */
public class HandlerDecodeThread extends Thread{
	private boolean isPortrait;
	private Rect scanningAreaRect;
	private Camera.Size cameraPreviewSize;
	private DecodeHandler decodeHandler;
	private HandlerDecoder decoder;
	private CountDownLatch handlerInitLatch;
	private Map<DecodeHintType, Object> hints;
	
	public HandlerDecodeThread(HandlerDecoder decoder, Map<DecodeHintType, Object> hints,  Camera.Size cameraPreviewSize, Rect scanningAreaRect, boolean isPortrait) {
		this.decoder = decoder;
		this.hints = hints;
		this.cameraPreviewSize = cameraPreviewSize;
		this.scanningAreaRect = scanningAreaRect;
		this.isPortrait = isPortrait;
		handlerInitLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		Looper.prepare();
		decodeHandler = new DecodeHandler(decoder, hints, cameraPreviewSize, scanningAreaRect, isPortrait);
		handlerInitLatch.countDown();
		Looper.loop();
	}

	/**
	 * 获取解码处理器
	 * @return
	 */
	public DecodeHandler getDecodeHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
		}
		return decodeHandler;
	}
}