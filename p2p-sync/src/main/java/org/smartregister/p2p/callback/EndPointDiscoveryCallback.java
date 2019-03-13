package org.smartregister.p2p.callback;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import org.smartregister.p2p.interactor.P2pModeSelectInteractor;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 12/03/2019
 */

public class EndPointDiscoveryCallback extends EndpointDiscoveryCallback {

    // This should be removed
    private P2pModeSelectInteractor interactor;

    public EndPointDiscoveryCallback(@NonNull P2pModeSelectInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        String message = String.format("Endpoint found : %s   Endpoint info: (%s, %s)", s, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());
        Timber.i(message);
        interactor.showToast(message);
    }

    @Override
    public void onEndpointLost(@NonNull String s) {
        String message = String.format("Endpoint lost %s", s);
        Timber.i(message);
        interactor.showToast(message);
    }
}
