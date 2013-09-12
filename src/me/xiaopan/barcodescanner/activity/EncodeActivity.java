package me.xiaopan.barcodescanner.activity;

import java.io.File;

import me.xiaopan.barcodescanner.EncodeUtils;
import me.xiaopan.barcodescanner.R;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;

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
					try {
						qrcodeImage.setImageBitmap(EncodeUtils.encode(content, BarcodeFormat.QR_CODE, null, 400, 400, null, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, null), new File(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName()).getPath() + File.separator + System.currentTimeMillis() + ".jpeg")));
//						qrcodeImage.setImageBitmap(EncodeUtils.encode(content, BarcodeFormat.CODE_39, null, 200, 100, null, null, new File(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName()).getPath() + File.separator + System.currentTimeMillis() + ".jpeg")));
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getBaseContext(), "生成失败", Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(getBaseContext(), "内容不能为空", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}