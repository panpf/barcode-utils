package me.xiaopan.barcodescanner;

import com.google.zxing.Result;

/**
 * 解码监听器
 */
public interface DecodeListener {
	/**
	 * 解码成功
	 * @param result
	 * @param bitmapByteArray
	 * @param scaleFactory
	 */
	public void onDecodeSuccess(Result result, byte[] bitmapByteArray, float scaleFactory);
	/**
	 * 解码失败
	 */
	public void onDecodeFailure();
}