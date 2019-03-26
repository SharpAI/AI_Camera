package tv.danmaku.ijk.media.example.widget.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import com.mtcnn_as.FaceDetector;
import com.sharpai.detector.Classifier;
import com.sharpai.detector.Detector;
import com.sharpai.detector.env.ImageUtils;
import com.sharpai.pim.MotionDetectionRS;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import elanic.in.rsenhancer.processing.RSImageProcessor;
import io.github.silvaren.easyrs.tools.Nv21Image;
import tv.danmaku.ijk.media.example.utils.screenshot;

import static java.lang.Math.abs;

//import static com.arthenica.mobileffmpeg.FFmpeg.RETURN_CODE_CANCEL;
//import static com.arthenica.mobileffmpeg.FFmpeg.RETURN_CODE_SUCCESS;

public class Detection {


    private static final String TAG = "Detection";
    private Context mContext;
    private RenderScript mRS = null;
    private MotionDetectionRS mMotionDetection;
    private RSImageProcessor mRSProcessor;
    private Handler mBackgroundHandler;

    private Detector mDetector = null;
    private FaceDetector mFaceDetector = null;

    private long mLastTaskSentTimestamp = 0L;
    private int mSavingCounter = 0;

    private BackgroundSubtractor mMOG2;
    private boolean mSubtractorInited = false;
    private Rect mPreviousObjectRect = null;
    private int mIntersectCount = 0;
    private int mPreviousPersonNum = 0;

    private boolean mRecording = false;

    private long mLastCleanPicsTimestamp = 0L;

    private static final int PROCESS_SAVED_IMAGE_MSG = 1002;
    private static final int PROCESS_SAVED_IMAGE_MSG_NOTNOW = 2001;

    private int DETECTION_IMAGE_WIDTH = 854;
    private int DETECTION_IMAGE_HEIGHT = 480;
    private int PREVIEW_IMAGE_WIDTH = 1920;
    private int PREVIEW_IMAGE_HEIGHT = 1080;

    private static final int FACE_SAVING_WIDTH = 112;
    private static final int FACE_SAVING_HEIGHT = 112;

