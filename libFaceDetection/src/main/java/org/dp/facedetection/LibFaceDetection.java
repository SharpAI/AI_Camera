package org.dp.facedetection;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.MatOfRect;

public class LibFaceDetection {
    private final static String TAG = "LibFaceDetection";
    public LibFaceDetection(){}
    public Face[] Detect(Bitmap bmp) {

        MatOfRect rgba = new MatOfRect();
        Utils.bitmapToMat(bmp, rgba);

        Face[] facesArray = facedetect(rgba.getNativeObjAddr());

        return facesArray;
    }
    static {
        System.loadLibrary("facedetection");
    }
    public native Face[] facedetect(long matAddr);
}
