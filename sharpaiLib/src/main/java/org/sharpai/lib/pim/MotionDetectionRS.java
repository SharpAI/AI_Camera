package org.sharpai.lib.pim;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import org.sharpai.lib.pim.image.AndroidImageFactory;
import org.sharpai.lib.pim.image.Size;

import io.github.silvaren.easyrs.tools.Nv21Image;
import io.github.silvaren.easyrs.tools.Resize;
import ly.img.android.rembrandt.Rembrandt;
import ly.img.android.rembrandt.RembrandtComparisonResult;


public class MotionDetectionRS {

	private static final String TAG = "MotionDetectionRS";

	/* File storing motion detection's preferences */
	public static final String PREFS_NAME = "prefs_md";

	/* Control the threshold above which two pixels are considered different
	 * 25 means 10% pixel difference
	 * */
	private static final KeyValue<String,Integer> mPixelThreshold = new KeyValue<String,Integer>("pim.md.pixel_threshold", 20);

	/* Control the threshold above which two images are considered different
	 * 9216 = 3% of a 640x480 image
	 * */
	private static final KeyValue<String,Integer> mThreshold = new KeyValue<String,Integer>("pim.md.threshold", 9216);

	/* Control the erosion level to perform (unused) */
	private static final KeyValue<String,Integer> mErosionLevel = new KeyValue<String,Integer>("pim.md.erosion_level", 10);

	/* Percentage of pixels of the new image to be merged
	 * with the background  (unused)*/
	private static final KeyValue<String,Integer> mMorphLevel = new KeyValue<String, Integer>("pim.md.morph_level", 80);

	private static final KeyValue<String,Size<Integer,Integer>> mSize =
		new KeyValue<String,Size<Integer,Integer>>("pim.md.size", new Size<Integer,Integer>(640,480));

	/* The format of the preview frame */
	private static final KeyValue<String,Integer> mPixelFormat =
		new KeyValue<String, Integer>("pim.md.pixel_format", AndroidImageFactory.IMAGE_FORMAT_NV21);

	// this is 3%
	private static final double MOTION_THRESHOLD = 0.03;

	// Background image
	private Bitmap mBackground = null;
	private Bitmap mLastBitmap = null;

	private ExtractDiffFromRSResult mExtractor = new ExtractDiffFromRSResult();

	private double mPercentageOfDifferentPixels=0.0f;

	// The image that is used for motion detection
	private int[] mAndroidImage;

	private SharedPreferences mPrefs;

	private int mLastPixelDifference;
	private Rect mLastDiffRect;
	private Bitmap mComparisionBitmap;

	private Rembrandt mRSCompare = null;
	private RenderScript mRS = null;

	private int mOrigWidth;
	private int mOrigHeight;

	private int mToWidth;
	private int mToHeight;

	public KeyValue<String,Size<Integer,Integer>> getSize(){
		return mSize;
	}
	public KeyValue<String,Integer> getPixelFormat (){
		return mPixelFormat;
	}
	public MotionDetectionRS(SharedPreferences prefs, RenderScript rs,
                             int origWidth, int origHeight, int toWidth, int toHeight) {
		mPrefs = prefs;
		mPixelThreshold.value = mPrefs.getInt(mPixelThreshold.key, mPixelThreshold.value);
		mThreshold.value = mPrefs.getInt(mThreshold.key, mThreshold.value);
		mErosionLevel.value = mPrefs.getInt(mErosionLevel.key, mErosionLevel.value);
		mMorphLevel.value = mPrefs.getInt(mMorphLevel.key, mMorphLevel.value);
		mPixelFormat.value = mPrefs.getInt(mPixelFormat.key, mPixelFormat.value);

		mRS = rs;
		mOrigWidth = origWidth;
		mOrigHeight = origHeight;

		mToWidth = toWidth;
		mToHeight = toHeight;

		mRSCompare = new Rembrandt(mRS);
	}
	public Bitmap resizeBmp(Bitmap input, int width, int height){
		return Resize.resize(mRS,input,width,height);
	}