    private static final int PROCESS_FRAMES_AFTER_MOTION_DETECTED = 3;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }
    public Detection(Context context){

        mContext = context;

        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        MyCallback callback = new MyCallback();
        mBackgroundHandler = new Handler(handlerThread.getLooper(), callback);

        mLastCleanPicsTimestamp = System.currentTimeMillis();

        initDetectionContext();
    }
    /**
     * Initializes the UI and initiates the creation of a motion detector.
     */
    public void initDetectionContext() {
        String devModel = Build.MODEL;
        /*if (devModel != null && devModel.equals("JDN-W09") && PREVIEW_IMAGE_HEIGHT>960) {
            PREVIEW_IMAGE_WIDTH = 1280;
            PREVIEW_IMAGE_HEIGHT = 960;
        }*/

        DETECTION_IMAGE_HEIGHT = DETECTION_IMAGE_WIDTH * PREVIEW_IMAGE_HEIGHT  / PREVIEW_IMAGE_WIDTH;
        Log.i(TAG,"DETECTION_IMAGE_HEIGHT " + DETECTION_IMAGE_HEIGHT);

        mRS = RenderScript.create(mContext);
        mMotionDetection = new MotionDetectionRS(mContext.getSharedPreferences(
                MotionDetectionRS.PREFS_NAME, Context.MODE_PRIVATE),mRS,
                PREVIEW_IMAGE_WIDTH,PREVIEW_IMAGE_HEIGHT,DETECTION_IMAGE_WIDTH,DETECTION_IMAGE_HEIGHT);
        mRSProcessor = new RSImageProcessor(mRS);
        mRSProcessor.initialize(DETECTION_IMAGE_WIDTH, DETECTION_IMAGE_HEIGHT);

        mDetector = new Detector(mContext);
        mFaceDetector = new FaceDetector(mContext);

        mMOG2 = Video.createBackgroundSubtractorKNN(5,100,false);//Video.createBackgroundSubtractorMOG2();
        //mMOG2 = Video.createBackgroundSubtractorMOG2();//Video.createBackgroundSubtractorMOG2();
        //mMOG2.setHistory(5);
        //mMOG2.setDetectShadows(false);
        //mMOG2.setComplexityReductionThreshold(0);
    }
    class MyCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            File file = null;
            URL url;
            HttpURLConnection urlConnection = null;
            switch (msg.what) {
                case PROCESS_SAVED_IMAGE_MSG:
                    Log.d(TAG, "Processing file: " + msg.obj);
                    file = new File(msg.obj.toString());
                    try {
                        url = new URL("http://127.0.0.1:" + 3000 + "/api/post?url=" + msg.obj);

                        urlConnection = (HttpURLConnection) url
                                .openConnection();

                        int responseCode = urlConnection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d(TAG, "connect success ");
                        } else {
                            file.delete();
                        }
                    } catch (Exception e) {
                        file.delete();
                        urlConnection = null;
                        //e.printStackTrace();
                        Log.v(TAG, "Detector is not running");
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private double calcIOU(Rect rect1,Rect rect2){
        android.graphics.Rect rect_1 = new android.graphics.Rect(rect1.x,
                rect1.y,
                rect1.x+rect1.width,
                rect1.y+rect1.height);

        android.graphics.Rect rect_2 = new android.graphics.Rect(rect2.x,
                rect2.y,
                rect2.x+rect2.width,
                rect2.y+rect2.height);

        Log.d(TAG,"UO Area 1 "+rect_1.toShortString()+" 2 "+rect_2.toShortString());
        rect_1.intersect(rect_2);
        return 1.0*rect_1.width()*rect_2.height()/(rect_2.width()*rect_2.height());

    }
    public boolean detectObjectChanges(Bitmap bmp){

        // Let's detect if there's big changes
        long tsMatStart = System.currentTimeMillis();

        Mat rgba = new Mat();
        Bitmap resizedBmp = mMotionDetection.resizeBmp(bmp,DETECTION_IMAGE_WIDTH,DETECTION_IMAGE_HEIGHT);
        Utils.bitmapToMat(resizedBmp, rgba);

        Mat rgb = new Mat();
        Mat fgMask = new Mat();

        //Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.GaussianBlur(rgb,rgb,new Size(21, 21), 0);

        final List<Rect> rects = new ArrayList<>();
        if(mSubtractorInited == true){
            mMOG2.apply(rgb,fgMask,0.5);

            //reference https://github.com/melvincabatuan/BackgroundSubtraction/blob/master/app/jni/ImageProcessing.cpp#L78
            //Imgproc.GaussianBlur(fgMask,fgMask,new Size(11,11), 3.5,3.5);
            //Imgproc.threshold(fgMask,fgMask,10,255,Imgproc.THRESH_BINARY);

            final List<MatOfPoint> points = new ArrayList<>();
            final Mat hierarchy = new Mat();
            Imgproc.findContours(fgMask, points, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Rect biggest = null;
            for (MatOfPoint item :points){
                //Log.d(TAG,"UO Area result "+item);

                double area = Imgproc.contourArea(item);
                if(area > 1000){
                    Rect rect = Imgproc.boundingRect(item);
                    if(biggest==null){
                        biggest = rect;
                    } else if(rect.area()>biggest.area()){
                        biggest = rect;
                    }
                    rects.add(rect);
                }
            }
            if(biggest != null){
                if(mPreviousObjectRect !=null){
                    //mPreviousObjectRect.
                    double iou = calcIOU(biggest,mPreviousObjectRect);
                    if(iou > 0.4){
                        mIntersectCount++;
                        Log.d(TAG,"UO Intersect IOU "+iou+" count "+mIntersectCount);
                        if(mIntersectCount>2){
                            if(mRecording){
                                Log.i(TAG,"There is something spotted ");
                                //FFmpeg.cancel();
                                //String result = FFmpeg.getLastCommandOutput();
                                //Log.d(TAG,"FFMPEG output is "+result);
                            }
                        }
                    } else {
                        mIntersectCount = 0;
                        mPreviousObjectRect = biggest;
                    }
                } else {
                    mIntersectCount = 0;
                    mPreviousObjectRect = biggest;
                }
            } else {
                mPreviousObjectRect = null;
                mIntersectCount = 0;
            }

            long tsMatEnd = System.currentTimeMillis();
            Log.v(TAG,"time diff (Mat Run) "+(tsMatEnd-tsMatStart));
        } else {
            mSubtractorInited = true;
            tsMatStart = System.currentTimeMillis();
            mMOG2.apply(rgb,fgMask,-1);

            long tsMatEnd = System.currentTimeMillis();
            Log.v(TAG,"time diff (Mat Train) "+(tsMatEnd-tsMatStart));
        }
        double areaTotal = 0;
        for(Rect rect:rects){
            Log.d(TAG,"UO Area "+rect.area()+" rect "+rect.toString());
            areaTotal += rect.area();
        }
        double diffarea = areaTotal /(DETECTION_IMAGE_WIDTH*DETECTION_IMAGE_HEIGHT)/100;

        //VideoActivity.setPixelDiff(diffarea);
        return rects.size()>0;
    }
    private RectF getFaceRectF(int []faceInfo){

        int left, top, right, bottom;
        int i = 0;
        left = faceInfo[1+14*i];
        top = faceInfo[2+14*i];
        right = faceInfo[3+14*i];
        bottom = faceInfo[4+14*i];

        RectF faceRectf = new RectF(left,top,right,bottom);
        return faceRectf;
    }
    private String calcFaceStyle(int[] faceInfo){
        int left, top, right, bottom;
        int i = 0;
        left = faceInfo[1+14*i];
        top = faceInfo[2+14*i];
        right = faceInfo[3+14*i];
        bottom = faceInfo[4+14*i];

        RectF faceRect = new RectF(left,top,right,bottom);
        //画特征点

        int[] eye_1 = new int[2];
        int[] eye_2 = new int[2];
        eye_1[0] = faceInfo[5+14*i];
        eye_2[0] = faceInfo[6+14*i];
        eye_1[1] = faceInfo[10+14*i];
        eye_2[1] = faceInfo[11+14*i];

        int eye_distance = abs(eye_1[0]-eye_2[0]);

        Rect rect = new Rect(left,top,right,bottom);

        int middle_point = (left + right)/2;
        int y_middle_point = (top + bottom) / 2;


        if (eye_1[0] > middle_point){
            Log.d(TAG,"(Left Eye on the Right) Add style");
            return "left_side";
        }
        if (eye_2[0] < middle_point){
            Log.d(TAG,"(Right Eye on the left) Add style");
            return "right_side";
        }
        if (Math.max(eye_1[1], eye_2[1]) > y_middle_point){
            Log.d(TAG,"(Eye lower than middle of face) Skip");
            return "lower_head";
        }
        if (faceRect.width()/eye_distance > 6){
            Log.d(TAG,"side_face, eye distance is "+eye_distance+", face width is "+faceRect.width());
            return "side_face";
        }
        //#elif nose[1] < y_middle_point:
        //#    # 鼻子的y轴高于图片的中间高度，就认为是抬头
        //#    style.append('raise_head')

        return "front";
    }
    private int calcBitmapBlurry(Bitmap bmp){
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);

        Mat matGray = new Mat();
        Mat destination = new Mat();

        Imgproc.cvtColor(mat, matGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(matGray, destination, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(destination, median, std);

        return (int) Math.pow(std.get(0, 0)[0], 2.0);
    }
    public int doFaceDetectionAndSendTask(List<Classifier.Recognition> result, Bitmap bmp){
        long tsStart;
        long tsEnd;
        int face_num = 0;
        String filename = "";
        File file = null;

        for(final Classifier.Recognition recognition:result){
            tsStart = System.currentTimeMillis();
            RectF rectf = recognition.getLocation();
            Log.d(TAG,"recognition rect: "+rectf.toString());
            Bitmap personBmp = getCropBitmapByCPU(bmp,rectf);
            int[] face_info = mFaceDetector.predict_image(personBmp);
            tsEnd = System.currentTimeMillis();
            Log.v(TAG,"time diff (FD) "+(tsEnd-tsStart));

            int num = 0;
            if(face_info != null && face_info.length > 0){
                num = face_info[0];
            }
            if(num > 0){
                face_num+=num;
                try {
                    tsStart = System.currentTimeMillis();
                    file = screenshot.getInstance()
                            .saveScreenshotToPicturesFolder(mContext, personBmp, "person_");

                    filename = file.getAbsolutePath();

                    String faceStyle = calcFaceStyle(face_info);
                    Log.d(TAG,"Face style is "+faceStyle);
                    RectF faceRectF = getFaceRectF(face_info);

                    Bitmap faceBmp = getCropBitmapByCPU(personBmp,faceRectF);
                    Bitmap resizedBmp = mMotionDetection.resizeBmp(faceBmp,FACE_SAVING_WIDTH,FACE_SAVING_HEIGHT);
                    int blurryValue = calcBitmapBlurry(resizedBmp);
                    File faceFile = screenshot.getInstance()
                            .saveFaceToPicturesFolder(mContext, resizedBmp, "face_");

                    tsEnd = System.currentTimeMillis();
                    Log.v(TAG,"time diff (Save) "+(tsEnd-tsStart));
                    Log.d(TAG,"Blurry value of face is "+blurryValue+", saving face into "+faceFile.getAbsolutePath());

                } catch (Exception e) {
                    e.printStackTrace();

                    //delete all jpg file in Download dir when disk is full
                    deleteAllCapturedPics();
                }
                //bitmap.recycle();
                //bitmap = null;
                if(filename.equals("")){
                    continue;
                }
                if(file == null){
                    continue;
                }

                mLastTaskSentTimestamp = System.currentTimeMillis();
                mBackgroundHandler.obtainMessage(PROCESS_SAVED_IMAGE_MSG, filename).sendToTarget();
            }
        }

        //VideoActivity.setNumberOfFaces(face_num);
        return face_num;
    }
    public void doSendDummyTask(Bitmap bmp){
        String filename = "";
        File file = null;

        long tsStart = System.currentTimeMillis();
        RectF rectf = new RectF(0.0f,0.0f,1.0f,1.0f);
        Log.d(TAG,"dummy recognition rect: "+rectf.toString());
        Bitmap personBmp = getCropBitmapByCPU(bmp,rectf);
        try {
            tsStart = System.currentTimeMillis();
            file = screenshot.getInstance()
                    .saveScreenshotToPicturesFolder(mContext, personBmp, "frame_");

            filename = file.getAbsolutePath();
            long tsEnd = System.currentTimeMillis();
            Log.v(TAG,"time diff (Save) "+(tsEnd-tsStart));

        } catch (Exception e) {
            e.printStackTrace();

            //delete all jpg file in Download dir when disk is full
            deleteAllCapturedPics();
        }
        if(filename.equals("")){
            return;
        }
        if(file == null){
            return;
        }
        mBackgroundHandler.obtainMessage(PROCESS_SAVED_IMAGE_MSG, filename).sendToTarget();

        return;
    }
    private void checkIfNeedSendDummyTask(Bitmap bmp){

        long tm = System.currentTimeMillis();
        if (tm - mLastTaskSentTimestamp > 30*1000) {
            mLastTaskSentTimestamp = System.currentTimeMillis();
            doSendDummyTask(bmp);
            Log.d(TAG,"To send dummy task to keep alive for client status");
        }

    }
    public void processBitmap(Bitmap bmp){
        long tsStart = System.currentTimeMillis();
        long tsEnd;

        boolean bigChanged = true;

        //clean up pictures every 2 mins
        if (tsStart-mLastCleanPicsTimestamp > 2*60*1000) {
            Log.d("##RDBG", "clean pictures every 2 mins");
            mLastCleanPicsTimestamp = tsStart;
            deleteAllCapturedPics();
        }

        if(mPreviousPersonNum == 0){
            bigChanged = mMotionDetection.detect(bmp);
            //VideoActivity.setMotionStatus(bigChanged);
            tsEnd = System.currentTimeMillis();

            Log.v(TAG,"time diff (motion) "+(tsEnd-tsStart));
            //VideoActivity.setPixelDiff(mMotionDetection.getPercentageOfDifferentPixels());
            if(!bigChanged){
                if(mSavingCounter > 0){
                    mSavingCounter--;
                } else {
                    //bmp.recycle();
                    //VideoActivity.setMotionStatus(false);
                    //VideoActivity.setNumberOfFaces(0);
                    boolean ifChanged = detectObjectChanges(bmp);
                    Log.d(TAG,"Object changed after person leaving: "+ifChanged);
                    checkIfNeedSendDummyTask(bmp);
                    if(!ifChanged && mRecording){
                        //FFmpeg.cancel();
                        //String result = FFmpeg.getLastCommandOutput();
                        //Log.d(TAG,"FFMPEG output is "+result);
                    }
                    return;
                }
            } else {
                mSavingCounter=PROCESS_FRAMES_AFTER_MOTION_DETECTED;
            }
        }
        tsStart = System.currentTimeMillis();
        List<Classifier.Recognition> result =  mDetector.processImage(bmp);
        int personNum = result.size();
        mPreviousPersonNum = personNum;
        //VideoActivity.setNumberOfPerson(personNum);
        tsEnd = System.currentTimeMillis();
        Log.v(TAG,"time diff (OD) "+(tsEnd-tsStart));

        if(personNum>0){
            doFaceDetectionAndSendTask(result,bmp);
            if(mRecording==false){
                mRecording = true;
                //"-i", url, "-acodec", "copy", "-vcodec", "copy", targetFile.toString()
                Log.v(TAG,"FFMPEG Starting video recording");
                File mp4File = getOutputMediaFile("video_");
                File mp4File2 = getOutputMediaFile("video_resized_");
                //String command = "-i "+VideoActivity.getVideoURL()+
                        //" -c:v h264_mediacodec -b:v 500k"+
                        //" -vf scale=-2:640 " +
                //        " -vcodec copy " +
                //        mp4File;
                //mFFmpegRecorder = new FFmpegRecorder(mContext,VideoActivity.getVideoURL(),mp4File);
            }
        }

        checkIfNeedSendDummyTask(bmp);

        return;
    }

    /*public void processAndroidCameraBitmap(List<Classifier.Recognition> results,Bitmap bmp){
        long tsStart = System.currentTimeMillis();
        long tsEnd;

        boolean bigChanged = true;

        //clean up pictures every 2 mins
        if (tsStart-mLastCleanPicsTimestamp > 2*60*1000) {
            Log.d("##RDBG", "clean pictures every 2 mins");
            mLastCleanPicsTimestamp = tsStart;
            deleteAllCapturedPics();
        }
        doFaceDetectionAndSendTask(results,bmp);
        checkIfNeedSendDummyTask(bmp);

        return;
    }*/
    private File getOutputMediaFile(String filename) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDirectory = new File(
                getSDCardPath()
                        + File.separator+ Environment.DIRECTORY_DOWNLOADS);
        // Create the storage directory if it does not exist
        if (!mediaStorageDirectory.exists()) {
            if (!mediaStorageDirectory.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSS").format(new Date());
        File mediaFile;
        String mImageName = filename + timeStamp + ".mp4";
        mediaFile = new File(mediaStorageDirectory.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    private File getSDCardPath(){
        String path = null;
        File sdCardFile = null;
        List<String> sdCardPossiblePath = Arrays.asList("external_sd", "ext_sd", "external", "extSdCard");
        for (String sdPath : sdCardPossiblePath) {
            File file = new File("/mnt/", sdPath);
            if (file.isDirectory() && file.canWrite()) {
                path = file.getAbsolutePath();
                String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
                File testWritable = new File(path, "test_" + timeStamp);
                if (testWritable.mkdirs()) {
                    testWritable.delete();
                }
                else {
                    path = null;
                }
            }
        }
        if (path != null) {
            sdCardFile = new File(path);
        }
        else {
            sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        return sdCardFile;
    }
    private Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(/*www.j  a va2  s  .  co  m*/
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        //if (source != null && !source.isRecycled()) {
        //    source.recycle();
        //}

        return resultBitmap;
    }
    public Bitmap yuv2Bitmap(byte[] data,int width,int height){
        return Nv21Image.nv21ToBitmap(mRS,data,width,height);
    }
    private Thread mDeletePicsThread = null;

    private void deleteAllCapturedPics() {
        if (mDeletePicsThread != null) {
            return;
        }

        long now = System.currentTimeMillis();

        mDeletePicsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File[] files = f.listFiles();

                    for (File fPic: files) {
                        if (fPic.isFile()/* && fPic.getPath().endsWith(".jpg")*/ && (now - fPic.lastModified() > 1               *60*1000)) {
                            fPic.delete();
                        }
                    }
                }
                catch (Exception ex) {}

                mDeletePicsThread = null;
            }
        });
        mDeletePicsThread.start();
    }

}
