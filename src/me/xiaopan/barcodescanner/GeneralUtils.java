package me.xiaopan.barcodescanner;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class GeneralUtils {
	/**
	 * 获取当前屏幕的尺寸
	 * @param context
	 * @return
	 */
	public static ScreenSize getScreenSize(Context context){
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		ScreenSize size = new ScreenSize(display.getWidth(), display.getHeight());
		return size;
	}
	
	/**
	 * 屏幕尺寸
	 */
	public static class ScreenSize {
		public int width;	
		public int height;
		
		public ScreenSize() {}
		
		public ScreenSize(int width, int height){
			this.width = width;
			this.height = height;
		}
	}
	
	/**
	 * 获取当前窗口的旋转角度
	 * @param activity
	 * @return
	 */
	public static int getDisplayRotation(Activity activity) {
		switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
			case Surface.ROTATION_0 : return 0;
			case Surface.ROTATION_90 : return 90;
			case Surface.ROTATION_180 : return 180;
			case Surface.ROTATION_270 : return 270;
			default : return 0;
		}
	}
	
	/**
	 * 当前是否是横屏
	 * @param context
	 * @return
	 */
	public static final boolean isLandscape(Context context){
		return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}
}