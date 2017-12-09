package me.panpf.barcode;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class BarcodeCreator {

    private int outCompressQuality = 100;
    private int imageWidth;
    private int imageHeight;
    private File outFile;
    private String content;
    private BarcodeFormat barcodeFormat;
    private CompressFormat outCompressFormat = CompressFormat.JPEG;
    private Map<EncodeHintType, Object> hints;

    private MultiFormatWriter multiFormatWriter;

    public BarcodeCreator(String content, BarcodeFormat barcodeFormat, int imageWidth, int imageHeight) {
        setContent(content)
                .setBarcodeFormat(barcodeFormat)
                .setImageWidth(imageWidth)
                .setImageHeight(imageHeight);

        putEncodeHintType(EncodeHintType.CHARACTER_SET, "UTF-8");

        multiFormatWriter = new MultiFormatWriter();
    }

    /**
     * 根据 logo 和二维码图的大小比例获取合适的容错级别
     *
     * @param proportion logo 占整个二维码的比例，最小 0.07 最大 0.3
     * @return {@link ErrorCorrectionLevel}
     */
    @SuppressWarnings("WeakerAccess")
    public static ErrorCorrectionLevel getErrorCorrectionLevel(float proportion) {
        if (proportion <= 0.07) {
            return ErrorCorrectionLevel.L;
        } else if (proportion <= 0.15) {
            return ErrorCorrectionLevel.M;
        } else if (proportion <= 0.25) {
            return ErrorCorrectionLevel.Q;
        } else if (proportion <= 0.30) {
            return ErrorCorrectionLevel.H;
        } else {
            return null;
        }
    }

    /**
     * 将 BitMatrix 格式的源数据转换为Bitmap
     *
     * @param bitMatrix {@link BitMatrix}
     * @return {@link Bitmap}
     */
    @SuppressWarnings("WeakerAccess")
    public static Bitmap bitMatrixToBitmap(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 缩放处理
     *
     * @param bitmap  原图
     * @param scaling 缩放比例
     * @return 缩放后的图片
     */
    @SuppressWarnings("WeakerAccess")
    public static Bitmap scale(Bitmap bitmap, float scaling) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaling, scaling);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 缩放处理
     *
     * @param bitmap   原图
     * @param newWidth 新的宽度
     * @return {@link Bitmap}
     */
    @SuppressWarnings("WeakerAccess")
    public static Bitmap scaleByWidth(Bitmap bitmap, int newWidth) {
        return scale(bitmap, (float) newWidth / bitmap.getWidth());
    }

    /**
     * 缩放处理
     *
     * @param bitmap    原图
     * @param newHeight 新的高度
     * @return {@link Bitmap}
     */
    @SuppressWarnings("WeakerAccess")
    public static Bitmap scaleByHeight(Bitmap bitmap, int newHeight) {
        return scale(bitmap, (float) newHeight / bitmap.getHeight());
    }

    /**
     * 创建文件，此方法的重要之处在于，如果其父目录不存在会先创建其父目录
     *
     * @param file File
     * @return true：创建成功；false：创建失败
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean createFile(File file) {
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                return false;
            }

            try {
                return file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 生成条码
     *
     * @param logoBitmap 可以在条码中间显示一个logo，如果logo的大小超过了条码宽度的30%那么就会将logo缩小至条码宽度的30%
     * @return null：生成失败
     */
    public Bitmap create(Bitmap logoBitmap) {
        Bitmap qrcodeBitmap = null;
        try {
            //根据Logo的大小同输出二维码的宽高设置合适的容错率，如果logo的大小已经超出了最大容错率的限制就将logo缩小至最大容错率允许的大小
            if (logoBitmap != null) {
                int logoWidth = logoBitmap.getWidth();
                int logoHeight = logoBitmap.getHeight();
                ErrorCorrectionLevel errorCorrectionLevel;
                if (logoWidth > logoHeight) {
                    errorCorrectionLevel = getErrorCorrectionLevel((float) logoWidth / (float) imageWidth);
                    if (errorCorrectionLevel == null) {
                        errorCorrectionLevel = ErrorCorrectionLevel.H;
                        Bitmap newLogoBitmap = scaleByWidth(logoBitmap, (int) (imageWidth * 0.3f));
                        logoBitmap.recycle();
                        logoBitmap = newLogoBitmap;
                    }
                } else {
                    errorCorrectionLevel = getErrorCorrectionLevel((float) logoHeight / (float) imageHeight);
                    if (errorCorrectionLevel == null) {
                        errorCorrectionLevel = ErrorCorrectionLevel.H;
                        Bitmap newLogoBitmap = scaleByHeight(logoBitmap, (int) (imageHeight * 0.3f));
                        logoBitmap.recycle();
                        logoBitmap = newLogoBitmap;
                    }
                }
                putEncodeHintType(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            }

            //生成二维码
            qrcodeBitmap = bitMatrixToBitmap(multiFormatWriter.encode(content, barcodeFormat, imageWidth, imageHeight, hints));

            //绘制Logo
            if (logoBitmap != null) {
                Paint paint = new Paint();
                Canvas canvas = new Canvas(qrcodeBitmap);
                canvas.drawBitmap(logoBitmap, (qrcodeBitmap.getWidth() - logoBitmap.getWidth()) / 2, (qrcodeBitmap.getHeight() - logoBitmap.getHeight()) / 2, paint);
                logoBitmap.recycle();
            }

            //将二维码输出到本地
            if (outFile != null) {
                if (!outFile.exists()) {
                    File parentFile = outFile.getParentFile();
                    if (!parentFile.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        parentFile.mkdirs();
                    }
                }

                if (createFile(outFile)) {
                    qrcodeBitmap.compress(outCompressFormat, outCompressQuality, new FileOutputStream(outFile));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (qrcodeBitmap != null && !qrcodeBitmap.isRecycled()) {
                qrcodeBitmap.recycle();
                qrcodeBitmap = null;
            }
            if (logoBitmap != null && !logoBitmap.isRecycled()) {
                logoBitmap.recycle();
            }
        }
        return qrcodeBitmap;
    }

    /**
     * 生成条码
     *
     * @return null：生成失败
     */
    @SuppressWarnings("unused")
    public Bitmap create() {
        return create(null);
    }

    /**
     * 设置条码图片宽度
     *
     * @param imageWidth 宽度
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("WeakerAccess")
    public BarcodeCreator setImageWidth(int imageWidth) {
        if (imageWidth <= 0) {
            throw new IllegalArgumentException(imageWidth + "（imageWidth必须大于0）");
        }
        this.imageWidth = imageWidth;
        return this;
    }

    /**
     * 设置条码图片高度
     *
     * @param imageHeight 高度
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public BarcodeCreator setImageHeight(int imageHeight) {
        if (imageHeight <= 0) {
            throw new IllegalArgumentException(imageHeight + "（imageHeight必须大于0）");
        }
        this.imageHeight = imageHeight;
        return this;
    }

    /**
     * 设置条码内容
     *
     * @param content 条码内容
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("WeakerAccess")
    public BarcodeCreator setContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content不能为null");
        }
        this.content = content;
        return this;
    }

    /**
     * 设置条码格式
     *
     * @param barcodeFormat BarcodeFormat
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("WeakerAccess")
    public BarcodeCreator setBarcodeFormat(BarcodeFormat barcodeFormat) {
        if (barcodeFormat == null) {
            throw new IllegalArgumentException("barcodeFormat不能为null");
        }
        this.barcodeFormat = barcodeFormat;
        return this;
    }

    /**
     * 设置编码方式
     *
     * @param charset 编码方式
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("unused")
    public BarcodeCreator setCharset(String charset) {
        if (TextUtils.isEmpty(charset)) {
            throw new IllegalArgumentException("charset is null or empty");
        }
        putEncodeHintType(EncodeHintType.CHARACTER_SET, charset);
        return this;
    }

    /**
     * 批量设置其它属性
     *
     * @param hints 编码属性
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("unused")
    public BarcodeCreator setHints(Map<EncodeHintType, Object> hints) {
        if (hints == null) {
            throw new IllegalArgumentException("hints is null");
        }
        this.hints = hints;
        return this;
    }

    /**
     * 增加其它属性
     *
     * @param key   {@link EncodeHintType}. 编码属性 key
     * @param value Object. 编码属性 value
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public BarcodeCreator putEncodeHintType(EncodeHintType key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key不能为null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value不能为null");
        }
        if (hints == null) {
            hints = new EnumMap<>(EncodeHintType.class);
        }
        hints.put(key, value);
        return this;
    }

    /**
     * 设置输出文件
     *
     * @param outFile {@link File}
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("UnusedReturnValue")
    public BarcodeCreator setOutFile(File outFile) {
        if (outFile == null) {
            throw new IllegalArgumentException("outFile不能为null");
        }
        this.outFile = outFile;
        return this;
    }

    /**
     * 设置输出压缩比例
     *
     * @param outCompressQuality 0-100
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("unused")
    public BarcodeCreator setOutCompressQuality(int outCompressQuality) {
        if (outCompressQuality <= 0) {
            throw new IllegalArgumentException(outCompressQuality + "（outCompressQuality必须大于0）");
        }
        this.outCompressQuality = outCompressQuality % 100;
        return this;
    }

    /**
     * 设置输出压缩格式
     *
     * @param outCompressFormat {@link CompressFormat}. 输出压缩格式
     * @return {@link BarcodeCreator}
     */
    @SuppressWarnings("unused")
    public BarcodeCreator setOutCompressFormat(CompressFormat outCompressFormat) {
        if (outCompressFormat == null) {
            throw new IllegalArgumentException("outCompressFormat不能为null");
        }
        this.outCompressFormat = outCompressFormat;
        return this;
    }
}
