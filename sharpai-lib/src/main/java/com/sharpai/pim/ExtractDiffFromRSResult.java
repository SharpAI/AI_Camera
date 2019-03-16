package com.sharpai.pim;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by simba on 7/27/17.
 */

public class ExtractDiffFromRSResult {
    private static final String TAG = "ExtractDiffFromRSResult";
    private int mLastPixelDiff = 0;
    private Rect mLastRect = null;

    public ExtractDiffFromRSResult() {
        mLastRect = new Rect();
        resetLastRect();
    }
    private void resetLastRect(){
        mLastRect.top = 0;
        mLastRect.bottom = 0;
        mLastRect.left = 0;
        mLastRect.right = 0;
    }

    public boolean isDifferent(Bitmap bmp, int pixel_threshold, int threshold) {
        int totDifferentPixels = 0;
        // For the sake of this demo just use a 640x480 image.
        int height = 480;
        int width = 640;
        int size = 307200; // 640x480
        boolean gotFirst = false;
        resetLastRect();

        int bytes = bmp.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        buffer.order(ByteOrder.nativeOrder());
        bmp.copyPixelsToBuffer(buffer); //Move the byte data to the buffer

        byte[] array = buffer.array();

        int pix;
        for (int i = 0, ij=0; i < height; i++) {
            for (int j = 0; j < width; j++,ij++) {
                //int pixel = bmp.getPixel(j,i);

                pix = array[ij*4+3]+ array[ij*4+1] + array[ij*4+2];

                if (pix < 0) pix = 0;
                if (pix > 255) pix = 255;

                if(pix >= pixel_threshold*2){
                    totDifferentPixels++;

                    if(!gotFirst){
                        gotFirst = true;
                        mLastRect.top = i;
                        mLastRect.bottom = i;
                        mLastRect.left = width-j;
                        mLastRect.right = width-j;
                    } else {
                        mLastRect.top = Math.min(mLastRect.top,i);
                        mLastRect.bottom = Math.max(mLastRect.bottom,i);

                        mLastRect.left = Math.min(mLastRect.left,(width-j));
                        mLastRect.right = Math.max(mLastRect.right,(width-j));
                    }
                }
            }
        }

        if(totDifferentPixels == 0) totDifferentPixels = 1;
        Log.i(TAG, "Number of different pixels: " + totDifferentPixels + " -> "
                + (100 / ( size / totDifferentPixels) ) + "% " + mLastRect.toString());
        mLastPixelDiff = totDifferentPixels;
        return totDifferentPixels > threshold;
    }
    public int getLastPixelDiff() {
        return mLastPixelDiff;
    }
    public Rect getLastRect(){
        return mLastRect;
    }
}