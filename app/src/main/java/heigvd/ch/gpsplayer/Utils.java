package heigvd.ch.gpsplayer;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Utils {
    public final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public static void showMessage(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }
}
