package org.sharpai.lib.pim.image;

import android.graphics.Rect;
import android.util.Log;

/**
 * @author Marco Dinacci <marco.dinacci@gmail.com>
 */
public class AndroidImage_NV21 extends AbstractAndroidImage {

	private static final String TAG = "AndroidImage_NV21";
	private int mLastPixelDiff = 0;
	private Rect mLastRect = null;

	public AndroidImage_NV21(byte[] data, Size<Integer, Integer> size) {
		super(data, size);
		mLastRect = new Rect();
		resetLastRect();
	}
	private void resetLastRect(){
		mLastRect.top = 0;
		mLastRect.bottom = 0;
		mLastRect.left = 0;
		mLastRect.right = 0;
	}

	@Override
	public boolean isDifferent(AndroidImage other, int pixel_threshold, 
			int threshold) {
		
		if(!assertImage(other)) {
			return false;
		}
		
		byte[] otherData = other.get();
		int totDifferentPixels = 0;
		
		// For the sake of this demo just use a 640x480 image.
		int height = 480;
		int width = 640;
		int size = 307200; // 640x480
		int top = 0,bottom = 0,left = 0,right = 0;
		int pix,otherPix;
		boolean gotFirst = false;
		resetLastRect();

		for (int i = 0, ij=0; i < height; i++) {
			for (int j = 0; j < width; j++,ij++) {
				pix = (0xff & ((int) mData[ij])) - 16;
				otherPix = (0xff & ((int) otherData[ij])) - 16;
				
				if (pix < 0) pix = 0;
				if (pix > 255) pix = 255;
				if (otherPix < 0) otherPix = 0;
				if (otherPix > 255) otherPix = 255;

				if(Math.abs(pix - otherPix) >= pixel_threshold){
					totDifferentPixels++;
					if(!gotFirst){
						gotFirst = true;
						top = i;
						bottom = i;
						left = width-j;
						right = width-j;
					} else {
						top = Math.min(top,i);
						bottom = Math.max(bottom,i);

						left = Math.min(left,(width-j));
						right = Math.max(right,(width-j));
					}
				}
			}
		}
		mLastRect.top = top;
		mLastRect.bottom = bottom;

		mLastRect.left = left;
		mLastRect.right = right;

		if(totDifferentPixels == 0) totDifferentPixels = 1;
		Log.d(TAG, "Number of different pixels: " + totDifferentPixels + " -> "
				+ (100 / ( size / totDifferentPixels) ) + "%");
		mLastPixelDiff = totDifferentPixels;
		return totDifferentPixels > threshold;
	}
	@Override
	public int getLastPixelDiff() {
		return mLastPixelDiff;
	}
	@Override
	public Rect getLastRect(){
		return mLastRect;
	}
	@Override
	public AndroidImage toGrayscale() {
		// TODO to implement.
		return this;
	}
}
