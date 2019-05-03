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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.ReceiverConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.AppDatabase;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.SyncReceiverHandler;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 18/03/2019
 */

public class P2PReceiverPresenter extends BaseP2pModeSelectPresenter implements P2pModeSelectContract.ReceiverPresenter
        , IReceiverSyncLifecycleCallback, P2PAuthorizationService.AuthorizationCallback {

    @Nullable
    private DiscoveredDevice currentSender;
    private SendingDevice currentSendingDevice;
    private ConnectionLevel connectionLevel;

    private SyncReceiverHandler syncReceiverHandler;

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
                        return Constants.RqCode.PERMISSIONS;
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
            keepScreenOn(true);
            view.showReceiveProgressDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    keepScreenOn(false);
                    interactor.stopAdvertising();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startAdvertising(this);
        }
    }

    @Nullable
    @Override
    public SendingDevice getSendingDevice() {
        return currentSendingDevice;
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

        // Reject when already connected or the connecting device is blacklisted
        if (currentSender == null && !blacklistedDevices.contains(endpointId)) {
            setCurrentDevice(new DiscoveredDevice(endpointId, connectionInfo));

            // First stop advertising
            keepScreenOn(false);
            interactor.stopAdvertising();
            view.removeReceiveProgressDialog();

            // This can be moved to the library for easy customisation by host applications
            BaseSyncConnectionAuthenticator syncConnectionAuthenticator = new ReceiverConnectionAuthenticator(this);
            syncConnectionAuthenticator.authenticate(currentSender, this);
        } else {
            Timber.e(view.getString(R.string.log_rejecting_connection_initiated_by_other_device)
                    , endpointId
                    , connectionInfo.getEndpointName()
                    , connectionInfo.getAuthenticationToken());
            // We can add connection support for multiple devices here later
            interactor.rejectConnection(endpointId);
        }
    }

    @Override
    public void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentSender != null) {
            connectionLevel = ConnectionLevel.AUTHENTICATED;
            interactor.connectedTo(endpointId);
            P2PAuthorizationService authorizationService = P2PLibrary.getInstance()
                    .getP2PAuthorizationService();
            authorizationService.getAuthorizationDetails(new P2PAuthorizationService.OnAuthorizationDetailsProvidedCallback() {
                @Override
                public void onAuthorizationDetailsProvided(@NonNull Map<String, Object> authorizationDetails) {
                    // Send the authorization details
                    sendAuthorizationDetails(authorizationDetails);
                }
            });
        }
    }

    @Override
    public void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        if (currentSender != null) {
            view.showToast(view.getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
            resetState();
            prepareForAdvertising(false);
        }
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to advertising mode
        //Todo: And show the user an error
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            view.showToast(view.getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
            disconnectAndReset(endpointId);
        }
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to advertising mode
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            disconnectAndReset(endpointId);
            view.showToast(String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId), Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onPayloadReceived(@NonNull final String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            if (connectionLevel.equals(ConnectionLevel.SENT_RECEIVED_HISTORY)) {
                processPayload(endpointId, payload);
            } else if (connectionLevel.equals(ConnectionLevel.AUTHENTICATED)) {
                // Authorize the connection from the details received
                performAuthorization(payload);
            } else if (connectionLevel.equals(ConnectionLevel.AUTHORIZED)) {
                // Waiting for the hash key
                processHashKey(endpointId, payload);
            }
        }
    }

    @Override
    public void processHashKey(@NonNull final String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            String payloadAsString = new String(payload.asBytes());

            Map<String, Object> basicDeviceDetails = null;
            try {
                basicDeviceDetails = (Map<String, Object>) new Gson()
                        .fromJson(payloadAsString, Map.class);
            } catch (JsonSyntaxException e) {
                Timber.e(e);
            }

            if (basicDeviceDetails == null || basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID) == null) {
                Timber.e(view.getString(R.string.log_hash_key_sent_was_null));
                disconnectAndReset(endpointId);
            } else {
                connectionLevel = ConnectionLevel.RECEIVED_HASH_KEY;

                // Check if the device has been interacting with this app if it's state when it started
                // and now is the same
                // Should be done in the background
                checkIfDeviceKeyHasChanged(basicDeviceDetails, endpointId);
            }

        } else {
            Timber.e(view.getString(R.string.log_hash_key_was_sent_in_an_invalid_format));
            disconnectAndReset(endpointId);
        }
    }

    @Override
    public void sendLastReceivedRecords(@NonNull List<P2pReceivedHistory> receivedHistory) {
        if (currentSender != null) {
            interactor.sendMessage(new Gson().toJson(receivedHistory));
            connectionLevel = ConnectionLevel.SENT_RECEIVED_HISTORY;

            syncReceiverHandler = new SyncReceiverHandler(this);
        }
    }

    private SendingDevice registerSendingDevice(Map<String, Object> basicDeviceDetails) {
        SendingDevice sendingDevice = new SendingDevice();
        sendingDevice.setDeviceId((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
        sendingDevice.setAppLifetimeKey((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY));

        P2PLibrary.getInstance().getDb()
                .sendingDeviceDao()
                .insert(sendingDevice);

        return sendingDevice;
    }

    @Nullable
    private Integer clearDeviceHistoryAndUpdateDeviceKey(final SendingDevice sendingDevice, String appLifetimeKey) {
        final AppDatabase db = P2PLibrary.getInstance().getDb();
        sendingDevice.setAppLifetimeKey(appLifetimeKey);

        try {
            return db.runInTransaction(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    db.sendingDeviceDao().update(sendingDevice);
                    return db.p2pReceivedHistoryDao()
                            .clearDeviceRecords(sendingDevice.getDeviceId());
                }
            });
        } catch (RuntimeException e) {
            Timber.e(e);
            return null;
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
                onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_sender_details_invalid));
            } else {
                P2PLibrary.getInstance().getP2PAuthorizationService()
                        .authorizeConnection(map, P2PReceiverPresenter.this);
            }
        } else {
            onConnectionAuthorizationRejected(view.getString(R.string.reason_authorization_rejected_by_sender_details_invalid));
        }
    }

    @Override
    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        if (syncReceiverHandler != null) {
            syncReceiverHandler.processPayload(endpointId, payload);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId, boolean startAdvertising) {
        interactor.disconnectFromEndpoint(endpointId);
        interactor.connectedTo(null);
        resetState();

        if (startAdvertising) {
            prepareForAdvertising(false);
        }
    }

    @Override
    public void disconnectAndReset(@NonNull String endpointId) {
        disconnectAndReset(endpointId, true);
    }

    private void checkIfDeviceKeyHasChanged(@NonNull final Map<String, Object> basicDeviceDetails, final @NonNull String endpointId) {
        Tasker.run(new Callable<SendingDevice>() {
            @Override
            public SendingDevice call() throws Exception {
                return P2PLibrary.getInstance().getDb()
                        .sendingDeviceDao()
                        .getSendingDevice((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
            }
        }, new GenericAsyncTask.OnFinishedCallback<SendingDevice>() {
            @Override
            public void onSuccess(@Nullable SendingDevice result) {
                if (result != null) {
                    final SendingDevice sendingDevice = result;
                    final String appLifetimeKey = (String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY);

                    if (sendingDevice.getAppLifetimeKey()
                            .equals(appLifetimeKey)) {
                        currentSendingDevice = sendingDevice;
                        Tasker.run(new Callable<List<P2pReceivedHistory>>() {
                            @Override
                            public List<P2pReceivedHistory> call() throws Exception {
                                return P2PLibrary.getInstance().getDb()
                                        .p2pReceivedHistoryDao()
                                        .getDeviceReceivedHistory(sendingDevice.getDeviceId());
                            }
                        }, new GenericAsyncTask.OnFinishedCallback<List<P2pReceivedHistory>>() {
                            @Override
                            public void onSuccess(@Nullable List<P2pReceivedHistory> result) {
                                sendLastReceivedRecords(result != null ? result :new ArrayList<P2pReceivedHistory>());
                            }

                            @Override
                            public void onError(Exception e) {
                                Timber.e(e);
                                disconnectAndReset(endpointId);
                            }
                        });

                    } else {
                        // Clear the device history records && update device app key
                        Tasker.run(new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                sendingDevice.setAppLifetimeKey(appLifetimeKey);
                                currentSendingDevice = sendingDevice;
                                return clearDeviceHistoryAndUpdateDeviceKey(sendingDevice, appLifetimeKey);
                            }
                        }, new GenericAsyncTask.OnFinishedCallback<Integer>() {
                            @Override
                            public void onSuccess(@Nullable Integer result) {
                                if (result != null) {
                                    Timber.e(view.getString(R.string.log_records_deleted), (int) result);
                                    sendLastReceivedRecords(new ArrayList<P2pReceivedHistory>());
                                } else {
                                    onError(new Exception("Clear device history and update device key Transaction failed!"));
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Timber.e(view.getString(R.string.log_error_occurred_trying_to_delete_p2p_received_history_on_device)
                                        , sendingDevice.getDeviceId());
                                disconnectAndReset(endpointId);
                            }
                        });

                    }
                } else {
                    // This is a new device we should save it
                    Tasker.run(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            currentSendingDevice = registerSendingDevice(basicDeviceDetails);
                            return null;
                        }
                    }, new GenericAsyncTask.OnFinishedCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            sendLastReceivedRecords(new ArrayList<P2pReceivedHistory>());
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.e(e);
                            view.showToast(view.getString(R.string.an_error_occurred_trying_to_save_new_sender_details), Toast.LENGTH_LONG);

                            disconnectAndReset(endpointId);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
                disconnectAndReset(endpointId);
            }
        });
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        // Do nothing for now
        if (syncReceiverHandler != null) {
            syncReceiverHandler.onPayloadTransferUpdate(endpointId, update);
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        if (getCurrentPeerDevice() != null && endpointId.equals(getCurrentPeerDevice().getEndpointId())) {
            Timber.e(view.getString(R.string.log_endpoint_lost), endpointId);
            resetState();
            prepareForAdvertising(false);
        }
    }

    @Override
    public void sendAuthorizationDetails(@NonNull Map<String, Object> authorizationDetails) {
        interactor.sendMessage(new Gson().toJson(authorizationDetails));
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
                    P2PReceiverPresenter.this.onPayloadTransferUpdate(s, payloadTransferUpdate);
                }
            });
        }
    }

    @Override
    public void onAuthenticationFailed(@NonNull Exception exception) {
        // Reject the connection
        if (currentSender != null) {
            String endpointId = currentSender.getEndpointId();
            interactor.rejectConnection(endpointId);

            rejectDeviceOnAuthentication(endpointId);
        }

        view.showToast(view.getString(R.string.authentication_failed_connection_rejected), Toast.LENGTH_LONG);
        resetState();
        prepareForAdvertising(false);

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
            String endpointId = currentSender.getEndpointId();
            interactor.rejectConnection(endpointId);
        }
        resetState();
        prepareForAdvertising(false);

        // Go back to discovering mode
        Timber.e(view.getString(R.string.log_authentication_cancelled), reason);
    }

    private void resetState() {
        syncReceiverHandler = null;
        connectionLevel = null;
        view.dismissAllDialogs();
        view.enableSendReceiveButtons(true);
        setCurrentDevice(null);
        currentSendingDevice = null;
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;
        view.showToast(String.format(view.getContext().getString(R.string.you_are_connected_to_sender), currentSender.getEndpointName())
                , Toast.LENGTH_LONG);
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        //Timber.e(reason);
        // Disconnect from the endpoint
        if (currentSender != null) {
            String endpointId = currentSender.getEndpointId();
            addDeviceToBlacklist(endpointId);

            view.showToast(String.format(view.getString(R.string.connection_could_not_be_authorized)
                    , currentSender.getEndpointName())
                    , Toast.LENGTH_LONG);

            disconnectAndReset(endpointId);
        } else {
            resetState();
            prepareForAdvertising(false);
        }
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentSender;
    }

    @Override
    public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
        currentSender = discoveredDevice;
        keepScreenOn(discoveredDevice != null);
    }
}
