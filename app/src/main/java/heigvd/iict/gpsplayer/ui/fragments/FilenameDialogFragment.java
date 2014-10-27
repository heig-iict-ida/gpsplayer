package heigvd.iict.gpsplayer.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.EditText;

import java.io.File;

import heigvd.iict.gpsplayer.R;

// A dialog that ask the user to give a filename, limiting the available characters
public class FilenameDialogFragment extends DialogFragment {
    public static String ARG_TRACK_NAME;

    public static FilenameDialogFragment newInstance(String defaultTrackName) {
        FilenameDialogFragment f = new FilenameDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRACK_NAME, defaultTrackName);
        f.setArguments(args);
        return f;
    }


    private DialogListener mListener;
    private String mTrackName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTrackName = getArguments() != null ? getArguments().getString(ARG_TRACK_NAME, "") : "";
        // Clean filename to keep only basename
        final int lastSlash = mTrackName.lastIndexOf(File.separatorChar);
        if (lastSlash != -1) {
            mTrackName = mTrackName.substring(lastSlash);
        }
        if (mTrackName.endsWith(".gpx")) {
            mTrackName = mTrackName.substring(0, mTrackName.length() - 4);
        }
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


    class LowercaseASCIIFilter implements InputFilter {
        // Getting InputFilter to work on all android version seems messy. So only modify the
        // isCharAllowed method
        // http://stackoverflow.com/questions/3349121/how-do-i-use-inputfilter-to-limit-characters-in-an-edittext-in-android
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            boolean keepOriginal = true;
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (isCharAllowed(c)) // put your condition here
                    sb.append(c);
                else
                    keepOriginal = false;
            }
            if (keepOriginal)
                return null;
            else {
                if (source instanceof Spanned) {
                    SpannableString sp = new SpannableString(sb);
                    TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                    return sp;
                } else {
                    return sb;
                }
            }
        }

        private boolean isCharAllowed(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-';
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setText(mTrackName);
        input.setFilters(new InputFilter[]{new LowercaseASCIIFilter()});
        builder.setTitle(R.string.name_track_dialog_title)
                .setMessage(R.string.name_track_dialog_message)
                .setView(input)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onOK(input.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onCancel();
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface DialogListener {
        public void onOK(String trackName);
        public void onCancel();
    }
}