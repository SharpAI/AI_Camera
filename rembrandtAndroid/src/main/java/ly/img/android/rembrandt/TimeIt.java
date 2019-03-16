package ly.img.android.rembrandt;

import android.os.SystemClock;
import android.util.Log;

public class TimeIt {

    private long startTime;
    private final String logIdentifier;

    public TimeIt(final String logIdentifier) {
        this.logIdentifier = logIdentifier;
        startTime = getCurrentTime();
    }

    public void restart() {
        startTime = getCurrentTime();
    }

    public long getElapsedTime() {
        return getCurrentTime() - startTime;
    }

    public void logElapsedTime() {
        Log.i(this.getClass().getSimpleName(), logIdentifier + " :: " + getElapsedTime());
    }

    private long getCurrentTime() {
        return SystemClock.elapsedRealtime();
    }

}
