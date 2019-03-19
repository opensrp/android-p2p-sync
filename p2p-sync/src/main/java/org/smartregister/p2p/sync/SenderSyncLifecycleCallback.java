package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.smartregister.p2p.R;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.SenderConnectionAuthenticator;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public class SenderSyncLifecycleCallback implements ISenderSyncLifecycleCallback {

    private P2pModeSelectContract.View view;
    private P2pModeSelectContract.Presenter presenter;
    private P2pModeSelectContract.Interactor interactor;

    @Nullable
    private DiscoveredDevice currentReceiver;

    public SenderSyncLifecycleCallback(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Presenter presenter
            , @NonNull P2pModeSelectContract.Interactor interactor) {
        this.view = view;
        this.presenter = presenter;
        this.interactor = interactor;
    }

    @Override
    public void onStartedDiscovering(@NonNull Object object) {
        // Do nothing here for now
        // Continue showing the progress dialog
    }

    @Override
    public void onDiscoveringFailed(@NonNull Exception exception) {
        view.showToast(view.getContext().getString(R.string.error_occurred_cannot_start_sending), Toast.LENGTH_LONG);
        view.removeDiscoveringProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
        Timber.i("Endpoint found : %s   Endpoint info: (%s, %s)", endpointId, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());

        if (currentReceiver == null) {
            currentReceiver = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

            // First stop discovering
            interactor.stopDiscovering();
            view.removeDiscoveringProgressDialog();

            interactor.requestConnection(endpointId, new OnResultCallback() {
                @Override
                public void onSuccess(@Nullable Object object) {
                    onRequestConnectionSuccessful(object);
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    onRequestConnectionFailed(e);
                }
            }, new org.smartregister.p2p.sync.SyncConnectionLifecycleCallback(this));
        }
    }

    @Override
    public void onRequestConnectionSuccessful(@Nullable Object result) {
        // Just show a success
        view.showToast("CONNECTION REQUEST WAS SUCCESSFUL", Toast.LENGTH_LONG);

    }

    @Override
    public void onRequestConnectionFailed(@NonNull Exception exception) {
        // Show the user an error trying to connect device XYZ
        view.showToast("COULD NOT INITIATE CONNECTION REQUEST TO THE DEVICE", Toast.LENGTH_LONG);
        resetState();
        presenter.startDiscoveringMode();
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        // Easier working with the device which we requested connection to, otherwise the callback for error should be called
        // so that we can work with other devices
        if (currentReceiver != null && currentReceiver.getEndpointId().equals(endpointId)) {
            currentReceiver.setConnectionInfo(connectionInfo);

            // This can be moved to the library for easy customisation by host applications
            BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new SenderConnectionAuthenticator(view, interactor, presenter);
            syncConnectionAuthenticator.authenticate(currentReceiver, this);
        } else {
            //("Connection was initiated by other device");
            Timber.e("Ignoring connection initiated by the other device %s, %s, %s"
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
        }

        // We can add connection support for multiple devices here later
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (currentReceiver != null){
            view.showToast("Authentication successful! Receiver can accept connection", Toast.LENGTH_LONG);
            interactor.acceptConnection(currentReceiver.getEndpointId(), new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    // Do nothing for now
                    if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                        // Show a simple message of the text sent
                        String message = new String(payload.asBytes());
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

    @Override
    public void onAuthenticationFailed(@NonNull Exception exception) {
        // Reject the connection
        if (currentReceiver != null) {
            interactor.rejectConnection(currentReceiver.getEndpointId());
        }

        view.showToast(view.getContext().getString(R.string.authentication_failed_connection_rejected), Toast.LENGTH_LONG);

        //Todo: Go back to discovering mode
        Timber.e(exception, "Authentication failed");
        // The rest will be handled in the rejectConnection callback
        // Todo: test is this is causing an error where the discovering mode can no longer be restarted
        // if the receiving device app is either removed or advertising cancelled while the sender
        // app is showing the QR code scanning dialog
    }

    @Override
    public void onAuthenticationCancelled(@NonNull String reason) {
        // Reject the connection
        if (currentReceiver != null) {
            interactor.rejectConnection(currentReceiver.getEndpointId());
        }

        // Go back to discovering mode
        Timber.e("Authentication cancelled : %s", reason);
    }


    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentReceiver != null) {
            view.showToast(String.format(view.getContext().getString(R.string.you_are_connected_to_receiver), currentReceiver.getEndpointName())
                    , Toast.LENGTH_LONG);
            view.displayMessage("CONNECTED");
            interactor.connectedTo(endpointId);
        }
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        view.showToast(view.getContext().getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
        resetState();
        presenter.startDiscoveringMode();
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to discovering mode
        //Todo: And show the user an error
        view.showToast(view.getContext().getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
        resetState();
        presenter.startDiscoveringMode();
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to discovering mode
        resetState();
        view.showToast(String.format("The connection to %s has broken", endpointId), Toast.LENGTH_LONG);
        presenter.startDiscoveringMode();
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        // Do nothing for now
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        Timber.e("Endpoint lost %s", endpointId);
        view.displayMessage("DISCONNECTED");
        resetState();
        presenter.startDiscoveringMode();
    }

    private void resetState() {
        view.dismissAllDialogs();
        currentReceiver = null;

    }

}
