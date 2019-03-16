package ly.img.android.rembrandt;

import android.graphics.Color;

/**
 * Created by winklerrr on 13/12/2016.
 */

public class RembrandtCompareOptions {

    // This value is color difference threshold
    public static final float DEFAULT_MAXIMUM_COLOR_DISTANCE = 150.0f;
    public static final float DEFAULT_MAXIMUM_PERCENTAGE_OF_DIFFERENT_PIXELS = 1.0f;
    public static final int DEFAULT_COLOR_BITMAP_PIXEL_EQUAL = Color.TRANSPARENT;
    public static final int DEFAULT_COLOR_BITMAP_PIXEL_DIFFERENT = Color.BLACK;

    private final float maximumColorDistance;
    private final float maximumPercentageOfDifferentPixels;
    private final int colorForEquality;
    private final int colorForDiversity;

    public RembrandtCompareOptions(final float maximumColorDistance, final float maximumPercentageDifference, final int colorForEquality, final int colorForDiversity) {
        this.maximumColorDistance = maximumColorDistance;
        this.maximumPercentageOfDifferentPixels = maximumPercentageDifference;
        this.colorForEquality = colorForEquality;
        this.colorForDiversity = colorForDiversity;
    }

    public static RembrandtCompareOptions createDefaultOptions() {
        return new RembrandtCompareOptions(DEFAULT_MAXIMUM_COLOR_DISTANCE,
                DEFAULT_MAXIMUM_PERCENTAGE_OF_DIFFERENT_PIXELS,
                DEFAULT_COLOR_BITMAP_PIXEL_EQUAL,
                DEFAULT_COLOR_BITMAP_PIXEL_DIFFERENT);
    }

    public float getMaximumColorDistance() {
        return maximumColorDistance;
    }

    public float getMaximumPercentageOfDifferentPixels() {
        return maximumPercentageOfDifferentPixels;
    }

    public int getColorForEquality() {
        return colorForEquality;
    }

    public int getColorForDiversity() {
        return colorForDiversity;
    }
}
