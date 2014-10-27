package heigvd.iict.gpsplayer.ui;

import android.app.FragmentManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import heigvd.iict.gpsplayer.Globals;
import heigvd.iict.gpsplayer.io.GpxLoader;
import heigvd.iict.gpsplayer.R;
import heigvd.iict.gpsplayer.Utils;
import heigvd.iict.gpsplayer.data.Track;
import heigvd.iict.gpsplayer.ui.fragments.DeleteTrackDialogFragment;


public class FilesListActivity extends ListActivity implements DeleteTrackDialogFragment.DialogListener {
    private final static String TAG = "FilesListActivity";

    private static File[] listAvailableFiles() {
        // Ensure directory exists
        File dir = Utils.getStorageDirectory();

        File[] gpxFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(".gpx");
            }
        });
        return gpxFiles;
    }

    private File[] gpxFiles;

    private View mProgressView;

    private Globals mGlobals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGlobals = Globals.getInstance(this);
        setContentView(R.layout.activity_files_list);

        final TextView appdirTextView = (TextView)findViewById(R.id.appdir);
        appdirTextView.setText(Utils.getStorageDirectory().getAbsolutePath());

        final ListView lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final File trackFile = gpxFiles[position];
                showTrack(trackFile);
                refreshFiles();
            }
        });

        registerForContextMenu(lv);

        mProgressView = findViewById(R.id.progress);
    }

    private void showTrack(File trackFile) {
        if (!mGlobals.isCurrentTrack(trackFile)) {
            new LoadTrackTask().execute(trackFile);
        } else {
            Log.i(TAG, "Track already loaded, just displaying it");
            final Intent intent = new Intent(FilesListActivity.this, TrackViewActivity.class);
            startActivity(intent);
        }
    }

    private void refreshFiles() {
        gpxFiles = listAvailableFiles();

        List<Map<String, String>> listData = new ArrayList<Map<String, String>>();
        for (File f : gpxFiles) {
            Map<String, String> data = new HashMap<String, String>();
            data.put("title", f.getName());
            if (mGlobals.isCurrentTrack(f)) {
                data.put("icon", Integer.toString(R.drawable.ic_action_play));
            } else {
                data.put("icon", Integer.toString(R.drawable.ic_action_transparent));
            }

            listData.add(data);
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this,
                listData,
                R.layout.activity_files_list_row,
                new String[]{ "title", "icon"},
                new int[]{ R.id.row_title, R.id.row_icon}
        );

        //ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.activity_files_list_row, R.id.row_title, filenames);
        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFiles();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_files_list_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        final int position = info.position;
        final File trackFile = gpxFiles[position];
        switch (item.getItemId()) {
            case R.id.action_show:
                showTrack(trackFile);
                return true;
            case R.id.action_delete:
                FragmentManager fm = getFragmentManager();
                DeleteTrackDialogFragment dialog = DeleteTrackDialogFragment.newInstance(trackFile);
                dialog.show(fm, "delete_track_fragment");
                return true;
        }
        return false;
    }

    @Override
    public void onDeleted(File trackFile) {
        if (mGlobals.isCurrentTrack(trackFile)) {
            mGlobals.stopService();
            mGlobals.setCurrentTrack(null);
        }
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
                mGlobals.setCurrentTrack(track);

                Log.i(TAG, "Track loaded");
                final Intent intent = new Intent(FilesListActivity.this, TrackViewActivity.class);
                startActivity(intent);
            }
        }
    }
}
