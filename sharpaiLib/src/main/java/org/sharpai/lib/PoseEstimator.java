/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.sharpai.lib;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;

/**
 * Classifies images with Tensorflow Lite.
 */
public abstract class PoseEstimator {
  // Display preferences
  /** 30% 이상은 강조글로 표현 */
  //private static final float GOOD_PROB_THRESHOLD = 0.3f;
  private static final int SMALL_COLOR = 0xffddaa88;

  Mat mMat = null;
  /** output shape (heatmap shape)*/

  //model_h
  private static final int HEATMAPWIGHT = 48;
  private static final int HEATMAPHEIGHT = 48;
  private static final int NUMJOINT = 14;
  // model_cpm

  /*
  private static final int HEATMAPWIGHT = 96;
  private static final int HEATMAPHEIGHT = 96;
  private static final int NUMJOINT = 14;
  */

  //multi_person_mobilenet_v1_075_float
  /*
  private static final int HEATMAPWIGHT = 23;
  private static final int HEATMAPHEIGHT = 17;
  private static final int NUMJOINT = 17;
  */
  /** Tag for the {@link Log}. */
  private static final String TAG = "TfLiteCameraDemo";

  /** Number of results to show in the UI.
   * 몇개 결과창으로 보여 줄지 */
  //private static final int RESULTS_TO_SHOW = 3;

  /** Dimensions of inputs. */
  private static final int DIM_BATCH_SIZE = 1;

  /** 채널 사이즈 입력 값*/
  private static final int DIM_PIXEL_SIZE = 3;

  /** Preallocated buffers for storing image data in.
   * 이미지 데이터를 저장하기 위해 사전 할당 된 버퍼 */
  private int[] intValues = new int[getImageSizeX() * getImageSizeY()];

  /** Options for configuring the Interpreter.
   * 인터프리터 구성 옵션 */
  private final Interpreter.Options tfliteOptions = new Interpreter.Options();

  /** The loaded TensorFlow Lite model.
   * MappedByteBuffer 파일 자체를 메모리에 적제하여 사용하는 방법의 일종*/
  private MappedByteBuffer tfliteModel;

  /** An instance of the driver class to run model inference with Tensorflow Lite.
   * Tensorflow Lite로 모델 추론을 실행하는 드라이버 클래스의 인스턴스 */
  protected Interpreter tflite;

  /** Labels corresponding to the output of the vision model.
   * 결과물의 레이블의 종류가 담기는 곳 */
  //private List<String> labelList;


  /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
   * Tensorflow Lite에 입력으로 제공될 이미지 데이터를 보유하는 ByteBuffer입니다. */
  protected ByteBuffer imgData = null;

  /** multi-stage low pass filter *
   * ?? 결과값에 대한 정규화? */
  /*
  private float[][] filterLabelProbArray = null;

  private static final int FILTER_STAGES = 3;
  private static final float FILTER_FACTOR = 0.4f;

  private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
      new PriorityQueue<>(
          RESULTS_TO_SHOW,
          new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
              return (o1.getValue()).compareTo(o2.getValue());
            }
          });
  */
  /** holds a gpu delegate */
  Delegate gpuDelegate = null;

  public float[][] mPrintPointArray = null;
  public List<Float> pointList;
  String []mIndexToPartName = {"top","neck",
      "r_shoulder","r_elbow","l_shoulder","l_elbow","l_wrist","r_hip","r_knee",
      "r_ankle","l_hip","l_knee","l_ankle"};

  /** Initializes an {@code PoseEstimator}. */
  PoseEstimator(Context activity) throws IOException {
    /** tfliteModel => MappedByteBuffer */
    tfliteModel = loadModelFile(activity);
    /** tflite => Interpreter */
    tflite = new Interpreter(tfliteModel, tfliteOptions);
    //labelList = loadLabelList(activity);
    imgData =
        ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                * getImageSizeX()
                * getImageSizeY()
                * DIM_PIXEL_SIZE
                * getNumBytesPerChannel());

    //메모리에 적게되는 방식 정리
    // TODO: 자세히 알아볼 필요 있음
    imgData.order(ByteOrder.nativeOrder());

