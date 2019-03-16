//
// Created by simba on 2/14/19.
//

//#include "io_github_melvincabatuan_backgroundsubtraction_MainActivity.h"
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>

#include "opencv2/core.hpp"
#include <opencv2/core/utility.hpp>
#include "opencv2/imgproc.hpp"
#include "opencv2/video/background_segm.hpp"

#define  LOG_TAG    "BackgroundSubtraction"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  DEBUG 0

using namespace std;
using namespace cv;



/* Global Variables */

//Ptr<BackgroundSubtractor> bg_model = createBackgroundSubtractorKNN().dynamicCast<BackgroundSubtractor>();
Ptr<BackgroundSubtractor> bg_model = createBackgroundSubtractorMOG2().dynamicCast<BackgroundSubtractorKNN>();

Mat *pfgimg = NULL;   // foreground image pointer
Mat *pfgmask = NULL;  // foreground mask pointer




/*
 * Class:     com_edvard_poseestimation_BackgroundJNIExtractor
 * Method:    predict
 * Signature: (Landroid/graphics/Bitmap;[B)V
 */
JNIEXPORT void JNICALL Java_com_edvard_poseestimation_BackgroundJNIExtractor_predict
        (JNIEnv * pEnv, jobject pClass, jobject pTarget, jbyteArray pSource){

    AndroidBitmapInfo bitmapInfo;
    uint32_t* bitmapContent; // Links to Bitmap content

    if(AndroidBitmap_getInfo(pEnv, pTarget, &bitmapInfo) < 0) abort();
    if(bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) abort();
    if(AndroidBitmap_lockPixels(pEnv, pTarget, (void**)&bitmapContent) < 0) abort();

    /// Access source array data... OK
    //jbyte* source = (jbyte*)pEnv->GetPrimitiveArrayCritical(pSource, 0);
    //if (source == NULL) abort();

    /// cv::Mat for YUV420sp source and output BGRA
    // Mat srcGray(bitmapInfo.height, bitmapInfo.width, CV_8UC1, (unsigned char *)source);
    Mat mbgra(bitmapInfo.height, bitmapInfo.width, CV_8UC4, (unsigned char *)bitmapContent);

/***********************************************************************************************/
    /// Native Image Processing HERE...


    if(DEBUG){
        LOGI("Starting native image processing...");
    }

    /// Initialize matrices on the first run
    //if(pfgimg == NULL)
    //    pfgimg = new Mat(srcGray.size(), srcGray.type());

    //if(pfgmask == NULL)
    //    pfgmask = new Mat(srcGray.size(), srcGray.type());

    Mat fgimg = *pfgimg;
    Mat fgmask = *pfgmask;


    /// Update the background model
    // bg_model->apply(srcGray, fgmask, -1);
    bg_model->apply(bitmapContent, fgmask, -1);

    /// Smoothing
    GaussianBlur(fgmask, fgmask, Size(11, 11), 3.5, 3.5);
    threshold(fgmask, fgmask, 10, 255, THRESH_BINARY);

    /// Set foreground image to black
    fgimg = Scalar::all(0);

    /// Copy source image to foreground image using the mask
    srcGray.copyTo(fgimg, fgmask);

    /// Display foreground image in Android
    cvtColor(fgimg, mbgra, CV_GRAY2BGRA);


    if(DEBUG){
        LOGI("Successfully finished native image processing...");
    }

/************************************************************************************************/

    /// Release Java byte buffer and unlock backing bitmap
    pEnv-> ReleasePrimitiveArrayCritical(pSource,source,0);
    if (AndroidBitmap_unlockPixels(pEnv, pTarget) < 0) abort();
}