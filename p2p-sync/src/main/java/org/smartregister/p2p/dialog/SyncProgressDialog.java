package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.smartregister.p2p.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class SyncProgressDialog extends DialogFragment {

    private DialogInterface dialogInterface;
    private SyncProgressDialogCallback syncProgressDialogCallback;

    private ProgressBar progressBar;
    private TextView progressTextView;
    private TextView summaryTextView;

    private String title;

    public static SyncProgressDialog create(@NonNull String title) {
        SyncProgressDialog syncProgressDialog = new SyncProgressDialog();
        syncProgressDialog.title = title;

        return syncProgressDialog;
    }

    public SyncProgressDialog() {
        dialogInterface = new DialogInterface() {
            @Override
            public void cancel() {
                SyncProgressDialog.this.dismiss();
            }

            @Override
            public void dismiss() {
                SyncProgressDialog.this.dismiss();
            }
        };
    }

    public void setSummaryText(@NonNull String summaryText) {
        if (summaryTextView != null) {
            summaryTextView.setText(summaryText);
        }
    }

    public void setProgressText(@NonNull String progressText) {
        if (progressTextView != null) {
            progressTextView.setText(progressText);
        }
    }

    public void setProgress(int progress) {
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(progress);
        }
    }

    public void setSyncProgressDialogCallback(@NonNull SyncProgressDialogCallback syncProgressDialogCallback) {
        this.syncProgressDialogCallback = syncProgressDialogCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_sync_progress, null))
                .setTitle(title)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (syncProgressDialogCallback != null) {
                            syncProgressDialogCallback.onCancelClicked(dialogInterface);
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        setCancelable(false);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        progressBar = getDialog().findViewById(R.id.pb_syncProgressDialog_progressBar);
        progressTextView = getDialog().findViewById(R.id.tv_syncProgressDialog_progressText);
        summaryTextView = getDialog().findViewById(R.id.tv_syncProgressDialog_summaryText);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public interface SyncProgressDialogCallback {

        void onCancelClicked(@NonNull DialogInterface dialogInterface);
    }
}
