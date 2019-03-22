package org.sharpai.aicamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * Created by jerikc on 15/10/6.
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.CameraInfo mCameraInfo;

    private long mLastNotifyStamp = System.currentTimeMillis();
    private boolean mNotifyPopupShown = false;
    private PopupWindow mNotifyPopupWindow = null;

    private SensorEventListener mSel = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                double angle = Math.atan2(x, y)/(Math.PI/180);
                if (angle < 105 && angle > 75) {
                    if (mNotifyPopupWindow != null) {
                        mNotifyPopupWindow.dismiss();
                    }
                    mNotifyPopupShown = false;
                }
                else {
                    if (mNotifyPopupWindow != null) {
                        if (!mNotifyPopupShown) {
                            mNotifyPopupShown = true;
                            mNotifyPopupWindow.showAtLocation(mPreview, Gravity.CENTER, 0, 0);
                        }
                    }

                    long curStamp = System.currentTimeMillis();
                    if (curStamp - mLastNotifyStamp > 5000) {
                        Toast.makeText(MainActivity.this, "请调整平板角度, 箭头朝上", Toast.LENGTH_SHORT).show();
                        mLastNotifyStamp = curStamp;

                        if (mNotifyPopupWindow == null) {
                            LayoutInflater inflater = (LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            mNotifyPopupWindow = new PopupWindow(inflater.inflate(R.layout.popup_orientation_guide, null, false), 200, 200);
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, mCameraInfo);
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
    }
    private static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if ( !nif.getName().equalsIgnoreCase("eth0") &&
                        !nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    // res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X",b));
                }

                //if (res1.length() > 0) {
                //    res1.deleteCharAt(res1.length() - 1);
                //}
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }
    private String getUniqueSerialNO(){
        String UDID = getMacAddr();
        if (UDID == null || UDID.length() == 0) {
            UDID = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }
        if (UDID == null || UDID.length() == 0) {
            UDID = "0000000";
        }

        return UDID.toLowerCase();
    }
    private void setFragment(){
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, mCameraInfo);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        ImageView imageView =  findViewById(R.id.qrcode_view);
        try {
            // generate a 150x150 QR code
            Bitmap bm = encodeAsBitmap(getUniqueSerialNO(), 150, 150);

            if(bm != null) {
                imageView.setImageBitmap(bm);
            }
            //preview.addView(imageView);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    Bitmap encodeAsBitmap(String str,int width,int height) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, width, height, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h);
        return bitmap;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }

        SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor accelerator = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(mSel, accelerator, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        sm.unregisterListener(mSel);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(MainActivity.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }
}
