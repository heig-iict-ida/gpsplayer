package heigvd.ch.gpsplayer.ui;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import heigvd.ch.gpsplayer.Globals;
import heigvd.ch.gpsplayer.R;
import heigvd.ch.gpsplayer.background.LocationSentEvent;
import heigvd.ch.gpsplayer.data.Track;
import heigvd.ch.gpsplayer.data.TrackPoint;

public class TrackViewActivity extends FragmentActivity {
    private final static String TAG = "TrackViewActivity";

    private Globals mGlobals;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Polyline mPolyline;

    // Markers for start, end and current position
    private Marker startMarker;
    private Marker endMarker;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGlobals = Globals.getInstance(this);
        setContentView(R.layout.activity_track_view);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        mGlobals.eventBus.register(this);

        if (mMap != null) {
            refreshMarkers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGlobals.eventBus.unregister(this);
    }

    private void refreshMarkers() {
        if (mPolyline != null) {
            mPolyline.remove();
        }
        LatLngBounds.Builder bounds = LatLngBounds.builder();

        final Track track = Globals.getInstance(this).getCurrentTrack();
        PolylineOptions polyline = new PolylineOptions();
        polyline.width(5);
        polyline.color(Color.BLUE);

        for (TrackPoint pt : track.points) {
            final LatLng pos = new LatLng(pt.latitude, pt.longitude);
            polyline.add(pos);
            bounds.include(pos);
        }
        mPolyline = mMap.addPolyline(polyline);

        final int paddingPx = 100;
        //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), paddingPx));
        // Cannot use the shorter version because it has to wait until layout is completed
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),
                        this.getResources().getDisplayMetrics().widthPixels,
                        this.getResources().getDisplayMetrics().heightPixels,
                        paddingPx));

        if (startMarker == null) {
            TrackPoint p = track.points[0];

            startMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.latitude, p.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.latitude, p.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            p = track.points[track.points.length - 1];

            endMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.latitude, p.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    public void onEvent(final LocationSentEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TrackPoint p = event.point;
                currentMarker.setPosition(new LatLng(p.latitude, p.longitude));
            }
        });
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

    }
}
