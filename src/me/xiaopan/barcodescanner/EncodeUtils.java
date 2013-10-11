package me.xiaopan.barcodescanner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 编码工具箱
 */
public class EncodeUtils {
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
				pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}
	
	/**
	 * 生成条码
	 * @param content 条码内容
	 * @param barcodeFormat 条码格式，可以是二维码或者一维条码
	 * @param charset 条码内容编码方式
	 * @param width 条码宽度
	 * @param height 条码高度
	 * @param hints 选项集
	 * @param logoBitmap 可以在条码中间显示一个logo
	 * @param outFile 输出文件
	 * @return Bitmap
	 * @throws Exception
	 */
	public static final Bitmap encode(String content, BarcodeFormat barcodeFormat, String charset, int width, int height, Map<EncodeHintType, Object> hints, Bitmap logoBitmap, File outFile) throws Exception{
		Bitmap qrcodeBitmap = null;
		try {
			if(hints == null){
				hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
			}
			
			//根据Logo的大小同输出二维码的宽高设置合适的容错率
			if(logoBitmap != null){
				int logoWidth = logoBitmap.getWidth();
				int logoHeight = logoBitmap.getHeight();
				if(logoWidth > logoHeight){
					hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel((float) logoWidth/(float) width));
				}else{
					hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel((float) logoHeight/(float) height));
				}
			}
			
			//设置编码方式
			if(!hints.containsKey(EncodeHintType.CHARACTER_SET)){
				if(charset != null && !"".equals(charset.trim())){
					hints.put(EncodeHintType.CHARACTER_SET, charset);
				}else{
					hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
				}
			}
			
			//生成二维码
			qrcodeBitmap = EncodeUtils.bitMatrixToBitmap(new MultiFormatWriter().encode(content, barcodeFormat, width, height, hints));
			
			//绘制Logo
			if(logoBitmap != null){
				Paint paint = new Paint();
				Canvas canvas = new Canvas(qrcodeBitmap);
				canvas.drawBitmap(logoBitmap, (qrcodeBitmap.getWidth() - logoBitmap.getWidth())/2, (qrcodeBitmap.getHeight() - logoBitmap.getHeight())/2, paint);
				logoBitmap.recycle();
			}
			
			//将二维码输出到本地
			if(outFile != null){
				if(!outFile.exists()){
					File parentFile = outFile.getParentFile();
					if(!parentFile.exists()){
						parentFile.mkdirs();
					}
				}
				
				if(outFile.createNewFile()){
					qrcodeBitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(outFile));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(qrcodeBitmap != null && !qrcodeBitmap.isRecycled()){
				qrcodeBitmap.recycle();
				qrcodeBitmap = null;
			}
			if(logoBitmap != null && !logoBitmap.isRecycled()){
				logoBitmap.recycle();
				logoBitmap = null;
			}
			throw e;
		}
		return qrcodeBitmap;
	}
	
	/**
	 * 根据logo和二维码图的大小比例获取合适的容错级别
	 * @param proportion
	 * @return
	 */
	private static final ErrorCorrectionLevel getErrorCorrectionLevel(float proportion){
		if(proportion <= 0.07){
			return ErrorCorrectionLevel.L;
		}else if(proportion <= 0.15){
			return ErrorCorrectionLevel.M;
		}else if(proportion <= 0.25){
			return ErrorCorrectionLevel.Q;
		}else{
			return ErrorCorrectionLevel.H;
		}
	}
}