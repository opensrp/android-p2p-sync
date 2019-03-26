package org.smartregister.p2p.presenter;

import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.callback.ConnectionLevelChangeCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.interactor.P2pModeSelectInteractor;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.SyncLifecycleCallback;

import java.util.Map;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public abstract class BaseP2pModeSelectPresenter implements P2pModeSelectContract.BasePresenter
        , SyncLifecycleCallback {

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
    public long sendTextMessage(@NonNull String message) {
        if (getCurrentPeerDevice() != null) {
            return interactor.sendMessage(message);
        }

        return 0;
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

    @NonNull
    @Override
    public P2pModeSelectContract.View getView() {
        return view;
    }

}
