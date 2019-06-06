package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.SenderConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.callback.SyncFinishedCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.SyncProgressFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.sync.handler.SyncSenderHandler;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.SyncDataConverterUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public class P2PSenderPresenter extends BaseP2pModeSelectPresenter implements ISenderSyncLifecycleCallback, P2pModeSelectContract.SenderPresenter {

    @Nullable
    private DiscoveredDevice currentReceiver;
    private ConnectionLevel connectionLevel;
    private long hashKeyPayloadId;
    private long connectionSignalPayloadId;

    private HashMap<String, Integer> transferItems;

    @Nullable
    private SyncSenderHandler syncSenderHandler;

    public P2PSenderPresenter(@NonNull P2pModeSelectContract.View view) {
        super(view);
    }

    @VisibleForTesting
    public P2PSenderPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        super(view, p2pModeSelectInteractor);
    }

    @Override
    public void onSendButtonClicked() {
        prepareForDiscovering(false);
    }

    @Override
    public void prepareForDiscovering(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startDiscoveringMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startDiscoveringMode();
                    }
                });
            }
        } else {
            if (!returningFromRequestingPermissions) {
                view.addOnActivityRequestPermissionHandler(new OnActivityRequestPermissionHandler() {
                    @Override
                    public int getRequestCode() {
                        return Constants.RqCode.PERMISSIONS;
                    }

                    @Override
                    public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                        view.removeOnActivityRequestPermissionHandler(this);
                        prepareForDiscovering(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startDiscoveringMode() {
        if (!interactor.isDiscovering()) {
            view.enableSendReceiveButtons(false);
            keepScreenOn(true);
            view.showDiscoveringProgressDialog (new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    keepScreenOn(false);
                    interactor.stopDiscovering();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startDiscovering(this);
        }
    }

    @Override
    public void sendSyncComplete() {
        // Do nothing for now
        transferItems = (HashMap<String, Integer>) syncSenderHandler.getTransferProgress().clone();

        SyncFinishedCallback syncFinishedCallback = P2PLibrary.getInstance().getSyncFinishedCallback();
        if (syncFinishedCallback != null) {
            syncFinishedCallback.onSuccess(transferItems);
        }

        syncSenderHandler = null;

        // incase the other side has hung at some point
        connectionSignalPayloadId = interactor.sendMessage(Constants.Connection.SYNC_COMPLETE);
    }

    @Override
    public long sendManifest(@NonNull SyncPackageManifest syncPackageManifest) {
        if (getCurrentPeerDevice() != null) {
            return interactor.sendMessage(new Gson().toJson(syncPackageManifest));
        }

        return 0;
    }

    @Override
    public void sendPayload(@NonNull Payload payload) {
        if (getCurrentPeerDevice() != null) {
            interactor.sendPayload(payload);
        }
    }

    @Override
    public void errorOccurredSync(@NonNull Exception e) {
        Timber.e(e);

        SyncFinishedCallback syncFinishedCallback = P2PLibrary.getInstance().getSyncFinishedCallback();
        if (syncFinishedCallback != null) {
            syncFinishedCallback.onFailure(e, transferItems);
        }

        if (syncSenderHandler != null) {
            getView().showSyncCompleteFragment(false, new SyncCompleteTransferFragment.OnCloseClickListener() {
                @Override
                public void onCloseClicked() {
                    getView().showP2PModeSelectFragment();
                }
            }, SyncDataConverterUtil.generateSummaryReport(getView().getContext(), syncSenderHandler.getTransferProgress()));

            syncSenderHandler = null;
        }

        if (getCurrentPeerDevice() != null) {
            interactor.disconnectFromEndpoint(getCurrentPeerDevice().getEndpointId());
            resetState();
        }
    }

    @Override
    public void onStartedDiscovering(@NonNull Object object) {
        // Do nothing here for now
        // Continue showing the progress dialog
    }

    @Override
    public void onDiscoveringFailed(@NonNull Exception exception) {
        view.showToast(view.getString(R.string.error_occurred_cannot_start_sending), Toast.LENGTH_LONG);
        view.removeDiscoveringProgressDialog();
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
        Timber.i(view.getString(R.string.log_endpoint_found)
                , endpointId, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());

        // Reject when already connected or the connecting device is blacklisted
        if (currentReceiver == null && !blacklistedDevices.contains(endpointId)) {
            setCurrentDevice(new DiscoveredDevice(endpointId, discoveredEndpointInfo));
            getCurrentPeerDevice().setUsername(discoveredEndpointInfo.getEndpointName());

            // First stop discovering
            keepScreenOn(false);
            interactor.stopDiscovering();

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
        // We ignore blacklisted devices and do not request a connection with them
    }

    @Override
    public void onRequestConnectionSuccessful(@Nullable Object result) {
        // Just show a success
        view.showToast(view.getString(R.string.connection_request_successful), Toast.LENGTH_LONG);
        view.removeDiscoveringProgressDialog();
    }

    @Override
    public void onRequestConnectionFailed(@NonNull Exception exception) {
        // Show the user an error trying to connect device XYZ
        view.showToast(view.getString(R.string.could_not_initiate_connection_request_to_device), Toast.LENGTH_LONG);
        resetState();
        prepareForDiscovering(false);
    }

    @Override
    public void processReceivedHistory(@NonNull final String endpointId, @NonNull Payload payload) {
        if (currentReceiver != null) {
            connectionLevel = ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY;

            if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                try {
                    Type receivedHistoryListType = new TypeToken<ArrayList<P2pReceivedHistory>>() {}.getType();
                    final List<P2pReceivedHistory> receivedHistory = new Gson().fromJson(new String(payload.asBytes()), receivedHistoryListType);

                    Tasker.run(new Callable<TreeSet<DataType>>() {
                        @Override
                        public TreeSet<DataType> call() throws Exception {
                            return P2PLibrary.getInstance().getSenderTransferDao()
                                    .getDataTypes();
                        }
                    }, new GenericAsyncTask.OnFinishedCallback<TreeSet<DataType>>() {
                        @Override
                        public void onSuccess(@Nullable TreeSet<DataType> result) {
                            if (result != null) {
                                syncSenderHandler = new SyncSenderHandler(P2PSenderPresenter.this, result, receivedHistory);
                                syncSenderHandler.startSyncProcess();
                            } else {
                                sendSyncComplete();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.e(e);
                            disconnectAndReset(endpointId);
                        }
                    });
                } catch (JsonParseException ex) {
                    Timber.e(ex, view.getString(R.string.log_jsonparse_exception_trying_to_process_received_history));
                    disconnectAndReset(endpointId);
                }
            }
        }
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        // Easier working with the device which we requested connection to, otherwise the callback for error should be called
        // so that we can work with other devices
        if (currentReceiver != null && currentReceiver.getEndpointId().equals(endpointId)) {
            currentReceiver.setConnectionInfo(connectionInfo);

            // This can be moved to the library for easy customisation by host applications
            BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new SenderConnectionAuthenticator(this);
            syncConnectionAuthenticator.authenticate(currentReceiver, this);
        } else {
            //("Connection was initiated by other device");
            Timber.e(view.getString(R.string.log_rejecting_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
        }

        // We can add connection support for multiple devices here later
    }

    @Override
    public void onAuthenticationSuccessful() {
        if (currentReceiver != null){
            view.showToast(view.getString(R.string.authentication_successful_receiver_can_accept_connection), Toast.LENGTH_LONG);

            interactor.acceptConnection(currentReceiver.getEndpointId(), new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    P2PSenderPresenter.this.onPayloadReceived(endpointId, payload);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    // Do nothing for now
                    P2PSenderPresenter.this.onPayloadTransferUpdate(s, payloadTransferUpdate);
                }
            });
        } else {
            Timber.e(view.getString(R.string.log_onauthenticationsuccessful_without_peer_device));
        }
    }

    @Override
    public void onAuthenticationFailed(@NonNull Exception exception) {
        // Reject the connection
        if (currentReceiver != null) {
            String endpointId = currentReceiver.getEndpointId();
            interactor.rejectConnection(endpointId);
        }

        view.showToast(view.getString(R.string.authentication_failed_connection_rejected), Toast.LENGTH_LONG);

        //Todo: Go back to discovering mode
        Timber.e(exception, view.getString(R.string.authentication_failed));
        // The rest will be handled in the rejectConnection callback
        // Todo: test is this is causing an error where the discovering mode can no longer be restarted
        // if the receiving device app is either removed or advertising cancelled while the sender
        // app is showing the QR code scanning dialog
    }

    @Override
    public void onAuthenticationCancelled(@NonNull String reason) {
        // Reject the connection
        if (currentReceiver != null) {
            String endpointId = currentReceiver.getEndpointId();
            interactor.rejectConnection(endpointId);
        } else {
            Timber.e("onAuthenticationCancelled was called and no peer device is connected");
        }

        // Go back to discovering mode
        Timber.e(view.getString(R.string.authentication_cancelled_with_reason), reason);
    }


    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentReceiver != null) {
            connectionLevel = ConnectionLevel.AUTHENTICATED;
            interactor.connectedTo(endpointId);
            P2PAuthorizationService authorizationService = P2PLibrary.getInstance()
                    .getP2PAuthorizationService();
            authorizationService.getAuthorizationDetails(new P2PAuthorizationService.OnAuthorizationDetailsProvidedCallback() {
                @Override
                public void onAuthorizationDetailsProvided(@NonNull Map<String, Object> authorizationDetails) {
                    // Send the authorization details
                    authorizationDetails.put(Constants.AuthorizationKeys.PEER_STATUS, Constants.PeerStatus.SENDER);
                    sendAuthorizationDetails(authorizationDetails);
                }
            });
        } else {
            Timber.e(view.getString(R.string.log_onconnectionaccepted_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        // If my device is rejected by another device
        // I should also reject & start blacklisting the device here
        // so that our device is also able to connect to other devices
        if (currentReceiver != null) {
            rejectDeviceOnAuthentication(endpointId);

            view.showToast(view.getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
            resetState();
            startDiscoveringMode();
        } else {
            Timber.e(view.getString(R.string.log_onconnectionrejected_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to discovering mode
        //Todo: And show the user an error
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            view.showToast(view.getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
            resetState();
            prepareForDiscovering(false);
        } else {
            Timber.e(view.getString(R.string.log_onconnectionunknownerror_without_peer_device), endpointId);
        }
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to discovering mode
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            String errorMsg = String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId);
            errorOccurredSync(new Exception(errorMsg));
            view.showToast(errorMsg, Toast.LENGTH_LONG);
        } else {
            Timber.e(view.getString(R.string.log_onconnectionbroken_without_peer_device), endpointId);
        }
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        if (hashKeyPayloadId != 0 && hashKeyPayloadId == update.getPayloadId()) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                connectionLevel = ConnectionLevel.SENT_HASH_KEY;
            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                hashKeyPayloadId = 0;

                //Todo: Should retry sending the hash key if the connection to the device is still alive
            }
        } else if (connectionSignalPayloadId != 0l) {
            if (update.getPayloadId() == connectionSignalPayloadId
                    && currentReceiver != null
                    && update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {

                disconnectAndReset(currentReceiver.getEndpointId(), false);
                view.showSyncCompleteFragment(true, new SyncCompleteTransferFragment.OnCloseClickListener() {
                    @Override
                    public void onCloseClicked() {
                        view.showP2PModeSelectFragment();
                    }
                }, SyncDataConverterUtil.generateSummaryReport(view.getContext(), transferItems));
                transferItems = null;
            }
        } else {
            if (syncSenderHandler != null) {
                syncSenderHandler.onPayloadTransferUpdate(update);
            }
        }
    }

    @Override
    public void performAuthorization(@NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            String authenticationDetailsJson = new String(payload.asBytes());

            // Validate json is a map
            Map<String, Object> map = null;
            try {
                map = (Map<String, Object>) new Gson()
                        .fromJson(authenticationDetailsJson, Map.class);
            } catch (JsonSyntaxException e) {
                Timber.e(e);
            }

            if (map == null) {
                onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_receiver_details_invalid));
            } else {
                getCurrentPeerDevice().setAuthorizationDetails(map);
                P2PLibrary.getInstance()
                        .getP2PAuthorizationService()
                        .authorizeConnection(map, P2PSenderPresenter.this);
            }
        } else {
            onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_receiver_details_invalid));
        }
    }

    @Override
    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null && syncSenderHandler != null) {
            syncSenderHandler.processString(new String(payload.asBytes()));
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            Timber.e(view.getString(R.string.log_disconnected), endpointId);
            resetState();
            prepareForDiscovering(false);
        } else {
            Timber.e(view.getString(R.string.log_ondisconnected_without_peer_device), endpointId);
        }
    }

    @Override
    public void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails) {
        interactor.sendMessage(new Gson().toJson(authorizationDetails));
    }

    @Override
    public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            // We ignore the authorized state since we should not process anything at this point but more
            // at #onConnectionAuthorized
            if (connectionLevel.equals(ConnectionLevel.AUTHENTICATED)) {
                // Should get the details to authorize
                performAuthorization(payload);
            } else if (connectionLevel.equals(ConnectionLevel.SENT_HASH_KEY)) {
                // Do nothing for now
                processReceivedHistory(endpointId, payload);
            } else if (connectionLevel.equals(ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY)) {
                processPayload(endpointId, payload);
            }
        }
    }

    private void resetState() {
        connectionLevel = null;
        view.dismissAllDialogs();
        setCurrentDevice(null);
        view.enableSendReceiveButtons(true);
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;
        startTransfer();
    }

    public void startTransfer() {
        // Send the hash key
        sendBasicDeviceDetails();

        view.showSyncProgressFragment(view.getString(R.string.sending_data), new SyncProgressFragment.SyncProgressDialogCallback() {
            @Override
            public void onCancelClicked() {
                if (interactor.getCurrentEndpoint() != null) {
                    errorOccurredSync(new Exception("User cancelled sync process"));
                } else {
                    Timber.e("Could not stop sending data because no endpoint exists");
                }
            }
        });
    }

    private void sendBasicDeviceDetails() {
        Map<String, String> basicDeviceDetails = new HashMap<>();
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY, P2PLibrary.getInstance().getHashKey());
        basicDeviceDetails.put(Constants.BasicDeviceDetails.KEY_DEVICE_ID, P2PLibrary.getInstance().getDeviceUniqueIdentifier());

        hashKeyPayloadId = sendTextMessage(new Gson().toJson(basicDeviceDetails));
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        // Disconnect from the endpoint
        if (currentReceiver != null) {
            String endpointId = currentReceiver.getEndpointId();
            addDeviceToBlacklist(endpointId);

            view.showToast(String.format(view.getString(R.string.connection_could_not_be_authorized)
                    , currentReceiver.getEndpointName())
                    , Toast.LENGTH_LONG);

            disconnectAndReset(endpointId);
        } else {
            resetState();
            prepareForDiscovering(false);
        }
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentReceiver;
    }

    @Override
    public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
        currentReceiver = discoveredDevice;
        keepScreenOn(discoveredDevice != null);
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId, boolean startDiscovering) {
        view.removeSyncProgressDialog();

        interactor.disconnectFromEndpoint(endpointId);
        interactor.connectedTo(null);
        connectionSignalPayloadId = 0l;

        resetState();

        if (startDiscovering) {
            prepareForDiscovering(false);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId) {
        disconnectAndReset(endpointId, true);
    }

}
