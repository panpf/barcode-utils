package me.xiaopan.barcodescanner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.hardware.Camera.Size;
import android.util.Log;

/**
 * Camera最佳尺寸计算器
 */
public class CameraOptimalSizeCalculator {
	private static final String LOG_TAG = "CameraUtils";
	private boolean proportionPriority = true;
	private float previewSizeMinWidthProportion = 0.7f;
	private float pictureSizeMinWidthProportion = 0.7f;
	
	@SuppressWarnings("unchecked")
	public Size getPreviewSize(int surfaceViewWidth, int surfaceViewHeight, List<Size> supportPreviewSizes){
		/* 现在要计算最佳比例 */
		if(surfaceViewWidth < surfaceViewHeight){	//如果宽度小于高度就将宽高互换
			surfaceViewWidth = surfaceViewWidth + surfaceViewHeight;
			surfaceViewHeight = surfaceViewWidth - surfaceViewHeight;
			surfaceViewWidth = surfaceViewWidth - surfaceViewHeight;
		}
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		float optimalProportion = Float.valueOf(decimalFormat.format((float) surfaceViewWidth / (float) surfaceViewHeight));
		Log.e(LOG_TAG, "最佳尺寸="+optimalProportion+", "+surfaceViewWidth+","+surfaceViewHeight);
		
		/* 删除那些小于最小宽度比例的 */
		removeSmall(supportPreviewSizes, (int) (surfaceViewWidth * previewSizeMinWidthProportion));
		
		/* 首先获取预览尺寸集合和输出图片尺寸集合 */
		List<CameraSize> previewCameraSizes = new ArrayList<CameraSize>(supportPreviewSizes.size());
		for(Size size2 : supportPreviewSizes){
			previewCameraSizes.add(new CameraSize(size2));
		}
		log(previewCameraSizes, "初始状态：预览");
		
		/* 其次排序 */
		sortByProportion(previewCameraSizes);
		log(previewCameraSizes, "排序：预览");
		
		/* 接下来按比例分组 */
		Map<Float, List<Size>> previewSizeMap = groupingByProportion(previewCameraSizes);
		previewCameraSizes.clear();
		previewCameraSizes = null;
		logGrouping(previewSizeMap, "按比例分组：预览");
		
		/* 按最接近最佳比例排序 */
		List<Entry<Float, List<Size>>> previewSizeEntrys = new ArrayList<Entry<Float,List<Size>>>(previewSizeMap.entrySet());
		previewSizeMap.clear();
		previewSizeMap = null;
		sortByProportionForEntry(optimalProportion, previewSizeEntrys);
		logCameraSizeEntry(previewSizeEntrys, "按最接近最佳比例排序分组：预览");
		
		/* 按最佳比例一个一个找 */
		Size optimalSize = null;
		List<Size> previewSizes = null;
		for(int w = 0; w < previewSizeEntrys.size(); w++){
			previewSizes = previewSizeEntrys.get(w).getValue();
			optimalSize = findSame(previewSizes, surfaceViewWidth, surfaceViewHeight);
			if(optimalSize != null || proportionPriority){
				break;
			}
		}
		if(optimalSize == null){	//如果还是没有找到就从最佳比例的尺寸组中取宽度最接近的
			optimalSize = lookingWidthProximal(previewSizeEntrys.get(0).getValue(), surfaceViewWidth);
		}
		if(optimalSize != null){
			Log.e(LOG_TAG, "结果：预览="+optimalSize.width+","+optimalSize.height);
		}else{
			Log.e(LOG_TAG, "结果：没有找到合适的尺寸");
		}
		
		return optimalSize;
	}
	
