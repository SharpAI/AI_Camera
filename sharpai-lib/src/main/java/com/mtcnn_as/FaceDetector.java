package com.mtcnn_as;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FaceDetector {

    private int minFaceSize = 80;
    private int testTimeCount = 1;
    private int threadsNumber = 2;

    private final String TAG="FaceDetector";
    private boolean maxFaceSetting = false;
    private MTCNN mtcnn = new MTCNN();
    Context mContext;

    public FaceDetector(Context context) {
        mContext = context;
        //拷贝模型到sk卡
        try {
            copyBigDataToCache("det1.bin");
            copyBigDataToCache("det2.bin");
            copyBigDataToCache("det3.bin");
            copyBigDataToCache("det1.param");
            copyBigDataToCache("det2.param");
            copyBigDataToCache("det3.param");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //模型初始化
        File sdDir = context.getCacheDir();//获取跟目录
        Log.i("SDdir", sdDir.toString());
        String sdPath = sdDir.toString();
        mtcnn.FaceDetectionModelInit(sdPath);
        mtcnn.SetMinFaceSize(minFaceSize);
        mtcnn.SetTimeCount(testTimeCount);
        mtcnn.SetThreadsNumber(threadsNumber);
    }

    public int predict_image(Bitmap yourSelectedImage){
        if (yourSelectedImage == null)
            return 0;

        if (threadsNumber != 1&&threadsNumber != 2&&threadsNumber != 4&&threadsNumber != 8){
            Log.i(TAG, "线程数："+threadsNumber);
            //infoResult.setText("线程数必须是（1，2，4，8）之一");
            return 0;
        }

        //Log.i(TAG, "最小人脸："+minFaceSize);
        //mtcnn.SetMinFaceSize(minFaceSize);
        //mtcnn.SetTimeCount(testTimeCount);
        //mtcnn.SetThreadsNumber(threadsNumber);

        //检测流程
        int width = yourSelectedImage.getWidth();
        int height = yourSelectedImage.getHeight();
        byte[] imageDate = getPixelsRGBA(yourSelectedImage);

        long timeDetectFace = System.currentTimeMillis();
        int faceInfo[] = null;
        if(!maxFaceSetting) {
            faceInfo = mtcnn.FaceDetect(imageDate, width, height, 4);
            Log.i(TAG, "检测所有人脸");
        }
        else{
            faceInfo = mtcnn.MaxFaceDetect(imageDate, width, height, 4);
            Log.i(TAG, "检测最大人脸");
        }
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "人脸平均检测时间："+timeDetectFace/testTimeCount);

        if(faceInfo.length>1){
            int faceNum = faceInfo[0];
            //infoResult.setText("图宽："+width+"高："+height+"人脸平均检测时间："+timeDetectFace/testTimeCount+" 数目：" + faceNum +
            //        index + "/" + times);
            //if (index == times){
                //infoResult.setText("视频播放结束");
            //}
            Log.i(TAG, "图宽："+width+"高："+height+" 人脸数目：" + faceNum );
            return faceNum;
/*
            Bitmap drawBitmap = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
            for(int i=0;i<faceNum;i++) {
                int left, top, right, bottom;
                Canvas canvas = new Canvas(drawBitmap);
                Paint paint = new Paint();
                left = faceInfo[1+14*i];
                top = faceInfo[2+14*i];
                right = faceInfo[3+14*i];
                bottom = faceInfo[4+14*i];
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);//不填充
                paint.setStrokeWidth(5);  //线的宽度
                canvas.drawRect(left, top, right, bottom, paint);
                //画特征点
                canvas.drawPoints(new float[]{faceInfo[5+14*i],faceInfo[10+14*i],
                        faceInfo[6+14*i],faceInfo[11+14*i],
                        faceInfo[7+14*i],faceInfo[12+14*i],
                        faceInfo[8+14*i],faceInfo[13+14*i],
                        faceInfo[9+14*i],faceInfo[14+14*i]}, paint);//画多个点
            }
*/
            //Message msg = new Message();
            //msg.obj = drawBitmap;
            //msg.what = 1;
            //update_ui.sendMessage(msg);
            //imageView.setImageBitmap(drawBitmap);
        }else{
            //infoResult.setText("未检测到人脸" + index + "/" + times);
            //if (index == times){
                //infoResult.setText("视频播放结束");
            //}
            //Message msg = new Message();
            //msg.obj = yourSelectedImage;
            //update_ui.sendMessage(msg);
        }
        return 0;
    }

    //提取像素点
    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private void copyBigDataToCache(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File f = new File(mContext.getCacheDir()+"/"+strOutFileName);
        if (!f.exists()) try {

            InputStream is = mContext.getAssets().open(strOutFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e); }
        Log.i(TAG, "end copy file " + strOutFileName);

    }
}
