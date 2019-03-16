package com.edvard.poseestimation;

import android.graphics.Bitmap;

public class BackgroundJNIExtractor {
    static {
        System.loadLibrary("jupiter_opencv_320");
    }
    public native void predict(Bitmap pTarget, byte[] pSource);
}
