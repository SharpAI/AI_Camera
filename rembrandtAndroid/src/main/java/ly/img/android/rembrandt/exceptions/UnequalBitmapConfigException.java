package ly.img.android.rembrandt.exceptions;

import android.graphics.Bitmap;

/**
 * Created by winklerrr on 16/12/2016.
 */

public class UnequalBitmapConfigException extends RuntimeException {
    public UnequalBitmapConfigException(final Bitmap bitmap1, final Bitmap bitmap2) {
        super("Configs unequal: "
                + bitmap1.getConfig().toString() + " vs. "
                + bitmap2.getConfig().toString());
    }
}
