/*
 * Copyright 2013 Peng fei Pan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.xiaopan.barcodescanner;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.xiaopan.barcodescanner.Utils.ScreenSize;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.view.OrientationEventListener;

/**
 * 相机工具箱
 */
public class CameraUtils {
	/**
	 * 获取最佳的预览尺寸
	 * @param context
	 * @param camera
	 * @return
	 */
	public static Camera.Size getOptimalPreviewSize(Context context, Camera camera) {
		Camera.Size optimalSize = null;
		List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
		if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0){
			ScreenSize screenSize = Utils.getScreenSize(context);
			final double ASPECT_TOLERANCE = 0.1;
			double minDiff = Double.MAX_VALUE;
			
			//计算最佳的宽高比例
			double targetRatio = (double) screenSize.width / screenSize.height;
			int targetHeight = screenSize.height;
			
			//视图找到一个宽高和屏幕最接近的尺寸
			for (Camera.Size size : supportedPreviewSizes) {
				double ratio = (double) size.width / size.height;
				if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
			
			//当找不到的时候
			if (optimalSize == null) {
				minDiff = Double.MAX_VALUE;
				for (Camera.Size size : supportedPreviewSizes) {
					if (Math.abs(size.height - targetHeight) < minDiff) {
						optimalSize = size;
						minDiff = Math.abs(size.height - targetHeight);
					}
				}
			}
		}

        return optimalSize;
    }
	
