package me.xiaopan.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

public class DecodeActivity extends Activity implements CameraManager.CameraCallback, Camera.PreviewCallback, ResultPointCallback, DecodeListener {
	private static final String STATE_FLASH_CHECKED = "STATE_FLASH_CHECKED";
	private static final int REQUEST_CODE_GET_IMAGE = 46231;
	public static final String RETURN_BARCODE_CONTENT = "RETURN_BARCODE_CONTENT";
	private int beepId;//哔哔音效
	private SurfaceView surfaceView;	//显示画面的视图
	private ScanningAreaView scanningAreaView;//扫描框（取景器）
	private Decoder decoder;	//解码器
	private SoundPool soundPool;//音效池
	private CameraManager cameraManager;
	private RefreshScanFrameRunnable refreshScanFrameRunnable;
	private Handler handler;
	private TextView hintText;
	private ToggleButton flashButton;
	private View createQRCodeButton;
	private View imageDecodeButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_decode);
		surfaceView = (SurfaceView) findViewById(R.id.surface_decode);
		scanningAreaView = (ScanningAreaView) findViewById(R.id.scanningArea_decode);
		flashButton = (ToggleButton) findViewById(R.id.checkBox_decode_flash);
		hintText = (TextView) findViewById(R.id.text_decode_hint);
		createQRCodeButton = findViewById(R.id.button_decode_createQRCode);
		imageDecodeButton = findViewById(R.id.button_decode_imageDecode);
		
		scanningAreaView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startDecode();//开始解码
			}
		});
		
		flashButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setEnableTorckFlashMode(isChecked);
			}
		});
		
		createQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getBaseContext(), EncodeActivity.class));
			}
		});
		
		imageDecodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, REQUEST_CODE_GET_IMAGE);
			}
		});
		
		if(savedInstanceState != null){
			flashButton.setChecked(savedInstanceState.getBoolean(STATE_FLASH_CHECKED));
		}
		
		//初始化相机管理器
		cameraManager = new CameraManager(this, surfaceView.getHolder(), this);
		cameraManager.setFocusIntervalTime(3000);
		
		//初始化刷新扫描框的处理器
		handler = new Handler();
		refreshScanFrameRunnable = new RefreshScanFrameRunnable();
		
		//设置扫描框的描边宽度
		scanningAreaView.setStrokeWidth(2);
		
		//初始化音效
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		beepId = soundPool.load(getBaseContext(), R.raw.beep, 100);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		cameraManager.openBackCamera();
		setEnableTorckFlashMode(flashButton.isChecked());
	}	
	
	@Override
	protected void onPause() {
		super.onPause();
		cameraManager.release();
	}

	@Override
	protected void onDestroy() {
		cameraManager = null;
		soundPool.release();
		soundPool = null;
		decoder = null;
		refreshScanFrameRunnable = null;
		handler = null;
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_FLASH_CHECKED, flashButton.isChecked());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onInitCamera(Camera camera, Camera.Parameters cameraParameters) {
		camera.setPreviewCallback(this);//设置预览回调
		if(decoder == null){	//如果解码器尚未创建的话，就创建解码器并设置其监听器
			decoder = new Decoder(getBaseContext(), camera.getParameters(), scanningAreaView);
			decoder.setResultPointCallback(this);	//设置可疑点回调
			decoder.setDecodeListener(this);	//设置解码监听器
		}
	}

	@Override
	public void onOpenCameraException(Exception e) {
		e.printStackTrace();
		Toast.makeText(getBaseContext(), "无法打开您的摄像头，请确保摄像头没有被其它程序占用", Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (!success) {//如果没有对好就继续对
			cameraManager.autoFocus();
		}
	}

	@Override
	public void onStartPreview() {
		startDecode();	//开始解码
	}

	@Override
	public void onStopPreview() {
		stopDecode();	//停止解码
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		decoder.decode(data);
	}
	
	@Override
	public void foundPossibleResultPoint(ResultPoint arg0) {
		scanningAreaView.addPossibleResultPoint(arg0);
	}

	@Override
	public void onDecodeSuccess(Result result, byte[] barcodeBitmapByteArray, float scaleFactory) {
		stopDecode();//停止解码
		playSound();//播放音效
		playVibrator();//发出震动提示
		scanningAreaView.drawResultBitmap(BitmapFactory.decodeByteArray(barcodeBitmapByteArray, 0, barcodeBitmapByteArray.length));
		hintText.setText(result.getText());
		getIntent().putExtra(RETURN_BARCODE_CONTENT, result.getText());
		setResult(RESULT_OK, getIntent());
	}

	@Override
	public void onDecodeFailure() {
		if(cameraManager != null){
			cameraManager.autoFocus();//继续对焦
		}
	}

	private class RefreshScanFrameRunnable implements Runnable{
		@Override
		public void run() {
			if(scanningAreaView != null && handler != null){
				scanningAreaView.refresh();
				handler.postDelayed(refreshScanFrameRunnable, 50);
			}
		}
	}

	/**
	 * 开始解码
	 */
	private void startDecode(){
		if(decoder != null){
			startRefreshScanFrame();//开始刷新扫描框
			cameraManager.autoFocus();// 自动对焦
			decoder.resume();
		}
	}
	
	/**
	 * 停止解码
	 */
	private void stopDecode(){
		if(decoder != null){
			stopRefreshScanFrame();//停止刷新扫描框
			decoder.pause();//停止解码器
		}
	}
	
	/**
	 * 开始刷新扫描框
	 */
	public void startRefreshScanFrame(){
		handler.post(refreshScanFrameRunnable);
	}
	
	/**
	 * 停止刷新扫描框
	 */
	public void stopRefreshScanFrame(){
		handler.removeCallbacks(refreshScanFrameRunnable);
	}
	
	/**
	 * 播放音效
	 */
	private void playSound(){
		//播放音效
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		if(audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL){
			float volume = (float) (((float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 15) / 3.0);
			soundPool.play(beepId, volume, volume, 100, 0, 1);
		}
	}
	
	/**
	 * 震动
	 */
	private void playVibrator(){
		((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(200);//发出震动提示
	}
	
	/**
	 * 设置是否激活常亮闪光模式
	 * @param enable
	 */
	private void setEnableTorckFlashMode(boolean enable){
		if(cameraManager != null){
			if(enable){
				if(CameraUtils.isSupportFlashMode(cameraManager.getCamera(), Camera.Parameters.FLASH_MODE_TORCH)){
					cameraManager.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				}else{
					Toast.makeText(getBaseContext(), "您的设备不支持闪光灯常亮功能", Toast.LENGTH_SHORT).show();
					flashButton.setChecked(false);
				}
			}else{
				cameraManager.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){
			switch(requestCode){
				case REQUEST_CODE_GET_IMAGE : 
					Uri imageUri = data.getData();
					String imageFilePath = imageUri.getPath();
                    //如果不是一个路径而是一个ContentProvider的URI
                    if (!TextUtils.isEmpty(imageUri.getAuthority())){
                         Cursor cursor = getContentResolver().query(imageUri, new String[]{ MediaStore.Images.Media.DATA }, null, null, null);
                         if (cursor != null){
                              cursor.moveToFirst();
                              imageFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                         }else{
                              imageFilePath = null;
                         }
                    }
                    if(imageFilePath != null && !"".equals(imageFilePath.trim())){
                    	try {
							Result result = Utils.decodeFile(imageFilePath);
							if(result != null){
								hintText.setText(result.getText());
							}else{
								hintText.setText("解码失败");
							}
						} catch (Exception e) {
							e.printStackTrace();
							hintText.setText("解码失败");
						}
                    }else{
                    	Toast.makeText(getBaseContext(), "图片不存在", Toast.LENGTH_SHORT).show();
                    }
					break;
			}
		}
	}
}
