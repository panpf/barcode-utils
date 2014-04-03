package me.xiaopan.android.barcodescanner.sample;

import android.app.Activity;
import android.content.Intent;
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
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import me.xiaopan.android.barcodescanner.BarcodeScanner;
import me.xiaopan.android.barcodescanner.DecodeUtils;
import me.xiaopan.android.barcodescanner.R;
import me.xiaopan.android.barcodescanner.ScanAreaView;
import me.xiaopan.android.easy.hardware.camera.AutoFocusManager;
import me.xiaopan.android.easy.hardware.camera.BestPreviewSizeCalculator;
import me.xiaopan.android.easy.util.AndroidLogger;
import me.xiaopan.android.easy.util.RectUtils;
import me.xiaopan.android.easy.util.ViewUtils;
import me.xiaopan.android.easy.util.WindowUtils;

import java.io.IOException;

/**
 * 条码扫描Activity
 */
public class BarcodeScanActivity extends Activity{
	public static final String RETURN_BARCODE_CONTENT = "RETURN_BARCODE_CONTENT";
	private static final String STATE_FLASH_CHECKED = "STATE_FLASH_CHECKED";
	private static final int REQUEST_CODE_GET_IMAGE = 46231;
	private int beepId;
	private int number;
	private View createQRCodeButton;
	private View imageDecodeButton;
	private TextView barcodeText;
	private TextView numberText;
	private SoundPool soundPool;
	private SurfaceView surfaceView;	
	private ToggleButton flashButton;
	private ToggleButton modeToggleButton;
	private ScanAreaView scanAreaView;
	private BarcodeScanner barcodeScanner;
	private CameraManager cameraManager;
	private AutoFocusManager autoFocusManager;
	private Speedometer speedometer;
    private View upDoor;
    private View downDoor;
    private Handler handler;
    private Runnable openCameraRunnable;
    private Runnable pauseRunnable;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/* 设置全屏模式 */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_decode);
		surfaceView = (SurfaceView) findViewById(R.id.surface_decode);
		scanAreaView = (ScanAreaView) findViewById(R.id.scannArea_decode);
		flashButton = (ToggleButton) findViewById(R.id.toggleButton_decode_flash);
		modeToggleButton = (ToggleButton) findViewById(R.id.toggleButton_decode_mode);
		barcodeText = (TextView) findViewById(R.id.text_decode_content);
		numberText = (TextView) findViewById(R.id.text_decode_number);
		createQRCodeButton = findViewById(R.id.button_decode_createQRCode);
		imageDecodeButton = findViewById(R.id.button_decode_imageDecode);
        upDoor = findViewById(R.id.layout_main_doorUp);
        downDoor = findViewById(R.id.layout_main_doorDown);

        handler = new Handler();
        openCameraRunnable = new Runnable(){
            @Override
            public void run() {
                if(cameraManager != null){
                    try {
                        cameraManager.openBackCamera();
                    } catch (CameraManager.CameraBeingUsedException e) {
                        e.printStackTrace();
                        Toast.makeText(getBaseContext(), "无法打开您的摄像头，请确保摄像头没有被其它程序占用", Toast.LENGTH_SHORT).show();
                        BarcodeScanActivity.super.onBackPressed();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        pauseRunnable = new Runnable(){
            @Override
            public void run() {
                upDoor.setVisibility(View.VISIBLE);
                downDoor.setVisibility(View.VISIBLE);
            }
        };
		
		//点击扫描区域开始解码
		scanAreaView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(barcodeScanner != null){
					barcodeScanner.start();
				}
			}
		});
		
		//点击闪光灯按钮切换闪光灯常亮状态
		flashButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(!cameraManager.setTorchFlash(isChecked)){
					Toast.makeText(getBaseContext(), "您的设备不支持闪光灯常亮功能", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		modeToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(barcodeScanner != null){
					barcodeScanner.setReturnBitmap(!isChecked);
				}
			}
		});
		
		//点击生成二维码按钮启动生成二维码界面
		createQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getBaseContext(), BarcodeGenerateActivity.class));
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
		cameraManager = new CameraManager(this, surfaceView.getHolder(), new MyCameraCallback());
		cameraManager.setDebugMode(true);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		beepId = soundPool.load(getBaseContext(), R.raw.beep, 100);

        autoFocusManager = new AutoFocusManager(null);
		speedometer = new Speedometer();

        barcodeScanner = new BarcodeScanner(getBaseContext(), new BarcodeScanListener());
        barcodeScanner.setDebugMode(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        // 延迟100毫秒启动相机
        handler.postDelayed(openCameraRunnable, 100);
	}	
	
	@Override
	protected void onPause() {
		super.onPause();
        // 取消启动相机的Runnable
        handler.removeCallbacks(openCameraRunnable);

        cameraManager.release();

        // 延迟500毫秒显示门
        handler.postDelayed(pauseRunnable, 500);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_FLASH_CHECKED, flashButton.isChecked());
		super.onSaveInstanceState(outState);
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
								numberText.setText(result.getText());
							}else{
								numberText.setText("解码失败");
							}
						} catch (Exception e) {
							e.printStackTrace();
							numberText.setText("解码失败");
						}
                    }else{
                    	Toast.makeText(getBaseContext(), "图片不存在", Toast.LENGTH_SHORT).show();
                    }
					break;
			}
		}
	}

    @Override
    public void onBackPressed() {
        cameraManager.stopPreview();
        barcodeScanner.stop();
        // 执行关门动画，在动画执行完毕后退出Activity
        Animation upDoorCloseAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.base_slide_to_bottom_in);
        upDoorCloseAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        Animation downDoorCloseAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.base_slide_to_top_in);
        downDoorCloseAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        downDoorCloseAnimation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {
                BarcodeScanActivity.super.onBackPressed();
            }
        });
        upDoor.setVisibility(View.VISIBLE);
        downDoor.setVisibility(View.VISIBLE);
        upDoor.startAnimation(upDoorCloseAnimation);
        downDoor.startAnimation(downDoorCloseAnimation);
    }

	@Override
	protected void onDestroy() {
		cameraManager = null;
		if(soundPool != null){
			soundPool.release();
			soundPool = null;
		}
        if(barcodeScanner != null){
            barcodeScanner.release();
        }
		super.onDestroy();
	}

    private class MyCameraCallback implements CameraManager.CameraCallback {
        @Override
        public void onInitCamera(Camera camera) {
		        /* 设置预览分辨率 */
            Camera.Parameters parameters = camera.getParameters();
            Size bestPreviewSize = new BestPreviewSizeCalculator(getBaseContext(), parameters.getSupportedPreviewSizes()).getPreviewSize();
            if(bestPreviewSize != null){
                parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
                camera.setParameters(parameters);
            }

            //设置相机和扫描区域
            barcodeScanner.setCamera(camera);
            Size previewSize = camera.getParameters().getPreviewSize();
            barcodeScanner.setScanAreaRectInPreview(RectUtils.mappingRect(ViewUtils.getRelativeRect(scanAreaView, surfaceView), new Point(surfaceView.getWidth(), surfaceView.getHeight()), new Point(previewSize.width, previewSize.height), WindowUtils.isPortrait(getBaseContext())));

            autoFocusManager.setCamera(camera);
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {

        }

        @Override
        public void onStartPreview() {
            // 执行开门动画
            Animation upDoorOpenAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.base_slide_to_top_out);
            upDoorOpenAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            upDoorOpenAnimation.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationRepeat(Animation animation) {}
                public void onAnimationEnd(Animation animation) {
                    upDoor.setVisibility(View.GONE);
                }
            });
            Animation downDoorOpenAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.base_slide_to_bottom_out);
            downDoorOpenAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            downDoorOpenAnimation.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationRepeat(Animation animation) {}
                public void onAnimationEnd(Animation animation) {
                    downDoor.setVisibility(View.GONE);
                    if(barcodeScanner != null){
                        barcodeScanner.start();
                    }
                }
            });
            upDoor.startAnimation(upDoorOpenAnimation);
            downDoor.startAnimation(downDoorOpenAnimation);
        }

        @Override
        public void onStopPreview() {
            if(barcodeScanner != null){
                barcodeScanner.stop();
            }
        }
    }

    private class BarcodeScanListener implements me.xiaopan.android.barcodescanner.BarcodeScanListener{
        @Override
        public void onStartScan() {
            scanAreaView.startRefresh();
            autoFocusManager.start();
        }

        @Override
        public void onFoundPossibleResultPoint(ResultPoint resultPoint) {
            if(scanAreaView != null){
                scanAreaView.addResultPoint(resultPoint);
            }
        }

        @Override
        public boolean onFoundBarcode(final Result result, final byte[] barcodeBitmapByteArray, final float scaleFactor) {
            speedometer.count();
            if(!modeToggleButton.isChecked()){//如果是单扫模式
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                if(audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL){
                    float volume = (float) (((float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 15) / 3.0);
                    soundPool.play(beepId, volume, volume, 100, 0, 1);
                }
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(200);

			    /* 将返回的位图绘制在扫描区上 */
                Bitmap bitmap = BitmapFactory.decodeByteArray(barcodeBitmapByteArray, 0, barcodeBitmapByteArray.length);
                final Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                bitmap.recycle();
                DecodeUtils.drawResultPoints(newBitmap, scaleFactor, result, 0xc099cc00);
                scanAreaView.drawResultBitmap(newBitmap);
            }

		    /* 显示条码内容并计数加1 */
            barcodeText.setText(result.getText());
            numberText.setText("总数："+(++number)+"；速度："+speedometer.computePerSecondSpeed());

            return modeToggleButton.isChecked();
        }

        @Override
        public void onUnfoundBarcode() {
            AndroidLogger.e("没有找到条码");
            numberText.setText("总数："+(number)+"；速度："+0);
        }

        @Override
        public void onStopScan() {
            scanAreaView.stopRefresh();
            autoFocusManager.stop();
        }

        @Override
        public void onRelease() {

        }
    }
}
