package heigvd.ch.gpsplayer.data;

import java.util.List;

// Represents a Track
public class Track {
    public final List<TrackPoint> points;

    public Track(List<TrackPoint> points) {
        this.points = points;
    }
}
