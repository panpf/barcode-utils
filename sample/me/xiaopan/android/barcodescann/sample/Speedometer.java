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

package me.xiaopan.android.barcodescann.sample;

public class Speedometer {
	private int count;
	private long lastCountTime;
	
	/**
	 * 计数
	 */
	public void count(){
		count(1);
	}
	
	/**
	 * 计数
	 * @param number 本次增加个数
	 */
	public void count(int number){
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastCountTime > 1000){
			count = 0;
			lastCountTime = currentTime;
		}
		count += number;
	}
	
	public int computePerSecondSpeed(){
		return count;
	}
}