# ![Logo](https://github.com/ixiaopan/EasyBarcode/raw/master/res/drawable-mdpi/ic_launcher.png) EasyBarcode

这是一个基于Zxing基础上封装的Android条码扫描库，适合快速在项目中集成扫码功能。目前兼容Zxing核心库版本是2.3.0，点击下载：**[zxing-core-2.3.0.jar](https://github.com/ixiaopan/EasyBarcode/raw/master/downloads/zxing-core-2.3.0.jar)**


##Usage Guide
###1.创建BarcodeScanner
在onCreate()方法中创建BarcodeScanner
```java
//创建一个支持所有格式，编码方式为"UTF-8"并且默认扫描区域为全屏的条码扫描器
barcodeScanner = new BarcodeScanner(getBaseContext(), this);
barcodeScanner.setDebugMode(true);
```
默认扫描区域是全屏的，但是你想自定义扫描区域的话就调用barcodeScanner.setScanAreaRectInPreview(Rect)方法设置扫描区域

###2.开始解码
在需要启动解码的时候调用barcodeScanner.start(Camera)方法即可启动解码，但是要注意在调用此方法之前Camera必须已经启动预览
```java
barcodeScanner.start(camera);
```


###3.处理解码结果以及可疑点登回调事件
```java
private class MyBarcodeScanListener implements BarcodeScanListener{
	@Override
	public void onStartScan() {
		//启动扫描
	}

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

	@Override
	public void onStopScan() {
		//停止扫描
	}

	@Override
	public void onRelease() {
		//释放
	}
}
```


###4.停止解码
在需要停止解码的时候调用barcodeScanner.stop()即可停止解码
```java
barcodeScanner.stop();
```


###5.释放BarcodeScanner
在需要释放的时候调用barcodeScanner.release()方法即可释放，一般情况下建议重写Activity的onDestroy()方法，在onDestroy()方法内部释放BarcodeScanner，但是当已经释放了之后再去调用start()方法则会抛出IllegalStateException异常
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
>* BarcodeScanner.java新增setWhetherRotatePreview()方法，可设置在解码之前将预览图旋转90度
>* BarcodeScanner.java新增setScanAreaRotateMode()方法，可设置当扫描区的宽度小于高度时在解码之前将预览图旋转90度

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
