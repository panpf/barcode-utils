package me.xiaopan.barcodescanner;

import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

public class Utils {
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
	
	/**
	 * 解码Bitmap
	 * @param bitmap 要解码的Bitmap
	 * @param multiFormatReader 支持多种格式的读取器
	 * @return 解码结果
	 * @throws NotFoundException 在Bitmap中没有找到二维码时抛出此异常
	 */
	public static final Result decodeBitmap(Bitmap bitmap, MultiFormatReader multiFormatReader) throws NotFoundException{
    	int width = bitmap.getWidth();
    	int height = bitmap.getHeight();
    	int[] pixels = new int[width * height];
    	bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    	bitmap.recycle();
		return multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels))));
	}
	
	/**
	 * 解码Bitmap
	 * @param bitmap 要解码的Bitmap
	 * @param hints 解码选项集
	 * @return 解码结果
	 * @throws NotFoundException 在Bitmap中没有找到二维码时抛出此异常
	 */
	public static final Result decodeBitmap(Bitmap bitmap, Map<DecodeHintType, Object> hints) throws NotFoundException{
		MultiFormatReader multiFormatReader = new MultiFormatReader();
    	multiFormatReader.setHints(hints);
    	return decodeBitmap(bitmap, multiFormatReader);
	}
	
	/**
	 * 解码Bitmap
	 * @param bitmap 要解码的Bitmap
	 * @return 解码结果
	 * @throws NotFoundException 在Bitmap中没有找到二维码时抛出此异常
	 */
	public static final Result decodeBitmap(Bitmap bitmap) throws NotFoundException{
    	return decodeBitmap(bitmap, new MultiFormatReader());
	}
	
	/**
	 * 解码文件
	 * @param imageFilePath 图片文件的路径
	 * @param multiFormatReader 支持多种格式的读取器
	 * @return 解码结果
	 * @throws NotFoundException 在Bitmap中没有找到二维码时抛出此异常
	 */
	public static final Result decodeFile(String imageFilePath, MultiFormatReader multiFormatReader) throws NotFoundException{
    	return decodeBitmap(BitmapFactory.decodeFile(imageFilePath), multiFormatReader);
	}
	
	/**
     * 解码文件
     * @param imageFilePath 图片文件的路径
     * @param hints 解码选项集
     * @return 解码结果
     * @throws NotFoundException 没有在图片中找到二维码时抛出此异常
     */
	public static final Result decodeFile(String imageFilePath, Map<DecodeHintType, Object> hints) throws NotFoundException{
    	return decodeBitmap(BitmapFactory.decodeFile(imageFilePath), hints);
	}
	
	/**
     * 解码文件
     * @param imageFilePath 图片文件的路径
     * @return 解码结果
     * @throws NotFoundException 没有在图片中找到二维码时抛出此异常
     */
	public static final Result decodeFile(String imageFilePath) throws NotFoundException{
    	return decodeBitmap(BitmapFactory.decodeFile(imageFilePath));
	}
	
	/**
	 * 解码YUV格式的源数据（常见于Android Camera的预览数据）
	 * @param yuvImageData YUV格式的源数据
	 * @param dataWidth YUV格式源图的宽
	 * @param dataHeight YUV格式源图的宽
	 * @param left 二维码部分左顶点在YUV格式源图中的X坐标
	 * @param top 二维码部分左顶点在YUV格式源图中的Y坐标
	 * @param width 二维码部分在YUV格式源图中的宽
	 * @param height 二维码部分在YUV格式源图中的高
	 * @param reverseHorizontal 是否需要将YUV格式的源图旋转90度，当手机屏幕是竖屏时此处需要传入true
	 * @param multiFormatReader 支持多种格式的读取器
	 * @return 解码结果
	 * @throws NotFoundException 没有在图片中找到二维码时抛出此异常
	 */
	public static final Result decodeYUV(byte[] yuvImageData, int dataWidth, int dataHeight, int left, int top, int width, int height, boolean reverseHorizontal, MultiFormatReader multiFormatReader) throws NotFoundException{
		return multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new PlanarYUVLuminanceSource(yuvImageData, dataWidth, dataHeight, left, top, width, height, reverseHorizontal))));
	}
	
	/**
	 * 解码YUV格式的源数据（常见于Android Camera的预览数据）
	 * @param yuvImageData YUV格式的源数据
	 * @param dataWidth YUV格式源图的宽
	 * @param dataHeight YUV格式源图的宽
	 * @param left 二维码部分左顶点在YUV格式源图中的X坐标
	 * @param top 二维码部分左顶点在YUV格式源图中的Y坐标
	 * @param width 二维码部分在YUV格式源图中的宽
	 * @param height 二维码部分在YUV格式源图中的高
	 * @param reverseHorizontal 是否需要将YUV格式的源图旋转90度，当手机屏幕是竖屏时此处需要传入true
	 * @param hints 解码选项集
	 * @return 解码结果
	 * @throws NotFoundException 没有在图片中找到二维码时抛出此异常
	 */
	public static final Result decodeYUV(byte[] yuvImageData, int dataWidth, int dataHeight, int left, int top, int width, int height, boolean reverseHorizontal, Map<DecodeHintType, Object> hints) throws NotFoundException{
		MultiFormatReader multiFormatReader = new MultiFormatReader();
    	multiFormatReader.setHints(hints);
		return decodeYUV(yuvImageData, dataWidth, dataHeight, left, top, width, height, reverseHorizontal, multiFormatReader);
	}
	
	/**
	 * 解码YUV格式的源数据（常见于Android Camera的预览数据）
	 * @param yuvImageData YUV格式的源数据
	 * @param dataWidth YUV格式源图的宽
	 * @param dataHeight YUV格式源图的宽
	 * @param left 二维码部分左顶点在YUV格式源图中的X坐标
	 * @param top 二维码部分左顶点在YUV格式源图中的Y坐标
	 * @param width 二维码部分在YUV格式源图中的宽
	 * @param height 二维码部分在YUV格式源图中的高
	 * @param reverseHorizontal 是否需要将YUV格式的源图旋转90度，当手机屏幕是竖屏时此处需要传入true
	 * @return 解码结果
	 * @throws NotFoundException 没有在图片中找到二维码时抛出此异常
	 */
	public static final Result decodeYUV(byte[] yuvImageData, int dataWidth, int dataHeight, int left, int top, int width, int height, boolean reverseHorizontal) throws NotFoundException{
		return decodeYUV(yuvImageData, dataWidth, dataHeight, left, top, width, height, reverseHorizontal, new MultiFormatReader());
	}
}