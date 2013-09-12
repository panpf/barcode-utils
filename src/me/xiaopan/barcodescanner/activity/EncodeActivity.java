package me.xiaopan.barcodescanner.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Map;

import me.xiaopan.barcodescanner.EncodeUtils;
import me.xiaopan.barcodescanner.R;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 编码界面
 */
public class EncodeActivity extends Activity{
	private View createButton;
	private EditText contentEdit;
	private ImageView qrcodeImage;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_encode);
		createButton = findViewById(R.id.button_encode_create);
		contentEdit = (EditText) findViewById(R.id.edit_encode_content);
		qrcodeImage = (ImageView) findViewById(R.id.image_encode_qrcode);
		
		createButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String content = contentEdit.getEditableText().toString().trim();
				if(!TextUtils.isEmpty(content)){
					Bitmap qrcodeBitmap = null;
					Bitmap icBitmap = null;
					try {
						//设置容错率，因为要在二维码上绘制logo，所以必须要有容错率，要不然二维无法识别
						Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
						hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
						hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
						
						qrcodeBitmap = EncodeUtils.bitMatrixToBitmap(new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300, hints));
						icBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, null);
						int left = (qrcodeBitmap.getWidth() - icBitmap.getWidth())/2;
						int top = (qrcodeBitmap.getHeight() - icBitmap.getHeight())/2;
						Paint paint = new Paint();
						Canvas canvas = new Canvas(qrcodeBitmap);
						canvas.drawBitmap(icBitmap, left, top, paint);
						icBitmap.recycle();
						
						File dir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName());
						if(!dir.exists()){
							dir.mkdirs();
						}
						
						File newFile = new File(dir.getPath() + File.separator + System.currentTimeMillis() + ".jpeg");
						if(newFile.createNewFile()){
							qrcodeBitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(newFile));
							qrcodeImage.setImageBitmap(qrcodeBitmap);
							Toast.makeText(getBaseContext(), "已保存至 "+newFile.getPath(), Toast.LENGTH_SHORT).show();
						}else{
							throw new Exception();
						}
					} catch (Exception e) {
						e.printStackTrace();
						if(qrcodeBitmap != null && !qrcodeBitmap.isRecycled()){
							qrcodeBitmap.recycle();
						}
						if(icBitmap != null && !icBitmap.isRecycled()){
							icBitmap.recycle();
						}
						Toast.makeText(getBaseContext(), "生成失败", Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(getBaseContext(), "内容不能为空", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}