package org.sharpai.lib;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.TextureView;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.View.MeasureSpec;

/**
 * The type ScreenShott class.
 */
public class screenshot {
    private static final screenshot ourInstance = new screenshot();

    private screenshot() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static screenshot getInstance() {
        return ourInstance;
    }

    /**
     * Take screen shot of root view.
     *
     * @param v
     *     the v
     * @return the bitmap
     */
    public Bitmap takeScreenShotOfRootView(View v) {
        v = v.getRootView();
        return takeScreenShotOfView(v);
    }

    /**
     * Take screen shot of the View with spaces as per constraints
     *
     * @param v
     *     the v
     * @return the bitmap
     */
    public Bitmap takeScreenShotOfView(View v) {
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache(true);

        // creates immutable clone
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    /**
     * Take screen shot of texture view as bitmap.
     *
     * @param v
     *     the TextureView
     * @return the bitmap
     */
    public Bitmap takeScreenShotOfTextureView(TextureView v) {
        return v.getBitmap();
    }

    /**
     * Take screen shot of just the View without any constraints
     *
     * @param v
     *     the v
     * @return the bitmap
     */
    public Bitmap takeScreenShotOfJustView(View v) {
        v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        return takeScreenShotOfView(v);
    }

    /**
     * Save screenshot to pictures folder.
     *
     * @param context
     *     the context
     * @param image
     *     the image
     * @param filename
     *     the filename
     * @return the bitmap file object
     * @throws Exception
     *     the exception
     */
    public File saveScreenshotToPicturesFolder(Context context, Bitmap image, String filename)
            throws Exception {
        File bitmapFile = getOutputMediaFile(filename);
        if (bitmapFile == null) {
            throw new NullPointerException("Error creating media file, check storage permissions!");
        }
        FileOutputStream fos = new FileOutputStream(bitmapFile);
        //image.compress(Bitmap.CompressFormat.PNG, 90, fos);
        image.compress(Bitmap.CompressFormat.JPEG,90,fos);
        fos.flush();
        //fos.getFD().sync();
        fos.close();

        // Initiate media scanning to make the image available in gallery apps
        //MediaScannerConnection.scanFile(context, new String[] { bitmapFile.getPath() },
        //        new String[] { "image/jpeg" }, null);
        return bitmapFile;
    }
    /**
     * Save screenshot to pictures folder.
     *
     * @param context
     *     the context
     * @param image
     *     the image
     * @param filename
     *     the filename
     * @return the bitmap file object
     * @throws Exception
     *     the exception
     */
    public File saveFaceToPicturesFolder(Context context, Bitmap image, String filename)
            throws Exception {
        File bitmapFile = getPNGOutputMediaFile(filename);
        if (bitmapFile == null) {
            throw new NullPointerException("Error creating media file, check storage permissions!");
        }
        FileOutputStream fos = new FileOutputStream(bitmapFile);
        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
        //image.compress(Bitmap.CompressFormat.JPEG,90,fos);
        fos.flush();
        fos.getFD().sync();
        fos.close();

        return bitmapFile;
    }
    /**
     * Save screenshot to pictures folder.
     *
     * @param context
     *     the context
     * @param image
     *     the image
     * @param filename
     *     the filename
     * @return the bitmap file object
     * @throws Exception
     *     the exception
     */
    public File saveFaceToPicturesFolderWithOpenCV(Context context, Bitmap image, String filename)
        throws Exception {
        File bitmapFile = getPNGOutputMediaFile(filename);
        if (bitmapFile == null) {
            throw new NullPointerException("Error creating media file, check storage permissions!");
        }

        Mat rgba = new Mat();
        Utils.bitmapToMat(image, rgba);
        Mat rgb = new Mat();

        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2BGR);

        Imgcodecs.imwrite(bitmapFile.getAbsolutePath(),rgb);

        return bitmapFile;
    }
    private File getOutputMediaFile(String filename) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDirectory = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + File.separator);
        // Create the storage directory if it does not exist
        if (!mediaStorageDirectory.exists()) {
            if (!mediaStorageDirectory.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSS").format(new Date());
        File mediaFile;
        String mImageName = filename + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDirectory.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    private File getPNGOutputMediaFile(String filename) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDirectory = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + File.separator);
        // Create the storage directory if it does not exist
        if (!mediaStorageDirectory.exists()) {
            if (!mediaStorageDirectory.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss_SSS").format(new Date());
        File mediaFile;
        String mImageName = filename + timeStamp + ".png";
        mediaFile = new File(mediaStorageDirectory.getPath() + File.separator + mImageName);
        return mediaFile;
    }
}
