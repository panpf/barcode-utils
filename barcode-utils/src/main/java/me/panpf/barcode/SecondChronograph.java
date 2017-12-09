/*
 * Copyright (C) 2017 Peng fei Pan <sky@panpf.me>
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

package me.panpf.barcode;

/**
 * 秒表
 */
class SecondChronograph {
	private long firstMillis;	//第一次的时间
	private long lastFirstMillis;	//上一次的时间

	SecondChronograph(){
		firstMillis = System.currentTimeMillis();
		lastFirstMillis = firstMillis;
	}
	
	/**
	 * 计次，返回上一次计次到当前的间隔时间
	 */
	Count count(){
		long newMillis = System.currentTimeMillis();
		Count count = new Count(newMillis - firstMillis, newMillis - lastFirstMillis);
		lastFirstMillis = newMillis;
		return count;
	}
	
	/**
	 * 重置
	 */
	@SuppressWarnings("unused")
	public void reset(){
		firstMillis = System.currentTimeMillis();
		lastFirstMillis = firstMillis;
	}
	
	/**
	 * 计次
	 */
	public class Count{
		private long intervalMillis;
		private long useMillis;
		
		Count(long useMillis, long intervalMillis){
			this.useMillis = useMillis;
			this.intervalMillis = intervalMillis;
		}
		
		/**
		 * 获取距离上一次计次的间隔时间
		 */
		long getIntervalMillis() {
			return intervalMillis;
		}
		
		/**
		 * 获取当前总用时
		 */
		@SuppressWarnings("unused")
		public long getUseMillis() {
			return useMillis;
		}
	}
}