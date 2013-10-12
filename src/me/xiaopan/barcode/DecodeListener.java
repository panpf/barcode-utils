package me.xiaopan.barcode;

import com.google.zxing.Result;

/**
 * 解码监听器
 */
public interface DecodeListener {
	/**
	 * 解码成功
	 * @param result
	 * @param bitmapByteArray
	 * @param scaleFactor
	 */
	public void onDecodeSuccess(Result result, byte[] bitmapByteArray, float scaleFactor);
	
	/**
	 * 解码失败
	 */
	public void onDecodeFailure();
}