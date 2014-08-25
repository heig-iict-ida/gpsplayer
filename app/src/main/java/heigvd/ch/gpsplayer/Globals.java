package heigvd.ch.gpsplayer;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;

import de.greenrobot.event.EventBus;
import heigvd.ch.gpsplayer.background.RunTrackService;
import heigvd.ch.gpsplayer.data.Track;

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

    private boolean serviceRunning = false;
    public void startService() {
        final Intent intent = new Intent(context, RunTrackService.class);
        context.startService(intent);
        serviceRunning = true;
    }

    public void stopService() {
        context.stopService(new Intent(context, RunTrackService.class));
        serviceRunning = false;
    }

    public boolean isRunning() {
        return serviceRunning;
    }
}