    /** 결과값에ㅐ low pass filter를 적용하기 위한 변수 */
    //filterLabelProbArray = new float[FILTER_STAGES][getNumLabels()];
    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
  }

  /** Classifies a frame from the preview stream. */
  //TODO: 어디서 활용되는지 확인 필요
  void classifyFrame(Bitmap bitmap, SpannableStringBuilder builder) {
    if (tflite == null) {
      Log.e(TAG, "Image classifier has not been initialized; Skipped.");
      builder.append(new SpannableString("Uninitialized Classifier."));
    }

    convertBitmapToByteBuffer(bitmap);

    // Here's where the magic happens!!!
    long startTime = SystemClock.uptimeMillis();
    runInference();
    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

    drawBodyPoint();
//    Log.d(TAG,"hhhhhhhhh------"+pointList.toString());

    // Smooth the results across frames.
    //applyFilter();

    // Print the results.
    /*
    printTopKLabels(builder);
    long duration = endTime - startTime;
    SpannableString span = new SpannableString(duration + " ms");
    span.setSpan(new ForegroundColorSpan(android.graphics.Color.LTGRAY), 0, span.length(), 0);
    builder.append(span);
    */
  }
    private float get(int x, int y, float[] arr) {
        if (x < 0 || y < 0 || x >= getHeatmapWidth() || y >= getHeatmapHeight())
            return -1;
        return arr[x * getHeatmapWidth() + y];
    }
  void drawBodyPoint() {
    int index = 0;
    pointList = new ArrayList<>();
    //float[] tempArray = new float[getHeatmapHeight() * getHeatmapWidth()];
    //float[] outTempArray = new float[getHeatmapHeight() * getHeatmapWidth()];
    //if (mMat == null){
    //  mMat = new Mat(getHeatmapWidth(), getHeatmapHeight(), CvType.CV_32F);
    //}
    //if (mPrintPointArray == null){
    //  mPrintPointArray = new float[2][14];
    //}

    for (int k = 0; k < getNumJoint(); k++) {
      //int counter = 0;
      float[][] heatmap = new float[getHeatmapWidth()][getHeatmapHeight()];
      for (int i = 0; i < getHeatmapWidth(); i++){
        for (int j = 0; j < getHeatmapHeight(); j++){
          heatmap[i][j] = getProbability(index,i,j,k);
          //tempArray[counter] = heatmap[i][j];
          //counter ++;
          //prob = getProbability(index,i,j,k);
        }
      }
/*
      mMat.put(0, 0, tempArray);
      Imgproc.GaussianBlur(mMat, mMat, new Size(5, 5), 0, 0);
      mMat.get(0, 0, outTempArray);

        float maxX = 0, maxY = 0;
        float max = 0;
        for (int x = 0; x < getHeatmapWidth(); x++) {
            for (int y = 0; y < getHeatmapHeight(); y++) {
                float center = get(x, y, outTempArray);

                if (center >= 0.01) {

                    if (center > max) {
                        max = center;
                        maxX = x;
                        maxY = y;
                    }
                }
            }
        }

        if (max == 0) {
            mPrintPointArray = new float[2][14];

            mPrintPointArray[0][k] = 0;
            mPrintPointArray[1][k] = 0;
            Log.d("yangace","max is 0");
            //return;
        }
*/
      //PrintPointArray[0][k] = maxY;
      //mPrintPointArray[1][k] = maxX;

      float[] result = new float[3];
      result = findMaximumIndex(heatmap);

      if(result[2]>0){
          Log.d("yangace", "index["+k+"] = " + " "+ result[0] + " "+ result[1] + " " + mIndexToPartName[k] + " " + result[2]);
          pointList.add(result[0]);
          pointList.add(result[1]);
          pointList.add(result[2]);
      }

      //Log.d("yangace", "index["+k+"] = " + " "+ maxX+ " "+ maxY + " ");
    }

  }

  public static float[] findMaximumIndex(float[ ][ ] a)
  {
    float maxVal = 0.1f;
    float[] answerArray = new float[3];
    for(int row = 0; row < a.length; row++)
    {
      for(int col = 0; col < a[row].length; col++)
      {
        if(a[row][col] > maxVal)
        {
          maxVal = a[row][col];
          answerArray[0] = row;
          answerArray[1] = col;
          answerArray[2] = a[row][col];
        }
      }
    }
    return answerArray;
  }
  /*
  void applyFilter() {
    int numLabels = getNumLabels();

    // Low pass filter `labelProbArray` into the first stage of the filter.
    for (int j = 0; j < numLabels; ++j) {
    for (int j = 0; j < numLabesetProbabilityls; ++j) {
      filterLabelProbArray[0][j] +=
          FILTER_FACTOR * (getProbability(j) - filterLabelProbArray[0][j]);
    }
    // Low pass filter each stage into the next.
    for (int i = 1; i < FILTER_STAGES; ++i) {
      for (int j = 0; j < numLabels; ++j) {
        filterLabelProbArray[i][j] +=
            FILTER_FACTOR * (filterLabelProbArray[i - 1][j] - filterLabelProbArray[i][j]);
      }
    }

    // Copy the last stage filter output back to `labelProbArray`.
    for (int j = 0; j < numLabels; ++j) {
      setProbability(j, filterLabelProbArray[FILTER_STAGES - 1][j]);
    }
  }
  */

  private void recreateInterpreter() {
    if (tflite != null) {
      tflite.close();
      // TODO(b/120679982)
      // gpuDelegate.close();
      tflite = new Interpreter(tfliteModel, tfliteOptions);
    }
  }

  public void useGpu() {
    if (gpuDelegate == null && GpuDelegateHelper.isGpuDelegateAvailable()) {
      gpuDelegate = GpuDelegateHelper.createGpuDelegate();
      tfliteOptions.addDelegate(gpuDelegate);
      recreateInterpreter();
    }
  }

  public void useCPU() {
    tfliteOptions.setUseNNAPI(false);
    recreateInterpreter();
  }

  public void useNNAPI() {
    tfliteOptions.setUseNNAPI(true);
    recreateInterpreter();
  }

  public void setNumThreads(int numThreads) {
    tfliteOptions.setNumThreads(numThreads);
    recreateInterpreter();
  }

  /** Closes tflite to release resources. */
  public void close() {
    tflite.close();
    tflite = null;
    tfliteModel = null;
  }

  /** Reads label list from Assets. */
  /*
  private List<String> loadLabelList(Activity activity) throws IOException {
    List<String> labelList = new ArrayList<String>();
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(activity.getAssets().open(getLabelPath())));
    String line;
    while ((line = reader.readLine()) != null) {
      labelList.add(line);
    }
    reader.close();
    return labelList;
  }
  */
  //TODO: 세부적으로 볼 필요 있음
  /** Memory-map the model file in Assets. */
  private MappedByteBuffer loadModelFile(Context activity) throws IOException {
    AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  /** Writes Image data into a {@code ByteBuffer}. */
  private void convertBitmapToByteBuffer(Bitmap bitmap) {
    if (imgData == null) {
      return;
    }
    imgData.rewind();
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    // Convert the image to floating point.
    int pixel = 0;
    long startTime = SystemClock.uptimeMillis();
    for (int i = 0; i < getImageSizeX(); ++i) {
      for (int j = 0; j < getImageSizeY(); ++j) {
        final int val = intValues[pixel++];
        addPixelValue(val);
      }
    }
    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
}

  /** Prints top-K labels, to be shown in UI as the results. */
  /*
  private void printTopKLabels(SpannableStringBuilder builder) {
    for (int i = 0; i < getNumLabels(); ++i) {
      sortedLabels.add(
          new AbstractMap.SimpleEntry<>(labelList.get(i), getNormalizedProbability(i)));
      if (sortedLabels.size() > RESULTS_TO_SHOW) {
        sortedLabels.poll();
      }
    }

    final int size = sortedLabels.size();
    for (int i = 0; i < size; i++) {
      Map.Entry<String, Float> label = sortedLabels.poll();
      SpannableString span =
          new SpannableString(String.format("%s: %4.2f\n", label.getKey(), label.getValue()));
      int color;
      // Make it white when probability larger than threshold.
      if (label.getValue() > GOOD_PROB_THRESHOLD) {
        color = android.graphics.Color.WHITE;
      } else {
        color = SMALL_COLOR;
      }
      // Make first item bigger.
      if (i == size - 1) {
        float sizeScale = (i == size - 1) ? 1.25f : 0.8f;
        span.setSpan(new RelativeSizeSpan(sizeScale), 0, span.length(), 0);
      }
      span.setSpan(new ForegroundColorSpan(color), 0, span.length(), 0);
      builder.insert(0, span);
    }
  }
  */
  /**
   * Get the name of the model file stored in Assets.
   * 상속받는 대상에 추상화 클래스 모델 파일 이름과 경로가 리턴됨
   * @return
   */
  protected abstract String getModelPath();

  /**
   * Get the name of the label file stored in Assets.
   *
   * @return
   */
  //protected abstract String getLabelPath();

  /**
   * Get the image size along the x axis.
   *
   * @return
   */
  protected abstract int getImageSizeX();

  /**
   * Get the image size along the y axis.
   *
   * @return
   */
  protected abstract int getImageSizeY();

  /**
   * Get the number of bytes that is used to store a single color channel value.
   *
   * @return
   */
  protected abstract int getNumBytesPerChannel();

  /**
   * Add pixelValue to byteBuffer.
   *
   * @param pixelValue
   */
  protected abstract void addPixelValue(int pixelValue);

  /**
   * Read the probability value for the specified label This is either the original value as it was
   * read from the net's output or the updated value after the filter was applied.
   *
   * @param index
   * @param width
   * @param height
   * @param joint
   * @return
   */
  protected abstract float getProbability(int index, int width, int height, int joint);

  /**
   * Set the probability value for the specified label.
   *
   * @param labelIndex
   * @param labelIndex
   * @param value
   */
  //protected abstract void setProbability(int labelIndex, Number value);

  /**
   * Get the normalized probability value for the specified label. This is the final value as it
   * will be shown to the user.
   *
   * @return
   */
  //protected abstract float getNormalizedProbability(int labelIndex);

  /**
   * Run inference using the prepared input in {@link #imgData}. Afterwards, the result will be
   * provided by getProbability().
   *
   * <p>This additional method is necessary, because we don't have a common base for different
   * primitive data types.
   */
  protected abstract void runInference();

  /**
   * Get the total number of labels.
   *
   * @return
   */
  /*
  protected int getNumLabels() {
    return labelList.size();
  }
  */

  /**
   * Get the shape of output(heatmap) .
   *
   * @return
   */
  protected int getHeatmapWidth() {
    return HEATMAPWIGHT;
  }

  protected int getHeatmapHeight() {
    return HEATMAPHEIGHT;
  }

  protected int getNumJoint() {
    return NUMJOINT;
  }
}
