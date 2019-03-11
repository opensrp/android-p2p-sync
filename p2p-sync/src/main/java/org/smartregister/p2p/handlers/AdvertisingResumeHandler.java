package org.smartregister.p2p.handlers;

import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public class AdvertisingResumeHandler implements OnResumeHandler {

    private P2pModeSelectContract.Presenter presenter;
    private P2pModeSelectContract.Interactor interactor;


    public AdvertisingResumeHandler(@NonNull P2pModeSelectContract.Presenter presenter, P2pModeSelectContract.Interactor interactor) {
        this.presenter = presenter;
        this.interactor = interactor;
    }


    @Override
    public void onResume() {
        if (interactor.isAdvertising()) {
            presenter.prepareForAdvertising(false);
        }
    }
}
