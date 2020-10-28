/*
 * Copyright 2018 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sharpai.lib.detector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.sharpai.lib.detector.env.ImageUtils;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class Detector {
  // Configuration values for the prepackaged SSD model.
  private static final String TAG="detector";
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  //private static final String TF_OD_API_MODEL_FILE = "ssd_mobilenet_v1_coco_float.tflite";
  //private static final int TF_OD_API_INPUT_SIZE = 300;
  //private static final boolean TF_OD_API_IS_QUANTIZED = false;

  private static final String TF_OD_API_LABELS_FILE = "coco_labels_list.txt";

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  private static final DetectorMode MODE = DetectorMode.TF_OD_API;

  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

  private static final boolean MAINTAIN_ASPECT = false;

  private static final boolean SAVE_PREVIEW_BITMAP = false;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap croppedBitmap = null;
  //private Bitmap cropCopyBitmap = null;

  private int previewWidth=1920;
  private int previewHeight=1080;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  public Detector(Context context) {

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              context.getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      Log.e(TAG,"Exception initializing classifier!", e);
      Toast toast =
          Toast.makeText(
              context.getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      //context.finish();
    }


    Log.i(TAG,"Initializing at size "+previewWidth + " " + previewHeight);
    //rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            0, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);
  }


  public List<Classifier.Recognition> processImage(Bitmap rgbFrameBitmap) {
    ++timestamp;
    final long currTimestamp = timestamp;

    computingDetection = true;
    Log.i(TAG,"Preparing image " + currTimestamp + " for detection in bg thread.");

    Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

    Log.i(TAG,"Running detection on image " + currTimestamp);
    final long startTime = SystemClock.uptimeMillis();
    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

    Log.i(TAG,"Running detection cost " + lastProcessingTimeMs + "ms");

    //cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
    //canvas = new Canvas(cropCopyBitmap);
    //final Paint paint = new Paint();
    //paint.setColor(Color.RED);
    //paint.setStyle(Style.STROKE);
    //paint.setStrokeWidth(2.0f);

    float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
    switch (MODE) {
      case TF_OD_API:
        minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        break;
    }

    final List<Classifier.Recognition> mappedRecognitions =
        new LinkedList<Classifier.Recognition>();

    for (final Classifier.Recognition result : results) {
      final RectF location = result.getLocation();
      if (location != null && result.getConfidence() >= minimumConfidence) {
        Log.i(TAG,"Result is "+result.toString());
        if(result.getTitle().equals("person")){

          //canvas.drawRect(location, paint);

          cropToFrameTransform.mapRect(location);
          result.setLocation(location);
          Log.i(TAG,"person width: "+location.width()+" height: "+location.height());
          mappedRecognitions.add(result);
        }
      }
    }
    return mappedRecognitions;
  }
}
