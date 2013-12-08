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

package me.xiaopan.easy.barcode;

import android.graphics.Bitmap;
import android.graphics.Matrix;

class RequiredUtils {
	
	/**
	 * 将YUV格式的图片的源数据从横屏模式转为竖屏模式，注意：将源图片的宽高互换一下就是新图片的宽高
	 * @param sourceData YUV格式的图片的源数据
	 * @param width 源图片的宽
	 * @param height 源图片的高
	 * @return 
	 */
	public static final byte[] yuvLandscapeToPortrait(byte[] sourceData, int width, int height){
		byte[] rotatedData = new byte[sourceData.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = sourceData[x + y * width];
		}
		return rotatedData;
	}
	
	/**
	 * 缩放处理
	 * @param bitmap 原图
	 * @param scaling 缩放比例
	 * @return 缩放后的图片
	 */
	public static Bitmap scale(Bitmap bitmap, float scaling) {
		Matrix matrix = new Matrix();
		matrix.postScale(scaling, scaling);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}
	
	/**
	 * 缩放处理
	 * @param bitmap 原图
	 * @param newWidth 新的宽度
	 * @return
	 */
	public static Bitmap scaleByWidth(Bitmap bitmap, int newWidth) {
		return scale(bitmap, (float) newWidth / bitmap.getWidth());
	}
	
	/**
	 * 缩放处理
	 * @param bitmap 原图
	 * @param newHeight 新的高度
	 * @return
	 */
	public static Bitmap scaleByHeight(Bitmap bitmap, int newHeight) {
		return scale(bitmap, (float) newHeight / bitmap.getHeight());
	}
}
