package me.xiaopan.easy.barcode;

import java.util.concurrent.CountDownLatch;

import android.os.Looper;

/**
 * 解码线程
 */
class DecodeThread extends Thread{
	private DecodeHandler decodeHandler;
	private BarcodeDecoder barcodeDecoder;
	private CountDownLatch handlerInitLatch;
	
	public DecodeThread(BarcodeDecoder barcodeDecoder) {
		this.barcodeDecoder = barcodeDecoder;
		handlerInitLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		Looper.prepare();
		decodeHandler = new DecodeHandler(this);
		handlerInitLatch.countDown();
		Looper.loop();
	}

	/**
	 * 获取解码处理器
	 * @return
	 */
	private DecodeHandler getDecodeHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
		}
		return decodeHandler;
	}
	
	/**
	 * 解码
	 * @param data 源数据
	 */
	public void decode(byte[] data) {
		getDecodeHandler().sendDecodeMessage(data);
	}
	
	/**
	 * 释放
	 */
	public void release(){
		getDecodeHandler().sendQuitMessage();
	}

	/**
	 * 获取条码解码器
	 * @return
	 */
	BarcodeDecoder getBarcodeDecoder() {
		return barcodeDecoder;
	}
}