	/**
	 * 获取最佳的预览和输出图片尺寸
	 * @param surfaceView
	 * @param cameraParameters
	 */
	@SuppressWarnings("unchecked")
	public Size[] getPreviewAndPictureSize(int surfaceViewWidth, int surfaceViewHeight, List<Size> supportPreviewSizes, List<Size> supportPictureSizes){
		/* 现在要计算最佳比例 */
		if(surfaceViewWidth < surfaceViewHeight){	//如果宽度小于高度就将宽高互换
			surfaceViewWidth = surfaceViewWidth + surfaceViewHeight;
			surfaceViewHeight = surfaceViewWidth - surfaceViewHeight;
			surfaceViewWidth = surfaceViewWidth - surfaceViewHeight;
		}
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		float optimalProportion = Float.valueOf(decimalFormat.format((float) surfaceViewWidth / (float) surfaceViewHeight));
		Log.e(LOG_TAG, "最佳尺寸="+optimalProportion+", "+surfaceViewWidth+","+surfaceViewHeight);
		
		/* 删除那些小于最小宽度比例的 */
		removeSmall(supportPreviewSizes, (int) (surfaceViewWidth * previewSizeMinWidthProportion));
		removeSmall(supportPictureSizes, (int) (surfaceViewWidth * pictureSizeMinWidthProportion));

		/* 首先获取预览尺寸集合和输出图片尺寸集合 */
		List<CameraSize> previewCameraSizes = new ArrayList<CameraSize>(supportPreviewSizes.size());
		for(Size size2 : supportPreviewSizes){
			previewCameraSizes.add(new CameraSize(size2));
		}
		List<CameraSize> pictureCameraSizes = new ArrayList<CameraSize>(supportPictureSizes.size());
		for(Size size : supportPictureSizes){
			pictureCameraSizes.add(new CameraSize(size));
		}
		log(previewCameraSizes, "初始状态：预览");
		log(pictureCameraSizes, "初始状态：输出");
		
		/* 然后去除不相同的 */
		removalOfDifferent(previewCameraSizes, pictureCameraSizes);
		log(previewCameraSizes, "删除孤独的：预览");
		log(pictureCameraSizes, "删除孤独的：输出");
		
		/* 接下来按比例分组 */
		Map<Float, List<Size>> previewSizeMap = groupingByProportion(previewCameraSizes);
		previewCameraSizes.clear();
		previewCameraSizes = null;
		logGrouping(previewSizeMap, "按比例分组：预览");
		Map<Float, List<Size>> pictureSizeMap = groupingByProportion(pictureCameraSizes);
		pictureCameraSizes.clear();
		pictureCameraSizes = null;
		logGrouping(pictureSizeMap, "按比例分组：输出");
		
		/* 按最接近最佳比例排序 */
		List<Entry<Float, List<Size>>> previewSizeEntrys = new ArrayList<Entry<Float,List<Size>>>(previewSizeMap.entrySet());
		previewSizeMap.clear();
		previewSizeMap = null;
		List<Entry<Float, List<Size>>> pictureSizeEntrys = new ArrayList<Entry<Float,List<Size>>>(pictureSizeMap.entrySet());
		pictureSizeMap.clear();
		pictureSizeMap = null;
		sortByProportionForEntry(optimalProportion, previewSizeEntrys, pictureSizeEntrys);
		logCameraSizeEntry(previewSizeEntrys, "按最接近最佳比例排序分组：预览");
		logCameraSizeEntry(pictureSizeEntrys, "按最接近最佳比例排序分组：输出");
		
		/* 按最佳比例一个一个找 */
		Size[] optimalSizes = null;
		List<Size> previewSizes = null;
		List<Size> pictureSizes = null;
		for(int w = 0; w < previewSizeEntrys.size(); w++){
			previewSizes = previewSizeEntrys.get(w).getValue();
			pictureSizes = pictureSizeEntrys.get(w).getValue();
			optimalSizes = tryLookingSame(previewSizes, pictureSizes, surfaceViewWidth);	//尝试寻找相同尺寸的一组
			if(optimalSizes != null || proportionPriority){
				break;
			}
		}
		if(optimalSizes == null){//如果还是没有找到就从最佳比例的尺寸组中取宽度最接近的一组
			optimalSizes = tryLookingWidthProximal(previewSizeEntrys.get(0).getValue(), pictureSizeEntrys.get(0).getValue(), surfaceViewWidth);
		}
		logResult(optimalSizes);
		
		return optimalSizes;
	}
	
	/**
	 * 视图按宽度最接近的原则查找出最佳的尺寸
	 * @param previewSizes
	 * @param pictureSizes
	 * @param surfaceViewWidth
	 * @return
	 */
	private Size[] tryLookingWidthProximal(List<Size> previewSizes, List<Size> pictureSizes, int surfaceViewWidth){
		Size[] optimalSizes = new Size[2];
		optimalSizes[0] = lookingWidthProximal(previewSizes, surfaceViewWidth);
		optimalSizes[1] = lookingWidthProximal(pictureSizes, surfaceViewWidth);
		return optimalSizes;
	}
	
	/**
	 * 视图按相同的原则查找出最佳的尺寸
	 * @param previewSizes
	 * @param pictureSizes
	 * @param surfaceViewWidth
	 * @return
	 */
	private Size[] tryLookingSame(List<Size> previewSizes, List<Size> pictureSizes, int surfaceViewWidth){
		List<Size> sames = lookingSame(previewSizes, pictureSizes, surfaceViewWidth);	//查找出所有相同的
		logSame(sames);
		if(sames != null){	//如果存在相同的
			Size[] optimalSizes = new Size[2];
			Size optimalSize = null;
			if(sames.size() > 1){	//如果相同的还不止一个，就查找出最接近的
				optimalSize = lookingWidthProximal(sames, surfaceViewWidth);
				logSame(sames);
			}else{
				optimalSize = sames.get(0);
			}
			optimalSizes[0] = optimalSize;
			optimalSizes[1] = optimalSize;
			return optimalSizes;
		}else{
			return null;
		}
	}

