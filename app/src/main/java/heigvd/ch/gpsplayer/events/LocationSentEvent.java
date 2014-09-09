package heigvd.ch.gpsplayer.events;

import heigvd.ch.gpsplayer.data.TrackPoint;

// Event sent when RunTrackService sends a new location to LocationManager
public class LocationSentEvent {
    public final TrackPoint point;

    public LocationSentEvent(TrackPoint p) {
        this.point = p;
    }

    @Override
    public String toString() {
        return "LocationSentEvent["+point.toString()+"]";
    }
}
