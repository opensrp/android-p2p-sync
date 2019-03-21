package org.smartregister.p2p.presenter;

import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.interactor.P2pModeSelectInteractor;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public abstract class BaseP2pModeSelectPresenter implements P2pModeSelectContract.BasePresenter {

    protected P2pModeSelectContract.View view;
    protected P2pModeSelectContract.Interactor interactor;

    public BaseP2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view) {
        this(view, new P2pModeSelectInteractor(view.getContext()));
    }

    protected BaseP2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        this.view = view;
        this.interactor = p2pModeSelectInteractor;

        // This will be added when issue https://github.com/OpenSRP/android-p2p-sync/issues/24
        // is being worked on
        //view.addOnResumeHandler(new AdvertisingResumeHandler(this, interactor));
    }

    @Override
    public void sendTextMessage(@NonNull String message) {
        interactor.sendMessage(message);
    }

    @Override
    public void onStop() {
        view.dismissAllDialogs();
        view.enableSendReceiveButtons(true);

        interactor.stopAdvertising();
        interactor.stopDiscovering();
        interactor.closeAllEndpoints();

        interactor.cleanupResources();
        interactor = null;
    }

}
