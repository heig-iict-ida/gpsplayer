package heigvd.ch.gpsplayer.background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import heigvd.ch.gpsplayer.Globals;
import heigvd.ch.gpsplayer.R;
import heigvd.ch.gpsplayer.data.Track;
import heigvd.ch.gpsplayer.data.TrackPoint;
import heigvd.ch.gpsplayer.ui.TrackViewActivity;

// A background service that will use points in the current active track to set mock locations
public class RunTrackService extends Service {
    private final static String TAG = "RunTrackService";
    private final static int NOTIF_ID = 1;

    private final long MIN_LOC_UPDATE_INTERVAL = 200;

    // Providers to mock
    private final static String[] PROVIDERS = new String[]{
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER};

    private LocationManager mLocationManager;

    private Track mTrack;

    private LocationUpdater mWorkerThread;
    private Globals mGlobals;

    @Override
    public void onCreate() {
        mLocationManager = (LocationManager) this.getSystemService(
                Context.LOCATION_SERVICE);
        mGlobals = Globals.getInstance(this);
        setupTestLocationProvider();

        mTrack = mGlobals.getCurrentTrack();
        if (mTrack == null) {
            throw new IllegalStateException("current track is null in service");
        }

        mWorkerThread = new LocationUpdater(MIN_LOC_UPDATE_INTERVAL);
        mWorkerThread.start();

        // We want to display a notification while the data collection service is running
        // Intent used when user clicks on notification. Should redirect to preference pane
        Intent notifIntent = new Intent(this, TrackViewActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Notification noti = new Notification.Builder(this)
                .setContentTitle("Gpsplayer")
                .setContentText("Setting mock location from track")
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_SOUND)
                .getNotification();
        noti.flags = Notification.FLAG_ONGOING_EVENT;
        noti.contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);

        startForeground(NOTIF_ID, noti);
    }

    private class LocationUpdater extends Thread {
        private long mStartTime;
        private boolean mQuit = false;

        private long mIntervalMS;

        public LocationUpdater(long interval) {
            mIntervalMS = interval;
        }

        @Override
        public void run() {
            mStartTime = System.currentTimeMillis();
            Log.i(TAG, "Mock location service started");

            int prevIndex = -1;

            while (!mQuit) {
                final long now = System.currentTimeMillis();
                final long elapsed = now - mStartTime;

                final int pointIndex = mTrack.closestTime(elapsed);
                // Only update if we get a new position
                if (prevIndex != pointIndex) {
                    final TrackPoint point = mTrack.points[pointIndex];
                    prevIndex = pointIndex;

                    for (String provider : PROVIDERS) {
                        Location mockLocation = new Location(provider);
                        mockLocation.setLatitude(point.latitude);
                        mockLocation.setLongitude(point.longitude);
                        mockLocation.setTime(System.currentTimeMillis());
                        mockLocation.setAltitude(point.altitude);
                        mockLocation.setAccuracy(10.0f);
                        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                        mLocationManager.setTestProviderLocation(provider, mockLocation);
                    }

                    mGlobals.eventBus.post(new LocationSentEvent(point));
                }

                try {
                    Thread.sleep(mIntervalMS);
                } catch (InterruptedException e) { }
            }

            Log.i(TAG, "Mock location service stopped");

        }

        public void quit() {
            mQuit = true;
            this.interrupt();
        }
    }

    private void setupTestLocationProvider() {
        boolean requiresNetwork = false;
        boolean requiresSatellite = false;
        boolean requiresCell = false;
        boolean hasMonetaryCost = false;
        boolean supportsAltitude = true;
        boolean supportsSpeed = true;
        boolean supportsBearing = false;
        int powerRequirement = 0;
        int accuracy = 10;
        for (String provider : PROVIDERS) {
            mLocationManager.addTestProvider(
                    provider,
                    requiresNetwork,
                    requiresSatellite,
                    requiresCell,
                    hasMonetaryCost,
                    supportsAltitude,
                    supportsSpeed,
                    supportsBearing,
                    powerRequirement,
                    accuracy);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mWorkerThread.quit();
        try {
            mWorkerThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker thread join ", e);
        }
        for (String provider : PROVIDERS) {
            mLocationManager.removeTestProvider(provider);
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
