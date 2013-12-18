# ![Logo](https://github.com/ixiaopan/EasyBarcode/raw/master/res/drawable-mdpi/ic_launcher.png) EasyBarcode

这是一个基于Zxing基础上封装的Android条码扫描库，适合快速在项目中集成扫码功能。目前兼容Zxing核心库版本是2.3.0，点击下载：**[zxing-core-2.3.0.jar](https://github.com/ixiaopan/EasyBarcode/raw/master/downloads/zxing-core-2.3.0.jar)**


##Usage Guide
###1.创建BarcodeScanner
在初始化Camera的时候创建BarcodeScanner
```java
/* 初始化解码器 */
if(barcodeScanner == null){
	Size previewSize = camera.getParameters().getPreviewSize();
	Rect scanAreaInPreviewRect = Utils.mappingRect(new Point(surfaceView.getWidth(), surfaceView.getHeight()), ViewUtils.getRelativeRect(scanAreaView, surfaceView), new Point(previewSize.width, previewSize.height), getBaseContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
	barcodeScanner = new BarcodeScanner(getBaseContext(), previewSize,  scanAreaInPreviewRect, null, new MyBarcodeScanListener());
	barcodeScanner.setDebugMode(true);
}
```


###2.开始解码
在Camera启动预览的时候执行barcodeScanner.start()启动解码
```java
camera.startPreview();
barcodeScanner.start(camera);
```


###3.处理解码结果以及可疑点
```java
private class MyBarcodeScanListener implements BarcodeScanListener{
	@Override
	public void onFoundPossibleResultPoint(ResultPoint resultPoint) {
//		你可以在这里将可疑点绘制到你的界面上
//		具体如何绘制你可以参考Zxing的ViewfinderView.java的addPossibleResultPoint()方法或者参考本库中的ScanAreaView.java的addPossibleResultPoint()方法
//		你还可以直接使用本库自带的ScanAeaView.java来作为扫描区，具体使用方式你可以参考本项目中的BarcodeScanActivity.java
	}

	@Override
	public void onFoundBarcode(final Result result, final byte[] barcodeBitmapByteArray, final float scaleFactor) {
		Toast.makeText(getBaseContext(), "条码内容："+result.getText(), Toast.LENGTH_LONG).show();
//		如果你想在识别到条码后暂停识别就在此调用以代码
//		barcodeDecoder.stop();
//		当你暂停了之后需要再次识别就调用以下代码
//		barcodeDecoder.start();
	}

	@Override
	public void onUnfoundBarcode() {
	}
}
```


###4.停止解码
在Camera停止预览的时候执行barcodeScanner.stop()停止解码
```java
camera.stopPreview();
barcodeScanner.stop();
```


###5.释放BarcodeScanner
重写Activity的onDestroy()方法，在方法内部释放BarcodeScanner
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
###5.完整使用请参考BarcodeScanActivity.java

##Change Log
###1.0.9
>* BarcodeDecoder.java改名为BarcodeScanner.java
>* DecodeListener.java改名为BarcodeScanListener.java，并将foundPossibleResultPoint()方法改为onFoundPossibleResultPoint()、onDecodeSuccess()方法改为onDecodeFailure()、foundPossibleResultPoint()方法改为onUnfoundBarcode()

**[easy-barcode-1.0.9.jar](https://github.com/ixiaopan/EasyBarcode/raw/master/downloads/easy-barcode-1.0.9.jar)**

###1.0.8
>* 去掉BarcodeDecoder的setCamera()方法，改由在BarcodeDecoder的start()方法中传入Camera

###1.0.7
>* Decoder.java改名为BarcodeDecoder.java；
>* 优化了解码线程的实现方式使之更加稳定、节省内存，解码性能大幅提升；
>* 将ResultPointCallback的回调移到了DecodeListener中，使用更方便；
>* EasyBarcode不再依赖于EasyAndroid核心库。

##License
```java
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
