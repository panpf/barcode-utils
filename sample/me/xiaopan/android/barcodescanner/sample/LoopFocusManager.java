package me.xiaopan.android.barcodescanner.sample;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

/**
 * 循环对焦管理器，可以实现永不间断循环对焦，适用于条码扫描器
 *
 * <br>首先你需要在打开相机后调用setCamera()方法设置Camera，然后调用start()或stop()方法启动或停止对焦
 */
public class LoopFocusManager{
    private static final String LOG_TAG = LoopFocusManager.class.getSimpleName();
    private int focusFrequency = 2000; // 对焦频率，单位毫秒
    private boolean running;    // 标识运行状态
    private boolean debugMode;  // 标识debug状态
    private Camera camera;
    private Handler handler;
    private FocusRunnable focusRunnable;
    private Camera.AutoFocusCallback autoFocusCallback;

    public LoopFocusManager(Camera.AutoFocusCallback autoFocusCallback){
        this.handler = new Handler();
        this.focusRunnable = new FocusRunnable();
        this.autoFocusCallback = autoFocusCallback;
    }

    public LoopFocusManager(){
        this(null);
    }

    /**
     * 设置Camera
     * @param camera 相机
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * 开始对焦
     * @param delayFocus 延迟对焦，如果为true就会延迟focusFrequency毫秒后对焦，否则立即对焦
     */
    public void start(boolean delayFocus){
        if(!running && this.camera != null){
            running = true;
            if(delayFocus){
                if(debugMode){
                    Log.w(LOG_TAG, "延迟"+focusFrequency+"毫秒后对焦");
                }
                handler.postDelayed(focusRunnable, focusFrequency);
            }else{
                if(debugMode){
                    Log.d(LOG_TAG, "立即对焦");
                }
                handler.post(focusRunnable);
            }
        }
    }

    /**
     * 停止对焦
     */
    public void stop(){
        if(running){
            if(debugMode){
                Log.e(LOG_TAG, "停止对焦");
            }
            handler.removeCallbacks(focusRunnable);
            running = false;
        }
    }

    /**
     * 是否正在运行中
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 设置对焦频率，默认为每2000毫秒对焦一次
     * @param focusFrequency 对焦频率，单位毫秒
     */
    public void setFocusFrequency(int focusFrequency) {
        this.focusFrequency = focusFrequency;
    }

    /**
     * 设置自动对焦回调
     * @param autoFocusCallback
     */
    public void setAutoFocusCallback(Camera.AutoFocusCallback autoFocusCallback) {
        this.autoFocusCallback = autoFocusCallback;
    }

    /**
     * 设置是否开启Debug模式
     * @param debugMode 是否开启Debug模式，开始后将会在控制台输出LOG
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    private class FocusRunnable implements Runnable{
        @Override
        public void run() {
            if(running && camera != null){
                if(debugMode){
                    Log.d(LOG_TAG, "对焦");
                }
                camera.autoFocus(autoFocusCallback);
                handler.postDelayed(this, focusFrequency);
            }
        }
    }
}