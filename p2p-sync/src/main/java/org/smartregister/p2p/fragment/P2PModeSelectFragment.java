package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.smartregister.p2p.R;
import org.smartregister.p2p.activity.P2pModeSelectActivity;
import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 03/05/2019
 */

public class P2PModeSelectFragment extends Fragment implements P2pModeSelectContract.P2PModeSelectView {

    private Button sendButton;
    private Button receiveButton;

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mode_select, container, false);

        sendButton = view.findViewById(R.id.btn_p2pModeSelectActivity_send);
        receiveButton = view.findViewById(R.id.btn_p2pModeSelectActivity_receive);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof P2pModeSelectActivity) {
                    ((P2pModeSelectActivity) getActivity()).getSenderBasePresenter().onSendButtonClicked();
                }
            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof P2pModeSelectActivity) {
                    ((P2pModeSelectActivity) getActivity()).getReceiverBasePresenter().onReceiveButtonClicked();
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        sendButton.setOnClickListener(null);
        receiveButton.setOnClickListener(null);
    }

    @Override
    public void enableSendReceiveButtons(boolean enable) {
        sendButton.setEnabled(enable);
        receiveButton.setEnabled(enable);
    }
}
