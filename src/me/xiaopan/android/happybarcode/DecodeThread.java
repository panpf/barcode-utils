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

package me.xiaopan.android.happybarcode;

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