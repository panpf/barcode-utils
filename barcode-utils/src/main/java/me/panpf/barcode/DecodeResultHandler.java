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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;

/**
 * 解码结果处理器
 */
class DecodeResultHandler extends Handler {
    private static final int MESSAGE_WHAT_DECODE_SUCCESS = 4567890;
    private static final int MESSAGE_WHAT_DECODE_FAILURE = 4567891;
    private static final String PARAM_OPTIONAL_BYTE_ARRAY_BARCODE_BITMAP = "PARAM_OPTIONAL_BYTE_ARRAY_BARCODE_BITMAP";
    private static final String PARAM_OPTIONAL_FLOAT_BARCODE_SCALED_FACTOR = "PARAM_OPTIONAL_FLOAT_BARCODE_SCALED_FACTOR";

    private BarcodeScanner barcodeScanner;
    private BarcodeScanner.BarcodeScanCallback barcodeScanCallback;

    DecodeResultHandler(BarcodeScanner barcodeScanner, BarcodeScanner.BarcodeScanCallback barcodeScanCallback) {
        this.barcodeScanner = barcodeScanner;
        this.barcodeScanCallback = barcodeScanCallback;
    }

    @Override
    public void handleMessage(Message msg) {
        if (barcodeScanCallback != null && barcodeScanner.isScanning()) {
            switch (msg.what) {
                case MESSAGE_WHAT_DECODE_SUCCESS:
                    if (barcodeScanCallback.onDecodeCallback((Result) msg.obj, msg.getData().getByteArray(PARAM_OPTIONAL_BYTE_ARRAY_BARCODE_BITMAP), msg.getData().getFloat(PARAM_OPTIONAL_FLOAT_BARCODE_SCALED_FACTOR))) {
                        barcodeScanner.requestDecode();
                    } else {
                        barcodeScanner.stop();
                    }
                    break;
                case MESSAGE_WHAT_DECODE_FAILURE:
                    if (barcodeScanCallback.onDecodeCallback(null, null, 0)) {
                        barcodeScanner.requestDecode();
                    } else {
                        barcodeScanner.stop();
                    }
                    break;
            }
        }
    }

    /**
     * 发送成功消息
     */
    void sendSuccessMessage(Result result, byte[] bitmapByteArray, float scaleFactor) {
        Message message = obtainMessage(MESSAGE_WHAT_DECODE_SUCCESS, result);
        Bundle bundle = new Bundle();
        bundle.putByteArray(PARAM_OPTIONAL_BYTE_ARRAY_BARCODE_BITMAP, bitmapByteArray);
        bundle.putFloat(PARAM_OPTIONAL_FLOAT_BARCODE_SCALED_FACTOR, scaleFactor);
        message.setData(bundle);
        message.sendToTarget();
    }

    /**
     * 发送失败消息
     */
    void sendFailureMessage() {
        obtainMessage(MESSAGE_WHAT_DECODE_FAILURE).sendToTarget();
    }
}
