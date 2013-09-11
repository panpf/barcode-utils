package me.xiaopan.barcodescanner;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码线程
 */
public class DecodeThread extends Thread{
	private Decoder decoder;
	private DecodeListener decodeListener;
	private SourceDataHolder sourceDataHolder;
	private boolean running;
	
	public DecodeThread(Decoder decoder, DecodeListener decodeListener) {
		this.decoder = decoder;
		this.decodeListener = decodeListener;
		sourceDataHolder = new SourceDataHolder();
	}
	
	@Override
	public void run(){
		running = true;
		if(decodeListener != null){
			while(running){
				synchronized (sourceDataHolder) {
					if(sourceDataHolder.getSourceData() != null){
						Log.d("TAG", "解码");
						/* 初始化源数据，如果是竖屏的话就将源数据旋转90度 */
						int previewWidth = decoder.getCameraPreviewSize().width;
						int previewHeight = decoder.getCameraPreviewSize().height;
						if (decoder.isPortrait()) {
							sourceDataHolder.setSourceData(CameraUtils.yuvLandscapeToPortrait(sourceDataHolder.getSourceData(), previewWidth, previewHeight));
							previewWidth = previewWidth + previewHeight;
							previewHeight = previewWidth - previewHeight;
							previewWidth = previewWidth - previewHeight;
						}
						
						/* 解码 */
						Result decodeResult = null;
						PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(sourceDataHolder.getSourceData(), previewWidth, previewHeight, decoder.getBarcodeCameraApertureInPreviewRect().left, decoder.getBarcodeCameraApertureInPreviewRect().top, decoder.getBarcodeCameraApertureInPreviewRect().width(), decoder.getBarcodeCameraApertureInPreviewRect().height(), false);
						try {
							decodeResult = decoder.getMultiFormatReader().decodeWithState(new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource)));
						} catch (Exception re) {
						} finally {
							decoder.getMultiFormatReader().reset();
						}
						
						/* 解码结果处理 */
						if (decodeResult != null) {
							decoder.pause();
							if(decodeListener != null){
								int[] pixels = planarYUVLuminanceSource.renderThumbnail();
								int width = planarYUVLuminanceSource.getThumbnailWidth();
								int height = planarYUVLuminanceSource.getThumbnailHeight();
								Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
								ByteArrayOutputStream out = new ByteArrayOutputStream();    
								bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
								decodeListener.onDecodeSuccess(decodeResult, out.toByteArray(), (float) width / planarYUVLuminanceSource.getWidth());
							}
						} else {
							if(decodeListener != null){
								decodeListener.onDecodeFailure();
							}
						}
						
						sourceDataHolder.setSourceData(null);	//清除源数据
					}else{
						try {
							sourceDataHolder.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * 终止线程
	 */
	public void finish(){
		running = false;
		synchronized (sourceDataHolder) {
			try{
				sourceDataHolder.notify();
			}catch(Exception exception){
				exception.printStackTrace();
			}
		}
	}
	
//	/**
//	 * 暂停
//	 */
//	public void pause(){
//		if(sourceDataHolder.getSourceData() != null){
//			synchronized (sourceDataHolder) {
//				sourceDataHolder.setSourceData(null);
//				try{
//					sourceDataHolder.notify();
//				}catch(Exception exception){
//					exception.printStackTrace();
//				}
//			}
//		}
//	}

	/**
	 * 尝试解码
	 * @param sourceData
	 */
	public void tryDecode(byte[] sourceData) {
		if(sourceData != null && sourceDataHolder.getSourceData() == null){
			synchronized (sourceDataHolder) {
				sourceDataHolder.setSourceData(sourceData);
				try{
					sourceDataHolder.notify();
				}catch(Exception exception){
					exception.printStackTrace();
				}
			}
		}
	}
}