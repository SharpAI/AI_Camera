package ly.img.android.rembrandt;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Short4;
import android.util.Log;

import ly.img.android.rembrandt.exceptions.UnequalBitmapConfigException;
import ly.img.android.rembrandt.exceptions.UnequalBitmapSizesException;

/**
 * Created by winklerrr on 13/12/2016.
 */

public class Rembrandt {

    private static Application context;
    private RembrandtCompareOptions compareOptions;
    private RenderScript renderScript;
    private ScriptC_rembrandt rembrandtRenderScript;

    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap comparisonBitmap;
    private Bitmap.Config config;
    private int width;
    private int height;

    private Allocation allocationBitmap1;
    private Allocation allocationBitmap2;
    private Allocation allocationComparisonBitmap;
    private Allocation allocationTotalDifferentPixel;
    private Allocation allocationDiffRect;
    private int[] totalDifferentPixel = new int[1];
    private int[] diffRect = new int[4];

    private Short4 colorEquality;
    private Short4 colorDiversity;

    public Rembrandt() {
        this(RembrandtCompareOptions.createDefaultOptions());
    }

    public Rembrandt(RenderScript rs) {
        this(RembrandtCompareOptions.createDefaultOptions(),rs);
    }
    public Rembrandt(final RembrandtCompareOptions compareOptions, RenderScript rs) {
        this.compareOptions = compareOptions;
        renderScript = rs;
    }

    public Rembrandt(final RembrandtCompareOptions compareOptions) {
        if (context == null) {
            throw new NullPointerException("Context is null. Forgot to call init?");
        }

        this.compareOptions = compareOptions;
        renderScript = RenderScript.create(context);
    }

    public static void init(final Application context) {
        Rembrandt.context = context;
    }

    public RembrandtComparisonResult compareBitmaps(Bitmap bitmap1,Bitmap bitmap2) {
        this.bitmap1 = bitmap1;
        this.bitmap2 = bitmap2;

        prepareComparison();
        compare();
        return createComparisonResult();
    }

    private void prepareComparison() {
        createComparisonBitmap();
        createRembrandtRenderScript();
    }

    private void compare() {
        rembrandtRenderScript.forEach_compareBitmaps(allocationComparisonBitmap);

        allocationComparisonBitmap.syncAll(Allocation.USAGE_SCRIPT);
        //allocationComparisonBitmap.copyTo(comparisonBitmap);
    }

    private RembrandtComparisonResult createComparisonResult() {
        final int pixelsInTotal = width * height;

        rembrandtRenderScript.get_allocationTotalDifferentPixel().copyTo(totalDifferentPixel);
        rembrandtRenderScript.get_allocationDiffRect().copyTo(diffRect);

        //rembrandtRenderScript.get_allocationComparisonBitmap().copyTo(comparisonBitmap);

        int differentPixels = totalDifferentPixel[0];
//        long differentPixels = rembrandt.get_pix();
        final double percentageOfDifferentPixels = differentPixels * 1.0f / pixelsInTotal;
//      bitmapsSeemToBeEqual = isPercentageOfDifferentPixelsAcceptable(percentageOfDifferentPixels);
        return new RembrandtComparisonResult(differentPixels, percentageOfDifferentPixels, null, diffRect);
        //return new RembrandtComparisonResult(0, 0, comparisonBitmap, false);
    }

    private void createComparisonBitmap() {
        readBitmapProperties();
        comparisonBitmap = Bitmap.createBitmap(width, height, config);
    }

    private void createRembrandtRenderScript() {
        createAllocations();
        initRembrandtRenderScript();
    }

    private void readBitmapProperties() {
        readBitmapConfig();
        readBitmapDimensions();
    }

    private void createAllocations() {
        allocationBitmap1 = Allocation.createFromBitmap(renderScript, bitmap1);
        allocationBitmap2 = Allocation.createFromBitmap(renderScript, bitmap2);
        allocationComparisonBitmap = Allocation.createFromBitmap(renderScript, comparisonBitmap);

        colorEquality = convertColorToShort4(compareOptions.getColorForEquality());
        colorDiversity = convertColorToShort4(compareOptions.getColorForDiversity());

        allocationTotalDifferentPixel = Allocation.createSized(renderScript,
                Element.I32(renderScript), totalDifferentPixel.length);

        allocationDiffRect = Allocation.createSized(renderScript,
                Element.I32(renderScript), diffRect.length);
    }

    private void initRembrandtRenderScript() {
        rembrandtRenderScript = new ScriptC_rembrandt(renderScript);

        rembrandtRenderScript.set_rsAllocationBitmap1(allocationBitmap1);
        rembrandtRenderScript.set_rsAllocationBitmap2(allocationBitmap2);
        rembrandtRenderScript.set_rsAllocationComparisonBitmap(allocationComparisonBitmap);
        rembrandtRenderScript.set_allowedColorDistance(compareOptions.getMaximumColorDistance());
        rembrandtRenderScript.set_colorEquality(colorEquality);
        rembrandtRenderScript.set_colorDiversity(colorDiversity);
        rembrandtRenderScript.set_isClean(true);

        rembrandtRenderScript.set_gwidth(width);
        rembrandtRenderScript.set_gheight(height);

        //allocationComparisonBitmap.copyFrom(comparisonBitmap);
        //rembrandtRenderScript.bind_allocationComparisonBitmap(allocationComparisonBitmap);

        totalDifferentPixel[0] = 0;
        allocationTotalDifferentPixel.copyFrom(totalDifferentPixel);
        rembrandtRenderScript.bind_allocationTotalDifferentPixel(allocationTotalDifferentPixel);


        diffRect[0] = 0;
        diffRect[1] = 0;
        diffRect[2] = 0;
        diffRect[3] = 0;
        allocationDiffRect.copyFrom(diffRect);
        rembrandtRenderScript.bind_allocationDiffRect(allocationDiffRect);
    }

    private void readBitmapConfig() {
        if (areBitmapConfigsEqual()) {
            config = bitmap1.getConfig();
        } else {
            throw new UnequalBitmapConfigException(bitmap1, bitmap2);
        }
    }

    private void readBitmapDimensions() {
        if (areBitmapDimensionsEqual()) {
            width = bitmap1.getWidth();
            height = bitmap1.getHeight();
        } else {
            throw new UnequalBitmapSizesException(bitmap1, bitmap2);
        }
    }

    private boolean areBitmapConfigsEqual() {
        return bitmap1.getConfig() == bitmap2.getConfig();
    }

    private boolean areBitmapDimensionsEqual() {
        return bitmap1.getHeight() == bitmap2.getHeight() && bitmap1.getWidth() == bitmap2.getWidth();
    }

    private static Short4 convertColorToShort4(final int color) {
        final short red = (short) Color.red(color);
        final short green = (short) Color.green(color);
        final short blue = (short) Color.blue(color);
        final short alpha = (short) Color.alpha(color);

        return new Short4(red, green, blue, alpha);
    }

    private boolean isPercentageOfDifferentPixelsAcceptable(final double percentageOfDifferentPixels) {
        return percentageOfDifferentPixels <= compareOptions.getMaximumPercentageOfDifferentPixels();
    }

    public RembrandtCompareOptions getCompareOptions() {
        return compareOptions;
    }

    public void setCompareOptions(RembrandtCompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }
}
