# ![Logo](https://github.com/xiaopansky/HappyBarcode/raw/master/res/drawable-mdpi/ic_launcher.png) HappyBarcode

HappyBarcode是Android上的一个条码解析、生成、扫描库，其基于Zxing封装，适合快速在项目中集成条码相关功能，目前兼容Zxing核心库版本是2.3.0，最低兼容Android2.2

##Usage Guide

###解码：
快捷的解码方法都集中在DecodeUtils中，包括decodeFile()、decodeYUV()、decodeBitmap()三种方法，示例如下：
```java
Result result = DecodeUtils.decodeFile(new File("/mnt/sdcard/0/qrcode.png"));
Log.i("条码内容：", result.getText());
```

###生成码：
生成主要使用的是BarcodeCreator，示例如下：
```java
String qrcodeContent = "http://baidu.com";	// 条码内容
BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;	// 条码类型为二维条码
int imageWidth = 500;	// 条码宽为500
int imageHieght = 500;	// 条码高为500
BarcodeCreator barcodeCreator = new BarcodeCreator(qrcodeContent, barcodeFormat, imageWidth, imageHieght);
barcodeCreator.setCharset("UTF-8");	// 设置编码方式
barcodeCreator.setOutFile(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName() + File.separator + System.currentTimeMillis() + ".jpeg"));	// 设置将生成的条码保存到文件
barcodeCreator.setOutCompressFormat(CompressFormat.JPEG);	// 设置输出压缩格式
barcodeCreator.setOutCompressQuality(100);	// 设置输出压缩比例
Bitmap barcodeBitmap = barcodeCreator.create(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, null));	// 生成条码，并且在条码中间绘制当前应用的logo
```

###使用相机扫码：

####1.创建BarcodeScanner
在onCreate()方法中创建BarcodeScanner，例如：
```java
//创建一个支持所有格式，编码方式为"UTF-8"并且默认扫描区域为全屏的条码扫描器
barcodeScanner = new BarcodeScanner(getBaseContext(), this);
barcodeScanner.setDebugMode(true);
```
如果你想自定义扫描格式等信息，可使用其它构造函数

####2.设置Camera和扫描区域
在你打开Camera以后调用setCamera()方法设置Camera，例如：
```java
Camera camera = Camera.open();
barcodeScanner.setCamera(camera);
```
默认的扫描区域是全屏的，如果你想自定义扫描区域的话就调用barcodeScanner.setScanAreaRectInPreview(Rect)方法设置扫描区域，例如：
```java
barcodeScanner.setScanAreaRectInPreview(new Rect(100, 100, 400, 250));
```
        
###3.启动扫描
在执行完camera.startPreview()后调用barcodeScanner.start()启动扫描，例如：
```java
camera.startPreview();
barcodeScanner.start();
```

####4.处理回调事件
```java
private class MyBarcodeScanCallback implements BarcodeScanCallback{
	@Override
	public void onFoundPossibleResultPoint(ResultPoint resultPoint) {
//		你可以在这里将可疑点绘制到你的界面上
//		具体如何绘制你可以参考Zxing的ViewfinderView.java的addPossibleResultPoint()方法或者参考本库中的ScanAreaView.java的addPossibleResultPoint()方法
//		你还可以直接使用本库自带的ScanAeaView.java来作为扫描区，具体使用方式你可以参考本项目中的BarcodeScanActivity.java
	}

	@Override
	public boolean onDecodeCallback(final Result result, final byte[] barcodeBitmapByteArray, final float scaleFactor) {
		if(result != null){
			// 找到条码
			return false;	//停止扫描
		}else{
			// 没有找到他条码
			return true;	//继续扫描
		}
	}
}
```

####5.停止扫描
在需要停止解码的时候调用barcodeScanner.stop()即可停止扫描，例如：
```java
barcodeScanner.stop();
```

####6.释放BarcodeScanner
在需要释放的时候调用barcodeScanner.release()方法即可释放，一般情况下建议重写Activity的onDestroy()方法，在onDestroy()方法内部释放BarcodeScanner
注意：当已经释放了之后再去调用start()方法则会抛出IllegalStateException异常
```java
@Override
protected void onDestroy() {
	if(barcodeScanner != null){
		barcodeScanner.release();
		barcodeScanner = null;
	}
	super.onDestroy();
}
```

完整使用示例请参考BarcodeScanActivity.java

##Downloads
>* **[android-happy-barcode-1.3.0.jar](https://github.com/xiaopansky/HappyBarcode/raw/master/releases/android-happy-barcode-1.3.0.jar)**

>* **[android-happy-barcode-1.3.0-with-src.jar](https://github.com/xiaopansky/HappyBarcode/raw/master/releases/android-happy-barcode-1.3.0-with-src.jar)**

Dependencies
>* **[zxing-core-2.3.0.jar](https://github.com/xiaopansky/HappyBarcode/raw/master/libs/zxing-core-2.3.0.jar)** Required. 条码识别的核心库

##Change Log

###1.2.2
>* 优化扫描回调逻辑

###1.2.1
>* 更新版权信息

###1.2.0
>* 采用全新的命名规则来命名包
>* 优化扫码结果处理逻辑，新逻辑为扫描到条码后通过onFoundBarcode()方法的返回值来确定是否要继续扫描
>* 优化BarcodeScanner的Camera设置逻辑，改为直接调用setCamera()设置一次即可

###1.1.2
>* 修复当因为解码区域超出数据的范围时引发的崩溃问题

###1.1.1
>* 修复当使用setRotationBeforeDecodeOfLandscape()功能的时候会反复旋转扫描区的BUG

###1.1.0
>* BarcodeScanListener.java新增onStartScan()、onStopScan()、onRelease()回调方法
>* BarcodeScanner.java构造函数中去掉Camera.Size cameraPreviewSize参数和Rect scanAreaInPreviewRect参数，改为scanAreaInPreviewRect默认为全屏；cameraPreviewSize在start()方法内部设置
>* BarcodeScanner.java增加多个构造函数，方便直接创建指定解码格式的BarcodeScanner
>* BarcodeScanner.java去掉setWhetherRotatePreview()和setScanAreaRotateMode()方法，替换为setRotationBeforeDecodeOfLandscape()方法，此方法可控制在横屏的时候强制将预览图和扫描区Rect旋转90度再识别

###1.0.9
>* BarcodeDecoder.java改名为BarcodeScanner.java
>* DecodeListener.java改名为BarcodeScanListener.java，并将foundPossibleResultPoint()方法改为onFoundPossibleResultPoint()、onDecodeSuccess()方法改为onFoundBarcode()、foundPossibleResultPoint()方法改为onUnfoundBarcode()
>* BarcodeScanner.java新增setWhetherRotatePreview()方法，可设置在解码之前将预览图旋转90度
>* BarcodeScanner.java新增setScanAreaRotateMode()方法，可设置当扫描区的宽度小于高度时在解码之前将预览图旋转90度

###1.0.8
>* 去掉BarcodeDecoder的setCamera()方法，改由在BarcodeDecoder的start()方法中传入Camera

###1.0.7
>* Decoder.java改名为BarcodeDecoder.java；
>* 优化了解码线程的实现方式使之更加稳定、节省内存，解码性能大幅提升；
>* 将ResultPointCallback的回调移到了DecodeListener中，使用更方便；
>* HappyBarcode不再依赖于EasyAndroid核心库。

##License
```java
/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
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
 ```