	/**
	 * 试图找到一个最接近屏幕的预览和输出尺寸
	 * @param context
	 * @param camera
	 * @return
	 */
	public static Camera.Size getBestPreviewAndPictureSize(Context context, Camera camera){
		ScreenSize screenSize = Utils.getScreenSize(context);
		boolean landscape = screenSize.width > screenSize.height;
		
		//如果是竖屏就将宽高互换
		if(!landscape){
			screenSize.width = screenSize.width + screenSize.height;
			screenSize.height = screenSize.width - screenSize.height;
			screenSize.width = screenSize.width - screenSize.height;
		}
		
		Camera.Parameters cameraParameters = camera.getParameters();
		List<Camera.Size> supportPreviewSizes = cameraParameters.getSupportedPreviewSizes();
		List<Camera.Size> supportPictureSizes = cameraParameters.getSupportedPictureSizes();
		Comparator<Camera.Size> comparator = new Comparator<Camera.Size>() {
			@Override
			public int compare(android.hardware.Camera.Size lhs, android.hardware.Camera.Size rhs) {
				return (lhs.width - rhs.width) * -1;
			}
		};
		Collections.sort(supportPreviewSizes, comparator);
		Collections.sort(supportPictureSizes, comparator);
		
		Iterator<Camera.Size> supportPreviewSizeIterator;
		Iterator<Camera.Size> supportPictureSizeIterator;
		Camera.Size currentPreviewSize;
		Camera.Size currentPictureSize;
		
		//先剔除预览尺寸集合中大于屏幕尺寸的
		supportPreviewSizeIterator = supportPreviewSizes.iterator();
		while(supportPreviewSizeIterator.hasNext()){
			currentPreviewSize = supportPreviewSizeIterator.next();
			if(currentPreviewSize.width > screenSize.width || currentPreviewSize.height > screenSize.height){
				supportPreviewSizeIterator.remove();
			}
		}
		
		//然后剔除输出尺寸集合中大于屏幕尺寸的
		supportPictureSizeIterator = supportPictureSizes.iterator();
		while(supportPictureSizeIterator.hasNext()){
			currentPictureSize = supportPictureSizeIterator.next();
			if(currentPictureSize.width > screenSize.width || currentPictureSize.height > screenSize.height){
				supportPictureSizeIterator.remove();
			}
		}
		
		//最后找到相同的
		Camera.Size result = null;
		supportPreviewSizeIterator = supportPreviewSizes.iterator();
		while(supportPreviewSizeIterator.hasNext()){
			currentPreviewSize = supportPreviewSizeIterator.next();
			supportPictureSizeIterator = supportPictureSizes.iterator();
			while(supportPictureSizeIterator.hasNext()){
				currentPictureSize = supportPictureSizeIterator.next();
				if(currentPreviewSize.width == currentPictureSize.width && currentPreviewSize.height == currentPictureSize.height){
					result = currentPictureSize;
					break;
				}else if(currentPreviewSize.width > currentPictureSize.width || currentPreviewSize.height > currentPictureSize.height){
					supportPreviewSizeIterator.remove();
					break;
				}else{
					supportPictureSizeIterator.remove();
				}
			}
			
			if(result != null){
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * 根据当前窗口的显示方向设置相机的显示方向
	 * @param activity 用来获取当前窗口的显示方向
	 * @param cameraId 相机ID，用于区分是前置摄像头还是后置摄像头，在API级别xiaoyu9d系统下此参数无用
	 */
	public static int getOptimalDisplayOrientationByWindowDisplayRotation(Activity activity, int cameraId) {      
		int degrees = WindowUtils.getDisplayRotation(activity);      
		if(Build.VERSION.SDK_INT >= 9){
			Camera.CameraInfo info = new Camera.CameraInfo();      
			Camera.getCameraInfo(cameraId, info);      
			int result;
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {          
				result = (info.orientation + degrees) % 360;          
				result = (360 - result) % 360;    
			} else {
				result = (info.orientation - degrees + 360) % 360;      
			}      
			return result;  
		}else{
			return 0; 
		}
	}
	
	/**
	 * 根据当前窗口的显示方向设置相机的显示方向
	 * @param activity 用来获取当前窗口的显示方向
	 * @param cameraId 相机ID，用于区分是前置摄像头还是后置摄像头
	 * @param camera
	 */
	public static void setDisplayOrientationByWindowDisplayRotation(Activity activity, int cameraId, Camera camera) {      
		Camera.CameraInfo info = new Camera.CameraInfo();      
		Camera.getCameraInfo(cameraId, info);      
		int degrees = WindowUtils.getDisplayRotation(activity);      
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {          
			result = (info.orientation + degrees) % 360;          
			result = (360 - result) % 360;    
		} else {
			result = (info.orientation - degrees + 360) % 360;      
		}      
		camera.setDisplayOrientation(result);  
	}
	
	/**
	 * @param orientation OrientationEventListener类中onOrientationChanged()方法的参数
	 * @param cameraId
	 * @return
	 */
	public static int getOptimalParametersOrientationByWindowDisplayRotation(int orientation, int cameraId) {
		if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN){
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			orientation = (orientation + 45) / 90 * 90;
			
			//计算方向
			int rotation = 0;
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				rotation = (info.orientation - orientation + 360) % 360;
			} else {
				rotation = (info.orientation + orientation) % 360;
			}
			return rotation;
		}else{
			return -1;
		}
	}
	
	/**
	 *  OrientationEventListener类中onOrientationChanged()方法的参数
	 * @param orientation
	 * @param cameraId
	 * @param camera
	 */
	public static void setParametersOrientationByWindowDisplayRotation(int orientation, int cameraId, Camera camera) {
		int rotation = getOptimalParametersOrientationByWindowDisplayRotation(orientation, cameraId);
		if(rotation >= 0){
			Camera.Parameters parameters = camera.getParameters();
			parameters.setRotation(rotation);
			camera.setParameters(parameters);
		}
	}
	
	/**
	 * 判断给定的相机是否支持给定的闪光模式
	 * @param camera 给定的相机
	 * @param flashMode 给定的闪光模式
	 * @return
	 */
	public static boolean isSupportFlashMode(Camera camera, String flashMode){
		return camera != null?camera.getParameters().getSupportedFlashModes().contains(flashMode):false;
	}
	
	/**
	 * 计算取景框在Picture中的位置，可通过此Rect在Picture上裁剪出取景框中的内容
	 * @param context 上下文 用来判断是横屏还是竖屏
	 * @param surfaceViewWidth SurfaceView的宽度
	 * @param surfaceViewHeight SurfaceView的高度
	 * @param cameraApertureViewInSurfaceViewRect 取景框视图在SurfaceView上的Rect
	 * @param cameraPictureSize 输出图片的分辨率，可通过Camera.getParameters().getPictureSize()获得
	 * @return
	 */
	public static Rect computeCameraApertureInPictureRect(Context context, int surfaceViewWidth, int surfaceViewHeight, Rect cameraApertureViewInSurfaceViewRect, Camera.Size cameraPictureSize){
		Rect finslCameraApertureInSurfaceViewRect = new Rect(cameraApertureViewInSurfaceViewRect);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//如果是横屏
			finslCameraApertureInSurfaceViewRect.left = finslCameraApertureInSurfaceViewRect.left * cameraPictureSize.width / surfaceViewWidth;
			finslCameraApertureInSurfaceViewRect.right = finslCameraApertureInSurfaceViewRect.right * cameraPictureSize.width / surfaceViewWidth;
			finslCameraApertureInSurfaceViewRect.top = finslCameraApertureInSurfaceViewRect.top * cameraPictureSize.height / surfaceViewHeight;
			finslCameraApertureInSurfaceViewRect.bottom = finslCameraApertureInSurfaceViewRect.bottom * cameraPictureSize.height / surfaceViewHeight;
		} else {
			finslCameraApertureInSurfaceViewRect.left = finslCameraApertureInSurfaceViewRect.left * cameraPictureSize.height / surfaceViewWidth;
			finslCameraApertureInSurfaceViewRect.right = finslCameraApertureInSurfaceViewRect.right * cameraPictureSize.height / surfaceViewWidth;
			finslCameraApertureInSurfaceViewRect.top = finslCameraApertureInSurfaceViewRect.top * cameraPictureSize.width / surfaceViewHeight;
			finslCameraApertureInSurfaceViewRect.bottom = finslCameraApertureInSurfaceViewRect.bottom * cameraPictureSize.width / surfaceViewHeight;
		}
		return finslCameraApertureInSurfaceViewRect;
	}
	
	/**
	 * 将YUV格式的图片的源数据从横屏模式转为竖屏模式，注意：将源图片的宽高互换一下就是新图片的宽高
	 * @param sourceData YUV格式的图片的源数据
	 * @param width 源图片的宽
	 * @param height 源图片的高
	 * @return 
	 */
	public static final byte[] yuvLandscapeToPortrait(byte[] sourceData, int width, int height){
		byte[] rotatedData = new byte[sourceData.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = sourceData[x + y * width];
		}
		return rotatedData;
	}
}