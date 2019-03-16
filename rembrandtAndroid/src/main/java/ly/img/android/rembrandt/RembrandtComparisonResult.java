package ly.img.android.rembrandt;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by winklerrr on 13/12/2016.
 */

public class RembrandtComparisonResult implements Parcelable {

    private final int differentPixels;
    private final double percentageOfDifferentPixels;
    //private final Bitmap comparisionBitmap;
    private final Rect mRect = new Rect();

    public RembrandtComparisonResult(final int differentPixels, final double percentageOfDifferentPixels, final Bitmap comparisionBitmap,int [] diffArea) {
        this.differentPixels = differentPixels;
        this.percentageOfDifferentPixels = percentageOfDifferentPixels;
        //this.comparisionBitmap = comparisionBitmap;

        this.mRect.top = diffArea[0];
        this.mRect.bottom = diffArea[1];
        this.mRect.left = diffArea[2];
        this.mRect.right = diffArea[3];
    }

    public Rect getLastRect(){
        return mRect;
    }
    public int getDifferentPixels() {
        return differentPixels;
    }

    public double getPercentageOfDifferentPixels() {
        return percentageOfDifferentPixels;
    }

    //public Bitmap getComparisionBitmap() {
    //    return comparisionBitmap;
    //}


    protected RembrandtComparisonResult(Parcel in) {
        differentPixels = in.readInt();
        percentageOfDifferentPixels = in.readDouble();
        //comparisionBitmap = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(differentPixels);
        dest.writeDouble(percentageOfDifferentPixels);
        //dest.writeValue(comparisionBitmap);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<RembrandtComparisonResult> CREATOR = new Parcelable.Creator<RembrandtComparisonResult>() {
        @Override
        public RembrandtComparisonResult createFromParcel(Parcel in) {
            return new RembrandtComparisonResult(in);
        }

        @Override
        public RembrandtComparisonResult[] newArray(int size) {
            return new RembrandtComparisonResult[size];
        }
    };
}