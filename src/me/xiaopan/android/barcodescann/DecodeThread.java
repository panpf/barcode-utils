package me.xiaopan.android.barcodescann;

import java.util.concurrent.CountDownLatch;

import android.os.Looper;

/**
 * 解码线程
 */
class DecodeThread extends Thread{
	private DecodeHandler decodeHandler;
	private BarcodeScanner barcodeScanner;
	private CountDownLatch handlerInitLatch;
	
	public DecodeThread(BarcodeScanner barcodeDecoder) {
		this.barcodeScanner = barcodeDecoder;
		handlerInitLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		Looper.prepare();
		decodeHandler = new DecodeHandler(barcodeScanner);
		handlerInitLatch.countDown();
		Looper.loop();
	}

	/**
	 * 获取解码处理器
	 * @return
	 */
	DecodeHandler getDecodeHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
		}
		return decodeHandler;
	}

	/**
	 * 获取条码扫描器
	 * @return
	 */
	BarcodeScanner getBarcodeScanner() {
		return barcodeScanner;
	}
}