package heigvd.ch.gpsplayer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Utils {
    private final static String TAG = "Utils";

    public final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public static File getStorageDirectory() {
        final File d = Environment.getExternalStoragePublicDirectory("gpsplayer");
        d.mkdirs();
        return d;
    }

    public static void showMessage(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    public static boolean fileEquals(File f1, File f2) {
        if (f1 == f2) {
            return true;
        }
        if (f1 == null || f2 == null) {
            return false;
        }
        try {
            return f1.getCanonicalPath().equals(f2.getCanonicalPath());
        } catch (IOException e) {
            Log.e(TAG, "fileEquals", e);
            return false;
        }

    }
}
