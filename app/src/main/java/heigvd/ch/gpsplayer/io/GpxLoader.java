package heigvd.ch.gpsplayer.io;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import heigvd.ch.gpsplayer.Utils;
import heigvd.ch.gpsplayer.data.TrackPoint;
import heigvd.ch.gpsplayer.data.Track;

public class GpxLoader {
    public static class GpxException extends Exception {
        public GpxException(String message) {
            super(message);
        }

        public GpxException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    private final static String TAG = "GpxLoader";
    // See http://developer.android.com/training/basics/network-ops/xml.html
    // for doc on XmlPullParser

    // We don't use namespaces
    private static final String ns = null;

    public static Track Load(File file) throws GpxException {
        Track track = null;
        try {
            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            parser.setInput(new FileReader(file));
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, ns, "gpx");

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag = parser.getName();
                if (tag.equals("trk")) {
                    return readTrack(file, parser);
                } else {
                    skip(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new GpxException("Error parsing XML : " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new GpxException("Error loading file : " + file.getAbsolutePath(), e);
        }

        throw new GpxException("No <trk> found in GPX file");
    }

    private static Track readTrack(File file, XmlPullParser parser) throws IOException, XmlPullParserException, GpxException {
        List<TrackPoint> trackPoints = null;

        parser.require(XmlPullParser.START_TAG, ns, "trk");
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            if (tag.equals("trkseg")) {
                if (trackPoints != null) {
                    // TODO: Merge segments instead ?
                    Log.w(TAG, "Multiple segments in track. Only last one will be considered");
                }
                trackPoints = readSegment(parser);
            } else {
                skip(parser);
            }
        }

        if (trackPoints == null) {
            throw new GpxException("No <trkseg> found in GPX file");
        }
        return new Track(file, trackPoints);
    }

    // Read a <trkseg>
    private static List<TrackPoint> readSegment(XmlPullParser parser) throws IOException, XmlPullParserException, GpxException {
        List<TrackPoint> trackPoints = new ArrayList<TrackPoint>();

        parser.require(XmlPullParser.START_TAG, ns, "trkseg");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("trkpt")) {
                trackPoints.add(readTrackPoint(parser));
            } else {
                skip(parser);
            }
        }
        return trackPoints;

    }

    private static TrackPoint readTrackPoint(XmlPullParser parser) throws IOException, XmlPullParserException, GpxException {
        parser.require(XmlPullParser.START_TAG, ns, "trkpt");
        final double latitude = Double.parseDouble(parser.getAttributeValue(ns, "lat"));
        final double longitude = Double.parseDouble(parser.getAttributeValue(ns, "lon"));

        double altitude = 0;
        long timestamp = 0;

        // read <ele> and <time>
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("ele")) {
                altitude = readElevation(parser);
            } else if (name.equals("time")) {
                timestamp = readTime(parser);
            } else {
                skip(parser);
            }
        }

        // We must have a <time> entry
        if (timestamp == 0) {
            throw new XmlPullParserException("No <time> found for point", parser, null);
        }

        return new TrackPoint(timestamp, latitude, longitude, altitude);
    }

    private static long readTime(XmlPullParser parser) throws XmlPullParserException, GpxException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "time");
        final String timestring = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "time");
        try {
            return Utils.dateFormatISO8601.parse(timestring).getTime();
        } catch (ParseException e) {
            throw new GpxException("Error parsing timestring : " + timestring, e);
        }
    }

    private static double readElevation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "ele");
        final String ele = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "ele");
        return Double.parseDouble(ele);
    }

    // Extract tag's text value
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
