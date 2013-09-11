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

import java.io.IOException;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.view.SurfaceHolder;

/**
 * 相机管理器
 */
public class CameraManager implements SurfaceHolder.Callback, Camera.AutoFocusCallback{
	private Activity activity;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private Camera.Parameters cameraParameters;
	private int frontCameraId = -1;
	private int backCameraId = -1;
	private int currentCameraId = -1;
	private CameraCallback cameraCallback;
	private boolean resumeRestore;//是否需要在Activity Resume的时候恢复
	private int focusIntervalTime;//两次对焦的间隔时间
	private long lastFocusTime;//上次对焦的时间
	private int displayOrientation;	//显示方向
	
	public CameraManager(Activity activity, SurfaceHolder surfaceHolder, CameraCallback cameraCallback){
		this.activity = activity;
		this.surfaceHolder = surfaceHolder;
		this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		this.surfaceHolder.addCallback(this);
		this.cameraCallback = cameraCallback;
		
		//获取前置和后置摄像头的ID
		if(SystemUtils.getAPILevel() >= 9){
			int cameraNumbers = Camera.getNumberOfCameras();
			CameraInfo cameraInfo = new CameraInfo();
			for(int w = 0; w < cameraNumbers; w++){
				Camera.getCameraInfo(w, cameraInfo);
				if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
					frontCameraId = w;
				}else if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
					backCameraId = w;
				}
			}
		}
	}
	
	/**
	 * 打开后置摄像头
	 */
	public void openBackCamera(){
		try {
			camera = backCameraId != -1?Camera.open(backCameraId):Camera.open();
			currentCameraId = backCameraId;
			cameraParameters = camera.getParameters();
			//初始化Camera的方法是在surfaceCreated()方法里调用的，开启预览是在surfaceChanged()方法中调用的，
			//当屏幕是竖屏的时候按下电源键系统会锁屏，并且Activity会进入onPause()中并释放相机，
			//然而再解锁回到应用的时候只会调用onResume()方法，而不会调用surfaceCreated()和surfaceChanged()方法，所以Camera不会被初始化，也不会开启预览，显示这样是不行的。
			//所以我们要在Activity暂停释放Camera的时候做一个标记，当再次在onResume()中执行本方法打开摄像头的时候要初始化Camera并开启预览
			//另外当SurfaceView被销毁的时候要标记为不需要恢复，因为只要SurfaceView被销毁那么接下来必然会执行surfaceCreated()和surfaceChanged()方法
			if(resumeRestore){
				resumeRestore = false;
				initCamera();
				startPreview();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(camera != null){
				camera.release();
				camera = null;
				cameraParameters = null;
			}
			if(cameraCallback != null){
				cameraCallback.onOpenCameraException(e);
			}
		}
	}
	
	/**
	 * 打开前置摄像头
	 * @throws Exception 没有前置摄像头 
	 */
	public void openForntCamera() throws Exception{
		if(frontCameraId != -1){
			try {
				camera = Camera.open(frontCameraId);
				currentCameraId = frontCameraId;
				cameraParameters = camera.getParameters();
				//初始化Camera的方法是在surfaceCreated()方法里调用的，开启预览是在surfaceChanged()方法中调用的，
				//当屏幕是竖屏的时候按下电源键系统会锁屏，并且Activity会进入onPause()中并释放相机，
				//然而再解锁回到应用的时候只会调用onResume()方法，而不会调用surfaceCreated()和surfaceChanged()方法，所以Camera不会被初始化，也不会开启预览，显示这样是不行的。
				//所以我们要在Activity暂停释放Camera的时候做一个标记，当再次在onResume()中执行本方法打开摄像头的时候要初始化Camera并开启预览
				//另外当SurfaceView被销毁的时候要标记为不需要恢复，因为只要SurfaceView被销毁那么接下来必然会执行surfaceCreated()和surfaceChanged()方法
				if(resumeRestore){
					resumeRestore = false;
					initCamera();
					startPreview();
				}
			} catch (Exception e) {
				e.printStackTrace();
				if(camera != null){
					camera.release();
					camera = null;
					cameraParameters = null;
				}
				if(cameraCallback != null){
					cameraCallback.onOpenCameraException(e);
				}
			}
		}else{
			throw new Exception();
		}
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		initCamera();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stopPreview();
		resumeRestore = false;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if(cameraCallback != null){
			cameraCallback.onAutoFocus(success, camera);
		}
	}

	/**
	 * 开始预览
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean startPreview(){
		if(camera != null){
			camera.startPreview();
			autoFocus();
			if(cameraCallback != null){
				cameraCallback.onStartPreview();
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 停止预览
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean stopPreview(){
		if(camera != null){
			camera.stopPreview();
			if(cameraCallback != null){
				cameraCallback.onStopPreview();
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 释放
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean release(){
		if (camera != null) {
			stopPreview();
			try {
				camera.setPreviewDisplay(null);
			} catch (IOException e) {
				e.printStackTrace();
			}
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
			cameraParameters = null;
			resumeRestore = true;
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 自动对焦
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean autoFocus(){
		if(camera != null){
			if(focusIntervalTime > 0){
				long currentTime = System.currentTimeMillis();
				if(lastFocusTime == 0 || currentTime - lastFocusTime >= focusIntervalTime){
					camera.autoFocus(this);
					lastFocusTime = currentTime;
				}
			}else{
				camera.autoFocus(this);
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 拍照
	 * @param shutter 快门回调
	 * @param raw RAW格式图片回调
	 * @param jpeg JPEG格式图片回调
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg){
		if(camera != null){
			camera.takePicture(shutter, raw, jpeg);
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 设置闪光模式
	 * @param newFlashMode
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean setFlashMode(String newFlashMode){
		if(camera != null){
			cameraParameters.setFlashMode(newFlashMode);
			camera.setParameters(cameraParameters);
			return true;
		}else{
			return false;
		}
	}
	
	public int getDisplayOrientation() {
		return displayOrientation;
	}

	/**
	 * 设置显示方向
	 * @param displayOrientation
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	public boolean setDisplayOrientation(int displayOrientation){
		if(camera != null){
			this.displayOrientation = displayOrientation;
			if(SystemUtils.getAPILevel() >= 9){
				camera.setDisplayOrientation(displayOrientation);
			}else{
				cameraParameters.setRotation(displayOrientation);
				camera.setParameters(cameraParameters);
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 初始化Camera
	 * @return true：调用成功；false：调用失败，原因是camera尚未初始化
	 */
	private boolean initCamera(){
		if(camera != null){
			try {
				camera.setPreviewDisplay(surfaceHolder);
				if(cameraCallback != null){
					/* 设置最佳（最接近屏幕尺寸的）预览和图片输出分辨率 */
					Camera.Size bestSize = CameraUtils.getBestPreviewAndPictureSize(activity, camera);
					if(bestSize != null){
						cameraParameters.setPreviewSize(bestSize.width, bestSize.height);
						cameraParameters.setPictureSize(bestSize.width, bestSize.height);
					}else{
						cameraParameters.setPreviewSize(640, 480);
						cameraParameters.setPictureSize(640, 480);
					}
					camera.setParameters(cameraParameters);
					
					//设置预览界面旋转角度
					if(SystemUtils.getAPILevel() >= 9){
						setDisplayOrientation(CameraUtils.getOptimalDisplayOrientationByWindowDisplayRotation(activity, getCurrentCameraId()));
					}else{
						//如果是当前竖屏就将预览角度顺时针旋转90度
						if (!WindowUtils.isLandscape(activity)) {
							setDisplayOrientation(90);
						}
					}
					
					cameraCallback.onInitCamera(camera, cameraParameters);	//回调初始化
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 获取两次对焦的间隔时间
	 * @return 两次对焦的间隔时间，单位毫秒
	 */
	public int getFocusIntervalTime() {
		return focusIntervalTime;
	}

	/**
	 * 设置两次对焦的间隔时间
	 * @param focusIntervalTime 两次对焦的间隔时间，单位毫秒
	 */
	public void setFocusIntervalTime(int focusIntervalTime) {
		this.focusIntervalTime = focusIntervalTime;
	}
	
	public Camera getCamera() {
		return camera;
	}

	public void setCameraCallback(CameraCallback cameraCallback) {
		this.cameraCallback = cameraCallback;
	}

	public int getCurrentCameraId() {
		return currentCameraId;
	}

	public Camera.Parameters getCameraParameters() {
		return cameraParameters;
	}

	public interface CameraCallback{
		public void onInitCamera(Camera camera, Camera.Parameters cameraParameters);
		public void onAutoFocus(boolean success, Camera camera);
		public void onOpenCameraException(Exception e);
		public void onStartPreview();
		public void onStopPreview();
	}
}