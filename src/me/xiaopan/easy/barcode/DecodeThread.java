package me.xiaopan.easy.barcode;

import java.io.ByteArrayOutputStream;

import me.xiaopan.easy.android.util.camera.CameraUtils;
import me.xiaopan.easy.java.util.BoundedQueue;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码线程
 */
public class DecodeThread extends Thread{
	private boolean running;	//运行中
	private boolean isPortrait;	//是否是竖屏
	private Rect scanningAreaRect;	//扫描框相对于预览界面的矩形
	private Camera.Size cameraPreviewSize;	//相机预览尺寸
	private DecodeListener decodeListener;	//解码监听器
	private BoundedQueue<byte[]> yuvSources;	//yuv源数据
	private MultiFormatReader multiFormatReader;	//多格式解码器
	private boolean returnBitmap = true;
	
	public DecodeThread(MultiFormatReader multiFormatReader, DecodeListener decodeListener, Camera.Size cameraPreviewSize, Rect scanningAreaRect, boolean isPortrait) {
		this.multiFormatReader = multiFormatReader;
		this.decodeListener = decodeListener;
		this.cameraPreviewSize = cameraPreviewSize;
		this.scanningAreaRect = scanningAreaRect;
		this.isPortrait = isPortrait;
		yuvSources = new BoundedQueue<byte[]>(1);
	}
	
	@Override
	public void run(){
		running = true;
		byte[] yuvSource;
		while(running){
			yuvSource = yuvSources.poll();
			if(yuvSource != null){
				decode(yuvSource);
			}else{
				synchronized (yuvSources) {
					try {
						yuvSources.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void decode(byte[] yuvSource){
		/* 初始化源数据，如果是竖屏的话就将源数据旋转90度 */
		int previewWidth = cameraPreviewSize.width;
		int previewHeight = cameraPreviewSize.height;
		if (isPortrait) {
			yuvSource = CameraUtils.yuvLandscapeToPortrait(yuvSource, previewWidth, previewHeight);
			previewWidth = previewWidth + previewHeight;
			previewHeight = previewWidth - previewHeight;
			previewWidth = previewWidth - previewHeight;
		}
		
		/* 解码 */
		Result decodeResult = null;
		PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(yuvSource, previewWidth, previewHeight, scanningAreaRect.left, scanningAreaRect.top, scanningAreaRect.width(), scanningAreaRect.height(), false);
		try {
			decodeResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource)));
		} catch (Exception re) {
			re.printStackTrace();
		} finally {
			multiFormatReader.reset();
		}
		
		/* 解码结果处理 */
		if (decodeResult != null) {
			byte[] bitmapData = null;
			float scaleFactor = 0.0f;
			if(isReturnBitmap()){
				int[] pixels = planarYUVLuminanceSource.renderThumbnail();
				int width = planarYUVLuminanceSource.getThumbnailWidth();
				int height = planarYUVLuminanceSource.getThumbnailHeight();
				Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
				ByteArrayOutputStream out = new ByteArrayOutputStream();    
				bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
				bitmap.recycle();
				bitmapData = out.toByteArray();
				scaleFactor = (float) width / planarYUVLuminanceSource.getWidth();
			}
			decodeListener.onDecodeSuccess(decodeResult, bitmapData, scaleFactor);
		} else {
			decodeListener.onDecodeFailure();
		}
	}
	
	/**
	 * 终止线程
	 */
	public void finish(){
		running = false;
		synchronized (yuvSources) {
			try{
				yuvSources.clear();
				yuvSources.notify();
			}catch(Exception exception){
				exception.printStackTrace();
			}
		}
	}
	
	/**
	 * 暂停解码
	 */
	public void pause(){
		synchronized (yuvSources) {
			yuvSources.clear();
		}
	}
	
	/**
	 * 尝试解码
	 * @param yuvSource
	 */
	public void tryDecode(byte[] yuvSource) {
		synchronized (yuvSources) {
			yuvSources.add(yuvSource);
			try{
				yuvSources.notify();
			}catch(Exception exception){
				exception.printStackTrace();
			}
		}
	}
	
	public boolean isReturnBitmap() {
		return returnBitmap;
	}

	public void setReturnBitmap(boolean returnBitmap) {
		this.returnBitmap = returnBitmap;
	}

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
}