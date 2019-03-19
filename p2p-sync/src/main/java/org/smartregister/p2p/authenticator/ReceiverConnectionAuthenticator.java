package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeGeneratorDialog;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class ReceiverConnectionAuthenticator extends BaseSyncConnectionAuthenticator {

    public ReceiverConnectionAuthenticator(@NonNull P2pModeSelectContract.View view
            , @NonNull P2pModeSelectContract.Interactor interactor
            , @NonNull P2pModeSelectContract.Presenter presenter) {
        super(view, interactor, presenter);
    }

    @Override
    public void authenticate(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        if (discoveredDevice.getConnectionInfo() != null
                && discoveredDevice.getConnectionInfo().isIncomingConnection()) {

            view.showQRCodeGeneratorDialog(discoveredDevice.getConnectionInfo().getAuthenticationToken()
                    , discoveredDevice.getEndpointName()
                    , new QRCodeGeneratorDialog.QRCodeAuthenticationCallback() {
                @Override
                public void onAccepted(@NonNull DialogInterface dialogInterface) {
                    authenticationCallback.onAuthenticationSuccessful();
                }

                @Override
                public void onRejected(@NonNull DialogInterface dialogInterface) {
                    authenticationCallback.onAuthenticationFailed(new Exception("User rejected the connection"));
                }
            });

        } else {
            authenticationCallback.onAuthenticationFailed(new Exception("DiscoveredDevice information passed is invalid"));
        }
    }
}
