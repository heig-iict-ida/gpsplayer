package heigvd.iict.gpsplayer.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import heigvd.iict.gpsplayer.Globals;
import heigvd.iict.gpsplayer.R;
import heigvd.iict.gpsplayer.Utils;
import heigvd.iict.gpsplayer.data.Track;
import heigvd.iict.gpsplayer.io.GpxLoader;
import heigvd.iict.gpsplayer.ui.fragments.FilenameDialogFragment;

// An activity that will import a track (specified as a file in the intent's data uri)
public class ImportTrackActivity extends Activity implements FilenameDialogFragment.DialogListener {
    private final static String TAG = "ImportTrackActivity";

    private Globals mGlobals;

    private StreamWithFileNameHint mInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGlobals = Globals.getInstance(this);

        setContentView(R.layout.activity_import_track);

        // We proceed as follow :
        // 1. Get the stream and filename hint from intent (GetInputTask)
        // 2. Ask the user a filename for the new track (FilenameDialogFragment)
        // 3. Copy the GPX file to our own directory (CopyFileTask)
        // 4. Load the track from GPX (ImportGPXTask)
        // 5. Goto new track view
        new GetInputTask(getIntent()).execute();
    }

    class GetInputTask extends AsyncTask<Void, Void, StreamWithFileNameHint> {
        private final Intent mIntent;
        public GetInputTask(Intent intent) {
            this.mIntent = intent;
        }

        @Override
        protected StreamWithFileNameHint doInBackground(Void... params) {
            StreamWithFileNameHint input = getGpxStream(mIntent);
            return input;
        }

        @Override
        protected void onPostExecute(StreamWithFileNameHint input) {
            onInput(input);
        }
    }

    public void onInput(StreamWithFileNameHint input) {
        mInput = input;
        FragmentManager fm = getFragmentManager();

        FilenameDialogFragment dialog = FilenameDialogFragment.newInstance(mInput.filenameHint);
        dialog.show(fm, "name_track_dialog");
    }

    @Override
    public void onOK(String filename) {
        new CopyFileTask(mInput.stream).execute(filename);
    }

    @Override
    public void onCancel() {
        finish();
    }

    class CopyFileTask extends AsyncTask<String, Void, File> {
        private final InputStream mStream;
        public CopyFileTask(InputStream stream) {
            this.mStream = stream;
        }

        @Override
        protected File doInBackground(String... params) {
            final String filename = params[0];

            final File outFile = new File(Utils.getStorageDirectory(), filename + ".gpx");
            try {
                OutputStream outStream =new FileOutputStream(outFile);
                ByteStreams.copy(mStream, outStream);
                return outFile;
            } catch (IOException e) {
                Log.e(TAG, "Error copying file", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File localFile) {
            if (localFile == null) {
                Utils.showMessage(ImportTrackActivity.this, "Error importing track");
                finish();
            } else {
                new ImportGpxTask().execute(localFile);
            }
        }

    }

    class ImportGpxTask extends AsyncTask<File, Void, Track> {
        @Override
        protected Track doInBackground(File... params) {
            final File gpxFile = params[0];
            try {
                final Track track = GpxLoader.Load(gpxFile);
                return track;
            } catch (GpxLoader.GpxException e) {
                Log.e(TAG, "Error loading track", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Track track) {
            onTrackLoaded(track);
        }
    }

    private void onTrackLoaded(Track track) {
        if (track == null) {
            Utils.showMessage(ImportTrackActivity.this, "Error importing track");
            finish();
        } else {
            mGlobals.setCurrentTrack(track);

            Log.i(TAG, "Track loaded");
            final Intent intent = new Intent(this, TrackViewActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Utility class to associate a filename hint with a stream
    public static class StreamWithFileNameHint {
        public final InputStream stream;
        public final String filenameHint;

        public StreamWithFileNameHint(InputStream s, String n) {
            stream = s;
            filenameHint = n;
        }
    }

    // Given a SEND, VIEW or ATTACH_DATA intent that contains either the path to a GPX file or
    // a link to the file content in a TEXT extra (like when using Dropbox's share feature),
    // Returns a BufferedReader to read the content or null if something goes wrong
    private static StreamWithFileNameHint getGpxStream(Intent intent) {
        if (intent == null) {
            return null;
        }
        final String action = intent.getAction();
        final String type = intent.getType();

        if (!(Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_ATTACH_DATA.equals(action)
                || Intent.ACTION_SEND.equals(action))) {
            Log.e(TAG, "Unrecognized action : " + action);
            return null;
        }
        if (type == null) {
            Log.e(TAG, "type is null");
            return null;
        }

        final Uri uri = intent.getData();
        if (uri == null) {
            // Some ACTION_SEND intent won't have an URI, but the file content in a TEXT extra
            if (!intent.hasExtra(Intent.EXTRA_TEXT)) {
                Log.e(TAG, "Intent has no EXTRA_TEXT");
                for (String key : intent.getExtras().keySet()) {
                    Log.i(TAG, "Available extra key : " + key);
                }
                return null;
            }
            final String urltext = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString();
            Log.i(TAG, "Url : " + urltext);
            try {
                final URL url = new URL(urltext);
                return new StreamWithFileNameHint(url.openStream(), url.getFile());
            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid URL : " + urltext, e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Error reading URL : " + urltext, e);
                return null;
            }
        } else {
            if (!uri.getScheme().equals("file")) {
                Log.e(TAG, "Received a non-file uri : " + uri + ", ignoring");
                return null;
            }
            final String filename = uri.getPath();
            Log.i(TAG, "Importing " + filename);
            try {
                final File f = new File(filename);
                return new StreamWithFileNameHint(new FileInputStream(f), f.getName());
            } catch (IOException e) {
                Log.e(TAG, "Failed loading file : " + filename, e);
                return null;
            }
        }
    }
}

