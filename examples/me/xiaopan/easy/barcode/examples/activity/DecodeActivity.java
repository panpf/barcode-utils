package me.xiaopan.easy.barcode.examples.activity;

import me.xiaopan.easy.android.util.CameraManager;
import me.xiaopan.easy.android.util.CameraOptimalSizeCalculator;
import me.xiaopan.easy.android.util.CameraUtils;
import me.xiaopan.easy.android.util.Utils;
import me.xiaopan.easy.android.util.ViewUtils;
import me.xiaopan.easy.barcode.DecodeListener;
import me.xiaopan.easy.barcode.DecodeUtils;
import me.xiaopan.easy.barcode.Decoder;
import me.xiaopan.easy.barcode.R;
import me.xiaopan.easy.barcode.ScanAreaView;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
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
	public static final String RETURN_BARCODE_CONTENT = "RETURN_BARCODE_CONTENT";
	private static final String STATE_FLASH_CHECKED = "STATE_FLASH_CHECKED";
	private static final int REQUEST_CODE_GET_IMAGE = 46231;
	private int beepId;
	private View createQRCodeButton;
	private View imageDecodeButton;
	private Decoder decoder;
	private TextView hintText;
	private SoundPool soundPool;
	private SurfaceView surfaceView;	
	private ToggleButton flashButton;
	private ScanAreaView scanAreaView;
	private CameraManager cameraManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/* 设置全屏模式 */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_decode);
		surfaceView = (SurfaceView) findViewById(R.id.surface_decode);
		scanAreaView = (ScanAreaView) findViewById(R.id.scannArea_decode);
		flashButton = (ToggleButton) findViewById(R.id.checkBox_decode_flash);
		hintText = (TextView) findViewById(R.id.text_decode_hint);
		createQRCodeButton = findViewById(R.id.button_decode_createQRCode);
		imageDecodeButton = findViewById(R.id.button_decode_imageDecode);
		
		//点击扫描区域开始解码
		scanAreaView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startDecode();
			}
		});
		
		//点击闪光灯按钮切换闪光灯常亮状态
		flashButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setEnableTorckFlashMode(isChecked);
			}
		});
		
		//点击生成二维码按钮启动生成二维码界面
		createQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getBaseContext(), EncodeActivity.class));
			}
		});
		
		//点击图片解码按钮打开图库选择图片，选定图片后就会立即对选定的图片解码
		imageDecodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, REQUEST_CODE_GET_IMAGE);
			}
		});
		
		//如果是恢复状态就重置闪光灯的按钮的常量状态
		if(savedInstanceState != null){
			flashButton.setChecked(savedInstanceState.getBoolean(STATE_FLASH_CHECKED));
		}
		
		/* 初始化 */
		cameraManager = new CameraManager(this, surfaceView.getHolder(), this);
		cameraManager.setFocusIntervalTime(2500);
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
		if(soundPool != null){
			soundPool.release();
			soundPool = null;
		}
		if(decoder != null){
			decoder.release();
			decoder = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_FLASH_CHECKED, flashButton.isChecked());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onInitCamera(Camera camera) {
		/* 设置预览回调和预览分辨率 */
		camera.setPreviewCallback(this);
		Camera.Parameters parameters = camera.getParameters();
		Size optimalPreviewSize = new CameraOptimalSizeCalculator().getPreviewSize(surfaceView.getWidth(), surfaceView.getHeight(), parameters.getSupportedPreviewSizes());
		parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
		camera.setParameters(parameters);
		
		/* 初始化解码器 */
		if(decoder == null){
			Size previewSize = camera.getParameters().getPreviewSize();
			decoder = new Decoder(getBaseContext(), previewSize, 
					Utils.mappingRect(new Point(surfaceView.getWidth(), surfaceView.getHeight()), ViewUtils.getRelativeRect(scanAreaView, surfaceView), new Point(previewSize.width, previewSize.height), getBaseContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
					, null, null);
			decoder.setResultPointCallback(DecodeActivity.this);
			decoder.setDecodeListener(DecodeActivity.this);
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
		if (!success) {
			cameraManager.autoFocus();
		}
	}

	@Override
	public void onStartPreview() {
		startDecode();
	}

	@Override
	public void onStopPreview() {
		stopDecode();	
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if(decoder != null){
			decoder.decode(data);
		}
	}
	
	@Override
	public void foundPossibleResultPoint(ResultPoint arg0) {
		scanAreaView.addResultPoint(arg0);
	}

	@Override
	public void onDecodeSuccess(Result result, byte[] barcodeBitmapByteArray, float scaleFactor) {
		stopDecode();
		playSound();
		playVibrator();
		
		Bitmap bitmap = BitmapFactory.decodeByteArray(barcodeBitmapByteArray, 0, barcodeBitmapByteArray.length);
		Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		bitmap.recycle();
		DecodeUtils.drawResultPoints(newBitmap, scaleFactor, result, getResources().getColor(R.color.result_points));
		scanAreaView.drawResultBitmap(newBitmap);
		hintText.setText(result.getText());
		
		getIntent().putExtra(RETURN_BARCODE_CONTENT, result.getText());
		setResult(RESULT_OK, getIntent());
	}

	@Override
	public void onDecodeFailure() {
		if(cameraManager != null){
			cameraManager.autoFocus();
		}
	}

	/**
	 * 开始解码
	 */
	private void startDecode(){
		if(decoder != null){
			scanAreaView.startRefresh();
			cameraManager.autoFocus();
			decoder.resume();
		}
	}
	
	/**
	 * 停止解码
	 */
	private void stopDecode(){
		scanAreaView.stopRefresh();
		if(decoder != null){
			decoder.pause();
		}
	}
	
	/**
	 * 播放音效
	 */
	private void playSound(){
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
		((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(200);
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
							Result result = DecodeUtils.decodeFile(imageFilePath);
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
