package me.xiaopan.barcodescanner;

import android.graphics.Bitmap;

import com.google.zxing.common.BitMatrix;

/**
 * 编码工具箱
 */
public class EncodeUtils {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	
	/**
	 * 将BitMatrix格式的源数据转换为Bitmap
	 * @param bitMatrix
	 * @return
	 */
	public static Bitmap bitMatrixToBitmap(BitMatrix bitMatrix) {
		int width = bitMatrix.getWidth();
		int height = bitMatrix.getHeight();
		int[] pixels = new int[width * height];
		
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}
}