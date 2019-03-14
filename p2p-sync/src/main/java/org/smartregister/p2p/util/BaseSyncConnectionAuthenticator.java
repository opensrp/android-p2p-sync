package org.smartregister.p2p.util;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import org.smartregister.p2p.contract.P2pModeSelectContract;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public abstract class BaseSyncConnectionAuthenticator {

    protected P2pModeSelectContract.View view;
    protected P2pModeSelectContract.Interactor interactor;
    protected P2pModeSelectContract.Presenter presenter;

    public BaseSyncConnectionAuthenticator(@NonNull P2pModeSelectContract.View view
            , @NonNull P2pModeSelectContract.Interactor interactor, @NonNull P2pModeSelectContract.Presenter presenter) {
        this.view = view;
        this.interactor = interactor;
        this.presenter = presenter;
    }

    public P2pModeSelectContract.View getView() {
        return view;
    }

    public P2pModeSelectContract.Interactor getInteractor() {
        return interactor;
    }

    public P2pModeSelectContract.Presenter getPresenter() {
        return presenter;
    }

    public abstract void authenticate(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo
            , @NonNull final ConnectionInfo connectionInfo, @NonNull final AuthenticationListener authenticationListener);

    public interface AuthenticationListener {

        void onSuccess();

        void onFailure(@NonNull String reason);
    }
}
