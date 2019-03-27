package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.os.AsyncTask;
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

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;
import org.smartregister.p2p.authenticator.ReceiverConnectionAuthenticator;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.AppDatabase;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;

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
    private ConnectionLevel connectionLevel;

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
        view.showToast(view.getString(R.string.receiver_rejected_the_connection), Toast.LENGTH_LONG);
        resetState();
        prepareForAdvertising(false);
    }

    @Override
    public void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        //Todo: Go back to advertising mode
        //Todo: And show the user an error
        view.showToast(view.getString(R.string.an_error_occurred_before_acceptance_or_rejection), Toast.LENGTH_LONG);
        resetState();
        prepareForAdvertising(false);
    }

    @Override
    public void onConnectionBroken(@NonNull String endpointId) {
        //Todo: Show the user an error
        //Todo: Go back to advertising mode
        resetState();
        view.showToast(String.format(view.getString(R.string.connection_to_endpoint_broken), endpointId), Toast.LENGTH_LONG);
        prepareForAdvertising(false);
    }

    @Override
    public void onPayloadReceived(@NonNull final String endpointId, @NonNull Payload payload) {
        Timber.i(view.getString(R.string.log_received_payload_from_endpoint), endpointId);
        if (connectionLevel != null) {
            if (connectionLevel.equals(ConnectionLevel.RECEIVED_HASH_KEY)) {
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

            final Map<String, Object> basicDeviceDetails = (Map<String, Object>) new Gson()
                    .fromJson(payloadAsString, Map.class);
            if (basicDeviceDetails == null || basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID) == null) {
                Timber.e("Hash key was sent was null");
                disconnectAndReset(endpointId);
            } else {
                connectionLevel = ConnectionLevel.RECEIVED_HASH_KEY;
                // Check if the device has been interacting with this app if it's state when it started
                // and now is the same
                // Should be done in the background
                checkIfDeviceKeyHasChanged(basicDeviceDetails, new GenericAsyncTask.OnFinishedCallback<SendingDevice>() {
                    @Override
                    public void onSuccess(@Nullable SendingDevice result) {
                        if (result != null) {
                            final SendingDevice sendingDevice = result;
                            final String appLifetimeKey = (String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY);

                            if (sendingDevice.getAppLifetimeKey()
                                    .equals(appLifetimeKey)) {
                                // Todo: Get the records sent last time

                            } else {
                                // Clear the device history records && update device app key
                                Tasker.run(new Callable<Integer>() {
                                    @Override
                                    public Integer call() throws Exception {
                                        return clearDeviceHistoryAndUpdateDeviceKey(sendingDevice, appLifetimeKey);
                                    }
                                }, new GenericAsyncTask.OnFinishedCallback<Integer>() {
                                    @Override
                                    public void onSuccess(@Nullable Integer result) {
                                        if (result != null) {
                                            Timber.e("%d records deleted", (int) result);
                                        }

                                        // Todo: get the records sent last time and continue with the process
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Timber.e("An error occurred trying to delete the P2P received history of the device %s"
                                                , sendingDevice.getDeviceUniqueId());
                                    }
                                });

                            }
                        } else {
                            // This is a new device we should save it
                            Tasker.run(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    registerSendingDevice(basicDeviceDetails);

                                    return null;
                                }
                            }, new GenericAsyncTask.OnFinishedCallback<Void>() {
                                @Override
                                public void onSuccess(@Nullable Void result) {
                                    // Todo: get the records sent last time and continue with the process
                                }

                                @Override
                                public void onError(Exception e) {
                                    Timber.e(e);
                                    view.showToast("An error occurred trying to save the new sender details", Toast.LENGTH_LONG);

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

        } else {
            Timber.e("Hash key was sent in an invalid format");
            disconnectAndReset(endpointId);
        }
    }

    private void registerSendingDevice(Map<String, Object> basicDeviceDetails) {
        SendingDevice sendingDevice = new SendingDevice();
        sendingDevice.setDeviceUniqueId((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
        sendingDevice.setAppLifetimeKey((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY));

        P2PLibrary.getInstance().getDb()
                .sendingDeviceDao()
                .insert(sendingDevice);
    }

    @NonNull
    private Integer clearDeviceHistoryAndUpdateDeviceKey(SendingDevice sendingDevice, String appLifetimeKey) {
        AppDatabase db = P2PLibrary.getInstance().getDb();

        sendingDevice.setAppLifetimeKey(appLifetimeKey);
        db.sendingDeviceDao().update(sendingDevice);

        return db.p2pReceivedHistoryDao()
                .clearDeviceRecords(sendingDevice.getId());
    }

    @Override
    public void performAuthorization(@NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            String authenticationDetailsJson = new String(payload.asBytes());

            Map<String, Object> map = (Map<String, Object>) new Gson()
                    .fromJson(authenticationDetailsJson, Map.class);

            if (map == null) {
                onConnectionAuthorizationRejected("Authorization details sent by receiver are invalid");
            } else {
                P2PLibrary.getInstance().getP2PAuthorizationService()
                        .authorizeConnection(map, P2PReceiverPresenter.this);
            }
        } else {
            onConnectionAuthorizationRejected("Authorization details sent by receiver are invalid");
        }
    }

    @Override
    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            // Show a simple message of the text sent
            String message = new String(payload.asBytes());
            view.showToast(message, Toast.LENGTH_LONG);
            view.displayMessage(String.format(view.getString(R.string.chat_message_format),endpointId, message));
        }
    }

    private void disconnectAndReset(@NonNull String endpointId) {
        interactor.disconnectFromEndpoint(endpointId);
        resetState();
        prepareForAdvertising(false);
    }

    private void checkIfDeviceKeyHasChanged(@NonNull final Map<String, Object> basicDeviceDetails
            , @NonNull GenericAsyncTask.OnFinishedCallback<SendingDevice> onFinishedCallback) {
        Tasker.run(new Callable<SendingDevice>() {
            @Override
            public SendingDevice call() throws Exception {
                return P2PLibrary.getInstance().getDb()
                        .sendingDeviceDao()
                        .getSendingDevice((String) basicDeviceDetails.get(Constants.BasicDeviceDetails.KEY_DEVICE_ID));
            }
        }, onFinishedCallback);
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        // Do nothing for now
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        Timber.e(view.getString(R.string.log_endpoint_lost), endpointId);
        resetState();
        prepareForAdvertising(false);
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
        connectionLevel = null;
        view.dismissAllDialogs();
        currentSender = null;
    }

    @Override
    public void onConnectionAuthorized() {
        connectionLevel = ConnectionLevel.AUTHORIZED;
        view.showToast(String.format(view.getContext().getString(R.string.you_are_connected_to_sender), currentSender.getEndpointName())
                , Toast.LENGTH_LONG);
        view.displayMessage(view.getString(R.string.connected));
    }

    @Override
    public void onConnectionAuthorizationRejected(@NonNull String reason) {
        interactor.closeAllEndpoints();
        interactor.connectedTo(null);
        view.showToast(String.format(view.getContext().getString(R.string.connection_could_not_be_authorized)
                , currentSender.getEndpointName())
                , Toast.LENGTH_LONG);

        resetState();
        prepareForAdvertising(false);
    }

    @Nullable
    @Override
    public DiscoveredDevice getCurrentPeerDevice() {
        return currentSender;
    }
}
