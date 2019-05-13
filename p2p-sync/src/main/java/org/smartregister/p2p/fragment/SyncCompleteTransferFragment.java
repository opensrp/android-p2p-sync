package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.smartregister.p2p.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 03/05/2019
 */

public class SyncCompleteTransferFragment extends Fragment {

    private OnCloseClickListener onCloseClickListener;
    private String transferSummary;
    private boolean isSuccess;

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public void setTransferSummaryReport(@NonNull String transferSummaryReport) {
        this.transferSummary = transferSummaryReport;
    }

    public void setOnCloseClickListener(@Nullable OnCloseClickListener onCloseClickListener) {
        this.onCloseClickListener = onCloseClickListener;
    }

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_successful_transfer, container, false);
        ((TextView) view.findViewById(R.id.tv_successfulTransferFragment_transferSummary))
                .setText(transferSummary);

        if (!isSuccess) {
            ImageView imageView = view.findViewById(R.id.iv_successfulTransferFragment_successMark);
            imageView.setImageResource(R.drawable.ic_fail);

            ((TextView) view.findViewById(R.id.tv_successfulTransferFragment_transferSuccessText))
                    .setText(R.string.transfer_failed);
        }

        Button closeBtn = view.findViewById(R.id.btn_successfulTransferFragment_closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onCloseClickListener != null) {
                    onCloseClickListener.onCloseClicked();
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public interface OnCloseClickListener {

        void onCloseClicked();

    }
}
