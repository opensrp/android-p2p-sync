package org.smartregister.p2p.presenter;

import android.support.annotation.NonNull;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.interactor.P2pModeSelectInteractor;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public abstract class BaseP2pModeSelectPresenter implements P2pModeSelectContract.BasePresenter {

    protected P2pModeSelectContract.View view;
    protected P2pModeSelectContract.Interactor interactor;

    protected HashMap<String, Long> rejectedDevices = new HashMap<>();
    protected HashSet<String> blacklistedDevices = new HashSet<>();

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

        if (getCurrentPeerDevice() != null) {
            interactor.disconnectFromEndpoint(getCurrentPeerDevice().getEndpointId());
        }

        setCurrentDevice(null);

        interactor.cleanupResources();
        interactor = null;
    }

    @NonNull
    @Override
    public P2pModeSelectContract.View getView() {
        return view;
    }


    @Override
    public void addDeviceToRejectedList(@NonNull String endpointId) {
        rejectedDevices.put(endpointId, System.currentTimeMillis());
    }

    @Override
    public boolean addDeviceToBlacklist(@NonNull String endpointId) {
        rejectedDevices.remove(endpointId);
        if (blacklistedDevices.contains(endpointId)) {
            return false;
        } else {
            blacklistedDevices.add(endpointId);
            return true;
        }
    }

    @Override
    public void rejectDeviceOnAuthentication(@NonNull String endpointId) {
        Long rejectionTime = rejectedDevices.get(endpointId);
        if (rejectionTime != null) {
            if ((System.currentTimeMillis() - rejectionTime)/1e3 > P2PLibrary.getInstance().getDeviceMaxRetryConnectionDuration()) {
                addDeviceToRejectedList(endpointId);
            } else {
                addDeviceToBlacklist(endpointId);
            }
        } else {
            addDeviceToRejectedList(endpointId);
        }
    }
}
