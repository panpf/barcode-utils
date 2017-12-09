/*
 * Copyright (C) 2017 Peng fei Pan <sky@panpf.me>
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

package me.panpf.barcode;

import android.os.Looper;

import java.util.concurrent.CountDownLatch;

/**
 * 解码线程
 */
class DecodeThread extends Thread {
    private DecodeHandler decodeHandler;
    private BarcodeScanner barcodeScanner;
    private CountDownLatch handlerInitLatch;

    DecodeThread(BarcodeScanner barcodeDecoder) {
        this.barcodeScanner = barcodeDecoder;
        handlerInitLatch = new CountDownLatch(1);
    }

    @Override
    public void run() {
        Looper.prepare();
        decodeHandler = new DecodeHandler(barcodeScanner);
        handlerInitLatch.countDown();
        Looper.loop();
    }

    /**
     * 获取解码处理器
     */
    DecodeHandler getDecodeHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // ie.printStackTrace();
        }
        return decodeHandler;
    }
}