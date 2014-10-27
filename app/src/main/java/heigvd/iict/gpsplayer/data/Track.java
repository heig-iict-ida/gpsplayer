package heigvd.iict.gpsplayer.data;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Represents a Track

// When a track is replayed, all times are relative to its mStartTime
public class Track {
    private final static String TAG = "Track";

    public final String name;
    public final File file;
    public final TrackPoint[] points;
    public final long[] timestamps;

    // timestamps relative to timestamps[0]
    private final long[] mRelTimestamps;

    public Track(File file, String name, List<TrackPoint> points) {
        this.name = name;
        this.file = file;
        // Ensure sorted points
        Collections.sort(points, new Comparator<TrackPoint>() {
            @Override
            public int compare(TrackPoint p1, TrackPoint p2) {
                return Long.valueOf(p1.timestamp).compareTo(Long.valueOf(p2.timestamp));
            }
        });
        this.points = points.toArray(new TrackPoint[1]);
        this.timestamps = new long[this.points.length];
        for (int i = 0; i < timestamps.length; ++i) {
            timestamps[i] = this.points[i].timestamp;
        }

        final long start = getStartTime();
        mRelTimestamps = new long[timestamps.length];
        for (int i = 0; i < timestamps.length; ++i) {
            mRelTimestamps[i] = timestamps[i] - start;
        }
    }

    public long getDuration() {
        return timestamps[timestamps.length - 1] - timestamps[0];
    }

    public long getStartTime() {
        return this.timestamps[0];
    }

    // Get index with timestamp (relative to mStartTime) closest to 'elapsed'
    public int closestTime(long elapsed) {
        final int pos = Arrays.binarySearch(mRelTimestamps, elapsed);
        if (pos >= 0) {
            return pos;
        } else {
            // pos is -index - 1 where element would be inserted
            int i = -pos - 1;
            if (i >= mRelTimestamps.length) {
                i = mRelTimestamps.length - 1;
            }
            return i;
        }
    }
}
