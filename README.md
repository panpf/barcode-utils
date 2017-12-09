# ![Logo][logo_image] BarcodeUtils

![Platform][platform_image]
[![API][min_api_image]][min_api_link]
[![Release Version][release_version_image]][release_version_link]
[![ZXING Version][zxing_dep_image]][zxing_link]

BarcodeUtils 是一个条码解析、生成、扫描库，基于 zxing 封装，适合快速在项目中集成条码相关功能

依赖 zxing 核心库版本是  [![ZXING Version][zxing_dep_image]][zxing_link]

## 开始使用

### 1. 导入 BarcodeUtils

在 app 的 build.gradle 文件的 dependencies 节点中加入依赖

```groovy
dependencies {
	implementation 'me.panpf:barcode-util:$lastVersionName'
}
```

请自行替换 `$lastVersionName` 为最新的版本：[![Release Version][release_version_image]][release_version_link] `（不要v）`

### 2. 解码图片：

解码方法都集中在 [DecodeUtils] 中，包括 decodeFile()、decodeYUV()、decodeBitmap() 三种方法，如下：

```java
Result result = DecodeUtils.decodeFile(new File("/mnt/sdcard/0/qrcode.png"));
Log.i("条码内容：", result.getText());
```

### 3. 生成码：

生成主要使用的是 [BarcodeCreator]，如下：

```java
String qrcodeContent = "http://baidu.com";	// 条码内容
BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;	// 条码类型为二维码
int imageWidth = 500;	// 条码宽为 500
int imageHeight = 500;	// 条码高为 500
Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, null);
File outFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName() + File.separator + System.currentTimeMillis() + ".jpeg");

BarcodeCreator barcodeCreator = new BarcodeCreator(qrcodeContent, barcodeFormat, imageWidth, imageHeight);
barcodeCreator.setCharset("UTF-8");	// 设置编码方式
barcodeCreator.setOutFile(outFile);	// 设置将生成的条码保存到文件
barcodeCreator.setOutCompressFormat(CompressFormat.JPEG);	// 设置输出压缩格式
barcodeCreator.setOutCompressQuality(100);	// 设置输出压缩质量
Bitmap barcodeBitmap = barcodeCreator.create(logoBitmap);	// 生成条码，并且在条码中间绘制当前应用的 logo
```

### 4. 使用相机扫码：

#### 4.1. 创建 BarcodeScanner

在 onCreate() 方法中创建 [BarcodeScanner]，如下：

```java
// 扫描回调，后面再讲
MyBarcodeScanCallback scanCallback = new MyBarcodeScanCallback();

// 创建一个支持所有格式，编码方式为 UTF-8 并且默认扫描区域为全屏的条码扫描器
BarcodeScanner barcodeScanner = new BarcodeScanner(getBaseContext(), scanCallback);
```

如果你想自定义扫描格式等信息，可使用其它构造函数

#### 4.2. 设置 Camera 和扫描区域

在你打开 Camera 以后调用 barcodeScanner.setCamera() 方法设置 Camera，如下：

```java
Camera camera = Camera.open();
barcodeScanner.setCamera(camera);
```

默认的扫描区域是全屏的，如果你想自定义扫描区域的话就调用 barcodeScanner.setScanAreaRectInPreview(Rect) 方法设置扫描区域，如下：

```java
barcodeScanner.setScanAreaRectInPreview(new Rect(100, 100, 400, 250));
```

#### 4.3. 启动扫描

在执行完 camera.startPreview() 后调用 barcodeScanner.start() 启动扫描，如下：

```java
camera.startPreview();
barcodeScanner.start();
```

#### 4.4. 处理扫描回调事件

```java
private class MyBarcodeScanCallback implements BarcodeScanner.BarcodeScanCallback {
    @Override
    public void onFoundPossibleResultPoint(ResultPoint resultPoint) {
        // 你可以在这里将可疑点绘制到你的界面上
        // 具体如何绘制你可以参考 zxing 的 ViewfinderView.java 的 addPossibleResultPoint() 方法或者参考本库中的 ScanAreaView.java 的 addPossibleResultPoint() 方法
        // 你还可以直接使用本库自带的 ScanAreaView.java 来作为扫描区，具体使用方式你可以参考本项目中的 BarcodeScanActivity.java
    }

    @Override
    public boolean onDecodeCallback(final Result result, final byte[] barcodeBitmapByteArray, final float scaleFactor) {
        if (result != null) {
            // 找到条码
            return false;    //停止扫描
        } else {
            // 没有找到他条码
            return true;    //继续扫描
        }
    }
}
```

#### 4.5. 停止扫描

在需要停止解码的时候调用 barcodeScanner.stop() 即可停止扫描，如下：

```java
@Override
public void onStop(){
    super.onStop();

    barcodeScanner.stop();
}
```

#### 4.6. 释放 BarcodeScanner

在需要释放的时候调用 barcodeScanner.release() 方法即可释放，一般情况下建议重写 Activity 的 onDestroy() 方法，在 onDestroy() 方法内部释放 BarcodeScanner

注意：当已经释放了之后再去调用 start() 方法则会抛出 IllegalStateException 异常

```java
@Override
protected void onDestroy() {
    if (barcodeScanner != null) {
        barcodeScanner.release();
    }
    super.onDestroy();
}
```

完整使用示例请参考 sample 源码

## License
    Copyright (C) 2017 Peng fei Pan <sky@panpf.me>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[logo_image]: sample/src/main/res/drawable-mdpi/ic_launcher.png
[platform_image]: https://img.shields.io/badge/Platform-Android-brightgreen.svg
[min_api_image]: https://img.shields.io/badge/API-10%2B-orange.svg
[min_api_link]: https://android-arsenal.com/api?level=10
[release_version_image]: https://img.shields.io/github/release/panpf/barcode-utils.svg
[release_version_link]: https://github.com/panpf/barcode-utils/releases
[zxing_dep_image]: https://img.shields.io/badge/zxing:core-2.3.0-orange.svg
[zxing_link]: https://jcenter.bintray.com/com/google/zxing/core/
[DecodeUtils]: barcode-utils/src/main/java/me/panpf/barcode/BarcodeCreator.java
[BarcodeCreator]: barcode-utils/src/main/java/me/panpf/barcode/BarcodeCreator.java
[BarcodeScanner]: barcode-utils/src/main/java/me/panpf/barcode/BarcodeScanner.java
