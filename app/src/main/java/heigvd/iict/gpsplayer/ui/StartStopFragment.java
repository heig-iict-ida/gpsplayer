package heigvd.iict.gpsplayer.ui;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import heigvd.iict.gpsplayer.Globals;
import heigvd.iict.gpsplayer.R;
import heigvd.iict.gpsplayer.events.ServiceStateChangedEvent;


// Fragment containing start/stop/reset controls
public class StartStopFragment extends Fragment {
    private final static String TAG = "StartStopFragment";

    private Globals mGlobals;
    private Button mStartBtn;
    private Button mStopBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_start_stop, container, false);

        mStartBtn = (Button)v.findViewById(R.id.btn_start);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Start clicked");
                mGlobals.startService();
                refreshButtons();
            }
        });

        mStopBtn = (Button)v.findViewById(R.id.btn_stop);
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Stop clicked");
                mGlobals.stopService();
                refreshButtons();
            }
        });

        return v;
    }

    public void onEvent(final ServiceStateChangedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               refreshButtons();
            }
        });
    }

    private void refreshButtons() {
        final boolean running = mGlobals.isRunning();
        mStartBtn.setEnabled(!running);
        mStopBtn.setEnabled(running);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshButtons();
        mGlobals.eventBus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mGlobals.eventBus.unregister(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mGlobals = Globals.getInstance(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
