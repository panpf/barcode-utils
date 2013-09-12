package me.xiaopan.barcodescanner;

import java.util.LinkedList;
import java.util.Queue;

public class SourceDataHolder {
	private Queue<byte[]> queue;
	
	public SourceDataHolder(){
		queue = new LinkedList<byte[]>();
	}
	
	private byte[] sourceData;
	
	public byte[] getSourceData() {
		return sourceData;
	}
	
	public void setSourceData(byte[] sourceData) {
		this.sourceData = sourceData;
	}
}
