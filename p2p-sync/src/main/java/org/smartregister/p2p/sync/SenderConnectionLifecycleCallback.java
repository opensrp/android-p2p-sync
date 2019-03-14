package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.util.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.util.SyncConnectionAuthenticator;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class SenderConnectionLifecycleCallback extends ConnectionLifecycleCallback {

    private P2pModeSelectContract.View view;
    private P2pModeSelectContract.Presenter presenter;
    private P2pModeSelectContract.Interactor interactor;
    private DiscoveredEndpointInfo discoveredEndpointInfo;

    public SenderConnectionLifecycleCallback(@NonNull P2pModeSelectContract.View view
            , P2pModeSelectContract.Presenter presenter, @NonNull P2pModeSelectContract.Interactor interactor
            , @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        this.view = view;
        this.presenter = presenter;
        this.interactor = interactor;
        this.discoveredEndpointInfo = discoveredEndpointInfo;
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new SyncConnectionAuthenticator(view, interactor, presenter);
        syncConnectionAuthenticator.authenticate(endpointId, discoveredEndpointInfo, connectionInfo
                , new BaseSyncConnectionAuthenticator.AuthenticationListener() {
                    @Override
                    public void onSuccess() {
                        acceptConnection(endpointId);
                    }

                    @Override
                    public void onFailure(@NonNull String reason) {

                    }
                });
    }

    @Override
    public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        switch(connectionResolution.getStatus().getStatusCode()) {
            case ConnectionsStatusCodes.STATUS_OK:
                // The start of the connection
                view.showToast("You are now connected to the receiver", Toast.LENGTH_LONG);
                view.displayMessage("CONNECTED");
                break;

            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                // The receiver rejected the connection
                view.showToast("The receiver rejected the connection", Toast.LENGTH_LONG);
                presenter.startDiscoveringMode();
                break;

            case ConnectionsStatusCodes.STATUS_ERROR:
                // The connection broke before it was able to be accepted
                presenter.startDiscoveringMode();
                break;
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        // This will not be handled here

    }

    private void acceptConnection(@NonNull String endpointId) {
        interactor.acceptConnection(endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                // Do nothing for now
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    // Show a simple message of the text sent
                    String message = payload.asBytes().toString();
                    view.showToast(message, Toast.LENGTH_LONG);
                    view.displayMessage(endpointId + ": " + message);
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                // Do nothing for now
            }
        });
    }
}
