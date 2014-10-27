package heigvd.iict.gpsplayer.ui.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.File;

import heigvd.iict.gpsplayer.R;

public class DeleteTrackDialogFragment extends DialogFragment {
    private final static String ARG_TRACK_FILE = "trackFile";

    private DialogListener mListener;

    public static DeleteTrackDialogFragment newInstance(File trackFile) {
        DeleteTrackDialogFragment dialog = new DeleteTrackDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRACK_FILE, trackFile);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            mListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File trackFile = (File)getArguments().getSerializable(ARG_TRACK_FILE);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_delete_track)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        trackFile.delete();
                        mListener.onDeleted(trackFile);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface DialogListener {
        public void onDeleted(File f);
    }
}