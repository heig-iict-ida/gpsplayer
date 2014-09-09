package heigvd.ch.gpsplayer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import de.greenrobot.event.EventBus;
import heigvd.ch.gpsplayer.background.RunTrackService;
import heigvd.ch.gpsplayer.data.Track;
import heigvd.ch.gpsplayer.events.ServiceStateChangedEvent;
import heigvd.ch.gpsplayer.ui.TrackViewActivity;

public class Globals {
    private static Globals instance = null;

    public static Globals getInstance(Context context) {
        if (instance == null) {
            instance = new Globals(context.getApplicationContext());
        } else {
            if (instance.context != context.getApplicationContext()) {
                // I think this should not happen since the application is simple enough that it
                // runs in a single process (no contentprovider or broadcastreceiver).
                // If we extend the application and need to handle multiple contexts, we should
                // have a per-context Globals instance
                throw new IllegalStateException("Globals already instanciated with a different context");
            }
        }
        return instance;
    }

    private final static String TAG = "Globals";
    private Context context;
    private Track currentTrack = null;

    public final EventBus eventBus = new EventBus();

    public Globals(Context context) {
        this.context = context;
    }

    public void setCurrentTrack(Track track) {
        stopService();
        currentTrack = track;
    }

    public Track getCurrentTrack () {
        return currentTrack;
    }

    public boolean isCurrentTrack(File f) {
        return currentTrack != null && Utils.fileEquals(f, currentTrack.file);
    }

    private boolean serviceRunning = false;

    // This should only be call from the service
    public void setServiceRunning(boolean running) {
        serviceRunning = running;
        eventBus.post(new ServiceStateChangedEvent());
    }

    public void startService() {
        final Intent intent = new Intent(context, RunTrackService.class);
        context.startService(intent);
        setServiceRunning(true);

    }

    public void stopService() {
        context.stopService(new Intent(context, RunTrackService.class));
        setServiceRunning(false);
    }

    public boolean isRunning() {
        return serviceRunning;
    }
}