	/**
	 * 查找相同的
	 * @param cameraSizes1
	 * @param cameraSizes2
	 * @return 
	 */
	private List<Size> lookingSame(List<Size> cameraSizes1, List<Size> cameraSizes2, int surfaceViewWidth){
		List<Size> sames = null;
		for(Size size : cameraSizes1){
			if(exist(size, cameraSizes2)){
				if(sames == null){
					sames = new ArrayList<Size>();
				}
				sames.add(size);
			}
		}
		return sames;
	}
	
	/**
	 * 查找宽度最接近的
	 * @param cameraSizes
	 * @param surfaceViewWidth
	 * @return
	 */
	private Size lookingWidthProximal(List<Size> cameraSizes, final int surfaceViewWidth){
		Collections.sort(cameraSizes, new Comparator<Size>() {
			@Override
			public int compare(Size lhs, Size rhs) {
				return (Math.abs(lhs.width - surfaceViewWidth)) - Math.abs((rhs.width - surfaceViewWidth));
			}
		});
		return cameraSizes.get(0);
	}
	
	/**
	 * 给实体按最接近最佳比例排序
	 * @param cameraSizeEntrys
	 * @param cameraSizeEntrys2
	 * @param optimalProportion
	 */
	private void sortByProportionForEntry(final float optimalProportion, List<Entry<Float, List<Size>>>... cameraSizeEntrysList){
		Comparator<Entry<Float, List<Size>>> comparator = new Comparator<Entry<Float, List<Size>>>() {
			@Override
			public int compare(Entry<Float, List<Size>> lhs, Entry<Float, List<Size>> rhs) {
				int result = (int) (((Math.abs((lhs.getKey() - optimalProportion)) * 100) - (Math.abs((rhs.getKey() - optimalProportion)) * 100)));
				if(result == 0){
					result = (int) ((lhs.getKey() - rhs.getKey()) * 100) * -1;
				}
				return result;
			}
		};
		for(List<Entry<Float, List<Size>>> entry : cameraSizeEntrysList){
			Collections.sort(entry, comparator);
		}
	}
	
	/**
	 * 按比例排序，最接近最佳比例的在最前面
	 * @param cameraSizes
	 * @param cameraSizes2
	 */
	private void sortByProportion(List<CameraSize>... cameraSizes){
		Comparator<CameraSize> comparator = new Comparator<CameraSize>() {
			@Override
			public int compare(CameraSize lhs, CameraSize rhs) {
				return (int) ((lhs.proportion * 100 - rhs.proportion * 100) * -1);
			}
		};
		for(List<CameraSize> cameraSize : cameraSizes){
			Collections.sort(cameraSize, comparator);
		}
	}
	
	/**
	 * 删除孤独的
	 * @param previewCameraSizes
	 * @param pictureCameraSizes
	 */
	private void removalOfDifferent(List<CameraSize> previewCameraSizes, List<CameraSize> pictureCameraSizes){
		CameraSize tempCameraSize;
		Iterator<CameraSize> iterator = previewCameraSizes.iterator();
		while(iterator.hasNext()){
			tempCameraSize = iterator.next();
			if(!exist(tempCameraSize, pictureCameraSizes)){
				iterator.remove();
			}
		}
		
		iterator = pictureCameraSizes.iterator();
		while(iterator.hasNext()){
			tempCameraSize = iterator.next();
			if(!exist(tempCameraSize, previewCameraSizes)){
				iterator.remove();
			}
		}
	}
	
	/**
	 * 按比例分组
	 * @param cameraSizes
	 * @return
	 */
	private Map<Float, List<Size>> groupingByProportion(List<CameraSize> cameraSizes){
		Map<Float, List<Size>> previewSizeMap = new HashMap<Float, List<Size>>();
		for(CameraSize cameraSize : cameraSizes){
			if(previewSizeMap.containsKey(cameraSize.proportion)){
				previewSizeMap.get(cameraSize.proportion).add(cameraSize.size);
			}else{
				List<Size> tempCameraSizes = new ArrayList<Size>();
				tempCameraSizes.add(cameraSize.size);
				previewSizeMap.put(cameraSize.proportion, tempCameraSizes);
			}
		}
		cameraSizes.clear();
		return previewSizeMap;
	}
	
	/**
	 * 打印LOG
	 * @param previewCameraSizes
	 * @param name
	 */
	private void log(List<CameraSize> previewCameraSizes, String name){
		for(CameraSize cameraSize : previewCameraSizes){
			Log.e(LOG_TAG, name+": "+cameraSize.proportion+", "+cameraSize.size.width+","+cameraSize.size.height);
		}
	}
	
