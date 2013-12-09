# ![Logo](https://github.com/ixiaopan/EasyBarcode/raw/master/res/drawable-mdpi/ic_launcher.png) EasyBarcode

这是一个基于Zxing基础上封装的Android条码扫描库，适合快速在项目中集成扫码功能。


##Usage Guide
###1.创建BarcodeDecoder
在初始化Camera的时候创建BarcodeDecoder
```java
/* 初始化解码器 */
if(barcodeDecoder == null){
	Size previewSize = camera.getParameters().getPreviewSize();
	Rect scanAreaInPreviewRect = Utils.mappingRect(new Point(surfaceView.getWidth(), surfaceView.getHeight()), ViewUtils.getRelativeRect(scanAreaView, surfaceView), new Point(previewSize.width, previewSize.height), getBaseContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
	barcodeDecoder = new BarcodeDecoder(getBaseContext(), previewSize,  scanAreaInPreviewRect, null, this);
	barcodeDecoder.setDebugMode(true);
}
barcodeDecoder.setCamera(camera);
```
###2.处理解码结果以及可疑点
```java
private class MyDecodeListener implements DecodeListener{
	@Override
	public void foundPossibleResultPoint(ResultPoint resultPoint) {
//		你可以在这里将可疑点绘制到你的界面上
//		具体如何绘制你可以参考Zxing的ViewfinderView.java的addPossibleResultPoint()方法或者参考本库中的ScanAreaView.java的addPossibleResultPoint()方法
//		你还可以直接使用本库自带的ScanAeaView.java来作为扫描区，具体使用方式你可以参考本项目中的DecodeActivity.java
	}

	@Override
	public void onDecodeSuccess(final Result result, final byte[] barcodeBitmapByteArray, final float scaleFactor) {
		Toast.makeText(getBaseContext(), "条码内容："+result.getText(), Toast.LENGTH_LONG).show();
//		如果你想在识别到条码后暂停识别就在此调用以代码
//		barcodeDecoder.pause();
//		当你暂停了之后需要再次识别就调用以下代码
//		barcodeDecoder.resume();
	}

	@Override
	public void onDecodeFailure() {
	}
}
```

###3.暂停或恢复BarcodeDecoder
你需要在Camera停止预览的时候执行barcodeDecoder.pause()暂停解码，在Camera启动预览的时候执行barcodeDecoder.resume()恢复解码

###4.释放BarcodeDecoder
重写Activity的onDestory()方法，在方法内部释放BarcodeDecoder
```java
@Override
protected void onDestroy() {
	if(barcodeDecoder != null){
		barcodeDecoder.release();
		barcodeDecoder = null;
	}
	super.onDestroy();
}
```
###5.完整使用请参考DecodeActivity.java

##Change Log
###1.0.7
>* Decoder.java改名为BarcodeDecoder.java；
>* 优化了解码线程的实现方式使之更加稳定、节省内存，解码性能大幅提升；
>* 将ResultPointCallback的回调移到了DecodeListener中，使用更方便；
>* EasyBarcode不再依赖于EasyAndroid核心库。

**[easy-barcode-1.0.7.jar](https://github.com/ixiaopan/EasyBarcode/raw/master/downloads/easy-barcode-1.0.7.jar)**

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
