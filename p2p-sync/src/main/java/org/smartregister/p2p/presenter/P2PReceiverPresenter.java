package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.ReceiverConnectionAuthenticator;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.util.Constants;

import java.util.List;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 18/03/2019
 */

public class P2PReceiverPresenter extends BaseP2pModeSelectPresenter implements P2pModeSelectContract.ReceiverPresenter
        , IReceiverSyncLifecycleCallback {

    @Nullable
    private DiscoveredDevice currentSender;

    public P2PReceiverPresenter(@NonNull P2pModeSelectContract.View view) {
        super(view);
    }

    @VisibleForTesting
    public P2PReceiverPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        super(view, p2pModeSelectInteractor);
    }

    @Override
    public void onReceiveButtonClicked() {
        prepareForAdvertising(false);
    }

    @Override
    public void prepareForAdvertising(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startAdvertisingMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startAdvertisingMode();
                    }
                });
            }
        } else {
            if (!returningFromRequestingPermissions) {
                view.addOnActivityRequestPermissionHandler(new OnActivityRequestPermissionHandler() {
                    @Override
                    public int getRequestCode() {
                        return Constants.RQ_CODE.PERMISSIONS;
                    }

                    @Override
                    public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                        view.removeOnActivityRequestPermissionHandler(this);
                        prepareForAdvertising(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startAdvertisingMode() {
        if (!interactor.isAdvertising()) {
            view.enableSendReceiveButtons(false);
            view.showReceiveProgressDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    interactor.stopAdvertising();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startAdvertising(this);
        }
    }

    @Override
    public void onStartedAdvertising(Object result) {
        // Do nothing for now
        // Continue showing the dialog box
    }

    @Override
    public void onAdvertisingFailed(@NonNull Exception e) {
        view.showToast(view.getString(R.string.an_error_occurred_start_receiving), Toast.LENGTH_LONG);
        view.removeReceiveProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Timber.i(view.getString(R.string.log_connection_initiated_endpoint_auth_code), endpointId, connectionInfo.getEndpointName()
                , connectionInfo.getAuthenticationToken());

        if (currentSender == null) {
            currentSender = new DiscoveredDevice(endpointId, connectionInfo);

            // First stop advertising
            interactor.stopAdvertising();
            view.removeReceiveProgressDialog();

            // This can be moved to the library for easy customisation by host applications
            BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new ReceiverConnectionAuthenticator(this);
            syncConnectionAuthenticator.authenticate(currentSender, this);
        } else {
            Timber.e(view.getString(R.string.log_ignoring_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
            // We can add connection support for multiple devices here later
        }
    }

    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        view.showToast(String.format(view.getString(R.string.you_are_connected_to_sender), currentSender.getEndpointName())
                , Toast.LENGTH_LONG);
        view.displayMessage(view.getString(R.string.connected));
        interactor.connectedTo(endpointId);
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        view.showToast(view.getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
        resetState();
        startAdvertisingMode();
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to advertising mode
        //Todo: And show the user an error
        view.showToast(view.getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
        resetState();
        startAdvertisingMode();
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to advertising mode
        resetState();
        view.showToast(String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId), Toast.LENGTH_LONG);
        startAdvertisingMode();
    }

    @Override
    public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            // Show a simple message of the text sent
            String message = new String(payload.asBytes());
            view.showToast(message, Toast.LENGTH_LONG);
            view.displayMessage(String.format(view.getString(R.string.chat_message_format),endpointId, message));
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        Timber.e(view.getString(R.string.log_endpoint_lost), endpointId);
        resetState();
        startAdvertisingMode();
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (currentSender != null) {
            interactor.acceptConnection(currentSender.getEndpointId(), new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    P2PReceiverPresenter.this.onPayloadReceived(endpointId, payload);
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
        if (currentSender != null) {
            interactor.rejectConnection(currentSender.getEndpointId());
        }

        view.showToast(view.getString(R.string.authentication_failed_connection_rejected), Toast.LENGTH_LONG);

        //Todo: Go back to advertising mode
        Timber.e(exception, view.getString(R.string.authentication_failed));
        // The rest will be handled in the rejectConnection callback
        // Todo: test is this is causing an error where the discovering mode can no longer be restarted
        // if the receiving device app is either removed or discovering cancelled while the receiver is showing
        // the QR code dialog
    }

    @Override
    public void onAuthenticationCancelled(@NonNull String reason) {
        // Reject the connection
        if (currentSender != null) {
            interactor.rejectConnection(currentSender.getEndpointId());
        }

        // Go back to discovering mode
        Timber.e(view.getString(R.string.log_authentication_cancelled), reason);
    }

    private void resetState() {
        view.dismissAllDialogs();
        currentSender = null;
    }
}
