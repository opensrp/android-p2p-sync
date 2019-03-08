package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class P2pModeSelectPresenter implements P2pModeSelectContract.Presenter {

    private P2pModeSelectContract.View view;

    public P2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view) {
        this.view = view;
    }

    @Override
    public void onSendButtonClicked() {
        view.enableSendReceiveButtons(false);
    }

    @Override
    public void onReceiveButtonClicked() {
        view.enableSendReceiveButtons(false);
        view.showReceiveProgressDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
            @Override
            public void onCancelClicked(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
                view.enableSendReceiveButtons(true);
            }
        });
    }
}
