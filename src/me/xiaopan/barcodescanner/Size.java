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
package me.xiaopan.barcodescanner;

/**
 * 尺寸
 */
public class Size {
	private int width;	//宽
	private int height;	//高
	
	public Size() {}
	
	public Size(int width, int height){
		setWidth(width);
		setHeight(height);
	}
	
	/**
	 * 获取宽
	 * @return 宽
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * 设置宽 
	 * @param width 宽
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * 获取高
	 * @return 高
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * 设置高
	 * @param width 高
	 */
	public void setHeight(int height) {
		this.height = height;
	}
}
