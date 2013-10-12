package me.xiaopan.barcode;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;

public class DecodeHandler extends Handler{
	public static final String BARCODE_BITMAP = "barcode_bitmap";
	public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";
	public static final int MESSAGE_DECODE_SUCCESS = 1001;
	public static final int MESSAGE_DECODE_FAILURE = 1002;
	
	private Decoder decoder;
	
	public DecodeHandler(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_DECODE_SUCCESS:
			if(decoder.getDecodeListener() != null){
				Bundle bundle = msg.getData();
				decoder.getDecodeListener().onDecodeSuccess((Result) msg.obj, bundle.getByteArray(BARCODE_BITMAP), bundle.getFloat(BARCODE_SCALED_FACTOR));
			}
			break;
		case MESSAGE_DECODE_FAILURE:
			if(decoder.getDecodeListener() != null){
				decoder.getDecodeListener().onDecodeFailure();
			}
			break;
		}
	}
}