	public Bitmap resizeYuvToBmp(byte[] data){
		Bitmap bmp = null;
		if(mOrigHeight != mToHeight || mOrigWidth!= mToWidth){
			bmp = Resize.resizeNv21ToBmp(mRS,data,mOrigWidth,mOrigHeight,mToWidth,mToHeight);
		} else {
			bmp = Nv21Image.nv21ToBitmap(mRS, data, mToWidth, mToHeight);;
		}
		return bmp;
	}
	public Bitmap resizeYuvToBmp(byte[] data, int width, int height){
		Bitmap bmp = null;
		if(mOrigHeight != height || mOrigWidth!= width){
			bmp = Resize.resizeNv21ToBmp(mRS,data,mOrigWidth,mOrigHeight,width,height);
		} else {
			bmp = Nv21Image.nv21ToBitmap(mRS, data, width, height);;
		}
		return bmp;
	}
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
	public boolean detect(Bitmap bmp) {
		long start = System.currentTimeMillis();
		if(mOrigHeight != mToHeight || mOrigWidth!= mToWidth){
			bmp = resizeBmp(bmp,mToWidth,mToHeight);
		}

		bmp = toGrayscale(bmp);

		//Bitmap saveBmp = ;
		if(mBackground == null) {
			mBackground = bmp.copy(bmp.getConfig(),bmp.isMutable());
			bmp.recycle();
			bmp = null;
			Log.i(TAG, "Creating background image");
			return false;
		}

		boolean motionDetected = false;

		RembrandtComparisonResult result = mRSCompare.compareBitmaps(mBackground, bmp);
		//mRSCompare.compareBitmaps(mBackground, bmp);
		mPercentageOfDifferentPixels = result.getPercentageOfDifferentPixels();

		mLastPixelDifference = result.getDifferentPixels();
		mLastDiffRect = result.getLastRect();
		//mComparisionBitmap = result.getComparisionBitmap();

		if(mPercentageOfDifferentPixels > MOTION_THRESHOLD){
			motionDetected = true;
		}
		Log.i(TAG,"RS 2 "+ (System.currentTimeMillis() - start)+String.format(" %.2f%%",mPercentageOfDifferentPixels*100.0f)+" motion: "+motionDetected);

		if(!motionDetected){
			return false;
		}

		//if(mLastDiffRect.width() * mLastDiffRect.height() / (mToWidth*mToHeight) > 0.98){
		//	Log.i(TAG, "Almost all changed");
		//	mBackground = bmp.copy(bmp.getConfig(),bmp.isMutable());
		//	return false;
		//}

		mBackground = bmp.copy(bmp.getConfig(),bmp.isMutable());
		//Log.i(TAG, "Image difference  " + String.format(" %.2f",mPercentageOfDifferentPixels*100.0f) + "% Motion: " + motionDetected);

		return motionDetected;
	}
	public boolean detect(byte[] data) {
		long start = System.currentTimeMillis();
		Bitmap bmp = null;
		//Bitmap bmp = Nv21Image.nv21ToBitmap(mRS, data, orignal_width, orignal_height);
		if(mOrigHeight != mToHeight || mOrigWidth!= mToWidth){
			bmp = Resize.resizeNv21ToBmp(mRS,data,mOrigWidth,mOrigHeight,mToWidth,mToHeight);
		} else {
			bmp = Nv21Image.nv21ToBitmap(mRS, data, mToWidth, mToHeight);;
		}
		return detect(bmp);
	}
	public Bitmap getComparisionBitmap() { return mComparisionBitmap;}
	public Bitmap getLastBitmap(){ return mBackground;}
	public int getLastPixelDifference(){
		return mLastPixelDifference;
	}
	public double getPercentageOfDifferentPixels(){return mPercentageOfDifferentPixels;}
	public Rect getLastDiffRect(){
		return mLastDiffRect;
	}
}
