package org.smartregister.p2p.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.StartReceiveModeProgressDialog;
import org.smartregister.p2p.presenter.P2pModeSelectPresenter;

public class P2pModeSelectActivity extends AppCompatActivity implements P2pModeSelectContract.View {

    private Button sendButton;
    private Button receiveButton;

    private P2pModeSelectContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p_mode_select);

        sendButton = findViewById(R.id.btn_p2pModeSelectActivity_send);
        receiveButton = findViewById(R.id.btn_p2pModeSelectActivity_receive);

        initializePresenter();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onSendButtonClicked();
            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onReceiveButtonClicked();
            }
        });
    }

    @Override
    public void enableSendReceiveButtons(boolean enable) {
        sendButton.setEnabled(enable);
        receiveButton.setEnabled(enable);
    }

    @Override
    public void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartReceiveModeProgressDialog newFragment = new StartReceiveModeProgressDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, "dialog_receive_progress");
    }

    @NonNull
    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void initializePresenter() {
        presenter = new P2pModeSelectPresenter(this);
    }
}
