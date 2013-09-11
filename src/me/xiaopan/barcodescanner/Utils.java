package me.xiaopan.barcodescanner;

import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

public class Utils {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	
	public static Bitmap BitMatrixToBitmap(BitMatrix result) {
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}
	
	public static final Result decode(MultiFormatReader multiFormatReader, Bitmap bitmap){
    	int width = bitmap.getWidth();
    	int height = bitmap.getHeight();
    	int[] pixels = new int[width * height];
    	bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    	bitmap.recycle();
    	try {
    		return multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels))));
		} catch (NotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static final Result decode(Bitmap bitmap, Map<DecodeHintType, Object> hints){
		MultiFormatReader multiFormatReader = new MultiFormatReader();
    	multiFormatReader.setHints(hints);
    	return decode(multiFormatReader, bitmap);
	}
	
	public static final Result decode(Bitmap bitmap){
    	return decode(new MultiFormatReader(), bitmap);
	}
	
	public static final Result decode(MultiFormatReader multiFormatReader, String imageFilePath){
    	return decode(multiFormatReader, BitmapFactory.decodeFile(imageFilePath));
	}
	
	public static final Result decode(String imageFilePath, Map<DecodeHintType, Object> hints){
    	return decode(BitmapFactory.decodeFile(imageFilePath), hints);
	}
	
	public static final Result decode(String imageFilePath){
    	return decode(BitmapFactory.decodeFile(imageFilePath));
	}
}