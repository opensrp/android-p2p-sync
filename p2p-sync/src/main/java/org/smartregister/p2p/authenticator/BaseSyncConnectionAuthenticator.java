package org.smartregister.p2p.authenticator;

import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.DiscoveredDevice;

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

    public abstract void authenticate(@NonNull DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback);

    public interface AuthenticationCallback {

        void onAuthenticationSuccessful();

        void onAuthenticationFailed(@NonNull Exception exception);

        void onAuthenticationCancelled(@NonNull String reason);
    }
}
