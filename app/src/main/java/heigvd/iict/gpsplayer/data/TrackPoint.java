package heigvd.iict.gpsplayer.data;

public class TrackPoint {
    // Timestamp in milliseconds since epoch
    public final long timestamp;
    public final double latitude;
    public final double longitude;
    public final double altitude;

    public TrackPoint(long timestamp, double lat, double lon, double altitude) {
        this.timestamp = timestamp;
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        return "TrackPoint[timestamp=" + timestamp + ",lat="+latitude +",lon="+longitude+"]";

    }
}
