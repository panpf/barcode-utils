package me.xiaopan.barcode;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;

/**
 * 解码消息处理器
 */
public class DecodeMessageHandler extends Handler{
	public static final String PARAM_BYTE_ARRAY_BARCODE_BITMAP = "PARAM_BYTE_ARRAY_BARCODE_BITMAP";
	public static final String PARAM_FLOAT_BARCODE_SCALED_FACTOR = "PARAM_FLOAT_BARCODE_SCALED_FACTOR";
	public static final int MESSAGE_WHAT_DECODE_SUCCESS = 1001;
	public static final int MESSAGE_WHAT_DECODE_FAILURE = 1002;
	private Decoder decoder;
	
	public DecodeMessageHandler(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_WHAT_DECODE_SUCCESS:
			if(decoder.getDecodeListener() != null){
				Bundle bundle = msg.getData();
				decoder.getDecodeListener().onDecodeSuccess((Result) msg.obj, bundle.getByteArray(PARAM_BYTE_ARRAY_BARCODE_BITMAP), bundle.getFloat(PARAM_FLOAT_BARCODE_SCALED_FACTOR));
			}
			break;
		case MESSAGE_WHAT_DECODE_FAILURE:
			if(decoder.getDecodeListener() != null){
				decoder.getDecodeListener().onDecodeFailure();
			}
			break;
		}
	}
}