	/**
	 * 打印分组
	 * @param cameraSizeMap
	 * @param name
	 */
	private void logGrouping(Map<Float, List<Size>> cameraSizeMap, String name){
		for(Entry<Float, List<Size>> entry : cameraSizeMap.entrySet()){
			String log = name+": "+entry.getKey()+"=";
			for(Size size : entry.getValue()){
				log += "["+size.width+","+size.height+"]"+",";
			}
			Log.d(LOG_TAG, log);
		}
	}
	
	private void logCameraSizeEntry(List<Entry<Float, List<Size>>> cameraSizeEntrys, String name){
		for(Entry<Float, List<Size>> entry : cameraSizeEntrys){
			String log = name+": "+entry.getKey()+"=";
			for(Size size : entry.getValue()){
				log += "["+size.width+","+size.height+"]"+",";
			}
			Log.w(LOG_TAG, log);
		}
	}
	
	private void logSame(List<Size> sames){
		if(sames != null){
			String log = "相同的=";
			for(Size size : sames){
				log += "["+size.width+","+size.height+"]"+",";
			}
			Log.d(LOG_TAG, log);
		}else{
			Log.d(LOG_TAG, "没有找到相同的");
		}
	}
	
	private void logResult(Size[] optimalSizes){
		if(optimalSizes != null){
			Log.e(LOG_TAG, "结果：预览="+optimalSizes[0].width+","+optimalSizes[0].height+"；输出="+optimalSizes[1].width+","+optimalSizes[1].height);
		}else{
			Log.e(LOG_TAG, "结果：没有找到合适的尺寸");
		}
	}

	/**
	 * 判断指定的尺寸在指定的尺寸列表中是否存在
	 * @param cameraSize
	 * @param cameraSizes
	 * @return
	 */
	private boolean exist(CameraSize cameraSize, List<CameraSize> cameraSizes){
		for(CameraSize currentCameraSize : cameraSizes){
			if(currentCameraSize.proportion == cameraSize.proportion){
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断指定的尺寸在指定的尺寸列表中是否存在
	 * @param cameraSize
	 * @param cameraSizes
	 * @return
	 */
	private boolean exist(Size cameraSize, List<Size> cameraSizes){
		for(Size currentCameraSize : cameraSizes){
			if(currentCameraSize.width == cameraSize.width && currentCameraSize.height == cameraSize.height){
				return true;
			}
		}
		return false;
	}
	
	private Size findSame(List<Size> cameraSizes, int surfaceViewWidth, int surfaceViewHeight){
		for(Size tempSize : cameraSizes){
			if(tempSize.width == surfaceViewWidth && tempSize.height == surfaceViewHeight){
				return tempSize;
			}
		}
		return null;
	}
	
	private void removeSmall(List<Size> cameraSizes, int minWidth){
		Iterator<Size> iterator = cameraSizes.iterator();
		while(iterator.hasNext()){
			Size size = iterator.next();
			if(size.width < minWidth){
				iterator.remove();
			}
		}
	}
	
	private class CameraSize{
		private Size size;
		private float proportion;
		
		public CameraSize(Size size){
			this.size = size;
			proportion = Float.valueOf(new DecimalFormat("0.00").format((float) size.width / (float) size.height));
		}
	}

	/**
	 * @return 如果为true，最佳比例集合中会先在寻找宽高完全相同的一组，如果找不到就取最接近目标宽高的一组；如果为false，会首先在所有比例集合中寻找宽高完全相同的一组，如果找不到就在最佳比例集合中取最接近目标宽高的一组
	 */
	public boolean isProportionPriority() {
		return proportionPriority;
	}

	/**
	 * @param proportionPriority 如果为true，最佳比例集合中会先在寻找宽高完全相同的一组，如果找不到就取最接近目标宽高的一组；如果为false，会首先在所有比例集合中寻找宽高完全相同的一组，如果找不到就在最佳比例集合中取最接近目标宽高的一组
	 */
	public void setProportionPriority(boolean proportionPriority) {
		this.proportionPriority = proportionPriority;
	}

	public float getPreviewSizeMinWidthProportion() {
		return previewSizeMinWidthProportion;
	}

	public void setPreviewSizeMinWidthProportion(float previewSizeMinWidthProportion) {
		this.previewSizeMinWidthProportion = previewSizeMinWidthProportion;
	}

	public float getPictureSizeMinWidthProportion() {
		return pictureSizeMinWidthProportion;
	}

	public void setPictureSizeMinWidthProportion(float pictureSizeMinWidthProportion) {
		this.pictureSizeMinWidthProportion = pictureSizeMinWidthProportion;
	}
}