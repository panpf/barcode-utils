package me.xiaopan.android.barcode.sample;

import java.io.File;

import me.xiaopan.android.barcode.BarcodeCreator;
import me.xiaopan.android.barcode.R;
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
 * 条码生成Activity
 */
public class BarcodeGenerateActivity extends Activity{
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
						BarcodeCreator barcodeCreator = new BarcodeCreator(content, BarcodeFormat.QR_CODE, 500, 500);
						barcodeCreator.setOutFile(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName() + File.separator + System.currentTimeMillis() + ".jpeg"));
						qrcodeImage.setImageBitmap(barcodeCreator.create(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, null)));
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