package heigvd.ch.gpsplayer.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FilenameFilter;

import heigvd.ch.gpsplayer.Globals;
import heigvd.ch.gpsplayer.io.GpxLoader;
import heigvd.ch.gpsplayer.R;
import heigvd.ch.gpsplayer.Utils;
import heigvd.ch.gpsplayer.data.Track;


public class FilesListActivity extends ListActivity {
    private final static String TAG = "FilesListActivity";

    private static File[] listAvailableFiles() {
        // Ensure directory exists
        File dir = Environment.getExternalStoragePublicDirectory("gpsplayer");
        Log.i(TAG, "Storage directory : " + dir);
        if (!dir.exists()) {
            dir.mkdirs();
            Log.i(TAG, "Created directory : " + dir);
        }

        File[] gpxFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(".gpx");
            }
        });
        return gpxFiles;
    }

    private File[] gpxFiles;
    private String[] filenames;

    private View mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list);

        final ListView lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Click on " + filenames[position]);
                // TODO: Avoid reloading if this is the same track
                new LoadTrackTask().execute(gpxFiles[position]);
            }
        });

        mProgressView = findViewById(R.id.progress);
    }

    private void refreshFiles() {
        gpxFiles = listAvailableFiles();
        filenames = new String[gpxFiles.length];
        for (int i = 0; i < gpxFiles.length; ++i) {
            filenames[i] = gpxFiles[i].getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.activity_files_list_row, R.id.row_title, filenames);
        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFiles();
    }

    // A task that load a GPX file and replace the current track by the newly loaded track
    public class LoadTrackTask extends AsyncTask<File, Void, Track> {
        @Override
        protected void onPreExecute() {
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Track doInBackground(File... params) {
            File file = params[0];
            try {
                return GpxLoader.Load(file);
            } catch (GpxLoader.GpxException e) {
                Log.e(TAG, "Error loading GPX file", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Track track) {
            mProgressView.setVisibility(View.INVISIBLE);

            if (track == null) {
                Utils.showMessage(FilesListActivity.this, "Error loading file");
            } else {
                Globals.getInstance(FilesListActivity.this).setCurrentTrack(track);

                Log.i(TAG, "Track loaded");
                final Intent intent = new Intent(FilesListActivity.this, TrackViewActivity.class);
                startActivity(intent);
            }
        }
    }
}
