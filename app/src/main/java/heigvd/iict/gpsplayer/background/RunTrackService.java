package heigvd.iict.gpsplayer.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import heigvd.iict.gpsplayer.Globals;
import heigvd.iict.gpsplayer.R;
import heigvd.iict.gpsplayer.data.Track;
import heigvd.iict.gpsplayer.data.TrackPoint;
import heigvd.iict.gpsplayer.events.LocationSentEvent;
import heigvd.iict.gpsplayer.ui.TrackViewActivity;

// A background service that will use points in the current active track to set mock locations
public class RunTrackService extends Service {
    private final static String TAG = "RunTrackService";
    private final static int NOTIF_ID = 1;

    private final long MIN_LOC_UPDATE_INTERVAL = 200;

    private final static String EXTRA_REQUEST_CODE_KEY = "code";
    private final static int PLAY_STOP_REQUEST_CODE = 1;

    // Providers to mock
    private final static String[] PROVIDERS = new String[]{
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER};

    private LocationManager mLocationManager;

    private Track mTrack;

    private LocationUpdater mWorkerThread;
    private Globals mGlobals;

    private NotificationManager mNotifManager;

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

        // Display a notification while this service is running, letting the user control the
        // service directly from the notification
        mNotifManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        startForeground(NOTIF_ID, getBaseNotifBuilder().getNotification());

        startWorker();
    }

    private Notification.Builder getBaseNotifBuilder() {
        // Intent used when user clicks on notification
        Intent notifIntent = new Intent(this, TrackViewActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return new Notification.Builder(this)
                .setContentTitle("Gpsplayer")
                .setContentText("Playing mock location from track")
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentIntent(PendingIntent.getActivity(this, 0, notifIntent, 0))
                .setOngoing(true)
                .setProgress(100, 0, false);
    }

    private void startWorker() {
        mWorkerThread = new LocationUpdater(MIN_LOC_UPDATE_INTERVAL);
        mWorkerThread.start();
    }

    private class LocationUpdater extends Thread {
        private boolean mQuit = false;

        private long mIntervalMS;

        private final PendingIntent mStopIntent;

        public LocationUpdater(long interval) {
            mIntervalMS = interval;

            // Build stop pending intent to be sent from notification actions
            // We include the requestcode as an extra so we can access it in onStartCommand
            final Context ctx = RunTrackService.this;
            final Intent serviceStopIntent = new Intent(ctx, RunTrackService.class);
            serviceStopIntent.putExtra(EXTRA_REQUEST_CODE_KEY, PLAY_STOP_REQUEST_CODE);
            mStopIntent = PendingIntent.getService(ctx, PLAY_STOP_REQUEST_CODE, serviceStopIntent, 0);
        }

        // Plays the track. Returns either after quit() was called to interrupt or when the
        // track is finished
        @Override
        public void run() {
            Log.i(TAG, "Mock location service started");

            final long startTime = System.currentTimeMillis();
            int prevIndex = -1;

            int prevProgress = -1;

            mGlobals.setServiceRunning(true);

            Notification.Builder runningBuilder = getBaseNotifBuilder();
            runningBuilder.addAction(R.drawable.ic_action_stop, "Stop", mStopIntent);

            while (!mQuit) {
                final long now = System.currentTimeMillis();
                final long elapsed = now - startTime;

                if (elapsed > mTrack.getDuration()) {
                    Log.i(TAG, "Track finished");
                    /*Notification.Builder finishedBuilder = getBaseNotifBuilder();
                    finishedBuilder.setProgress(0, 0, false);
                    finishedBuilder.setContentText("Track finished");
                    mNotifManager.notify(NOTIF_ID, finishedBuilder.build());*/
                    stopSelf();
                    break;
                }

                final int progress = (int)(100.0f * elapsed / (float)mTrack.getDuration());

                if (progress != prevProgress) {
                    runningBuilder.setProgress(100, progress, false);
                    runningBuilder.setContentText("Playing mock location from track - " + progress + "%");
                    mNotifManager.notify(NOTIF_ID, runningBuilder.getNotification());
                    prevProgress = progress;
                }

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
        if (intent != null && intent.hasExtra(EXTRA_REQUEST_CODE_KEY)) {
            final int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE_KEY, -1);
            if (requestCode == PLAY_STOP_REQUEST_CODE) {
                stopSelf();
            } else {
                // TODO: This shouldn't happen : report to rollbar
                Log.e(TAG, "Invalid requestCode : " + requestCode);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping service");
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

        mGlobals.setServiceRunning(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
