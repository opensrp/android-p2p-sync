package org.smartregister.p2p.interactor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.util.Constants;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class P2pModeSelectInteractor extends ConnectionLifecycleCallback implements P2pModeSelectContract.Interactor {

    private Context context;
    private String appPackageName;
    private boolean advertising;
    private boolean discovering;

    private ConnectionsClient connectionsClient;
    private ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback;

    @Nullable
    private String endpointIdConnected;

    public P2pModeSelectInteractor(@NonNull Context context) {
        this.context = context;
        this.appPackageName = context.getApplicationContext().getPackageName();

        connectionsClient = Nearby.getConnectionsClient(context);
    }

    @NonNull
    @Override
    public String getUserNickName() {
        P2PLibrary p2PLibrary = P2PLibrary.getInstance();
        return p2PLibrary.getUsername();
    }

    @NonNull
    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void requestConnection(@NonNull String endpointId
            , @NonNull final OnResultCallback onRequestConnectionResult, @NonNull ConnectionLifecycleCallback connectionLifecycleCallback) {
        connectionsClient.requestConnection(getUserNickName(), endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        onRequestConnectionResult.onSuccess(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.e(e);
                        onRequestConnectionResult.onFailure(e);
                    }
                });
    }

    @Override
    public void acceptConnection(@NonNull String endpointId, PayloadCallback payloadCallback) {
        connectionsClient.acceptConnection(endpointId, payloadCallback);
    }

    @Override
    public void rejectConnection(@NonNull final String endpointId) {
        connectionsClient.rejectConnection(endpointId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.i("Connection %s rejected successfully", endpointId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.e(e, "Could not reject connection : %s", endpointId);
                    }
                });
    }

    @Override
    public void startAdvertising(@NonNull final IReceiverSyncLifecycleCallback iReceiverSyncLifecycleCallback) {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient
                .startAdvertising(getUserNickName(), getAppPackageName()
                        , new org.smartregister.p2p.sync.SyncConnectionLifecycleCallback(iReceiverSyncLifecycleCallback)
                        , advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        advertising = true;
                        String message = context.getString(R.string.advertising_started);
                        // For now this issue does not deal with this
                        Timber.i(message);
                        showToast(message);

                        iReceiverSyncLifecycleCallback.onStartedAdvertising(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING)) {
                            advertising = false;
                        }
                        String message = context.getString(R.string.advertising_could_not_be_started);
                        showToast(message);

                        iReceiverSyncLifecycleCallback.onAdvertisingFailed(e);
                        Timber.e(e);
                    }
                });
    }

    @Override
    public void stopAdvertising() {
        if (advertising) {
            advertising = false;
            connectionsClient.stopAdvertising();
        }
    }

    @Override
    public boolean isAdvertising() {
        return advertising;
    }

    @Override
    public void startDiscovering(@NonNull final ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback) {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient.startDiscovery(getAppPackageName(), new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
                iSenderSyncLifecycleCallback.onDeviceFound(endpointId, discoveredEndpointInfo);
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                iSenderSyncLifecycleCallback.onDisconnected(endpointId);
            }
        }, discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        discovering = true;
                        String message = "Discovery has been started successfully";
                        Timber.i(message);

                        iSenderSyncLifecycleCallback.onStartedDiscovering(aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING)) {
                            discovering = false;
                        }
                        String message = "Discovery could not be started - FAILED";
                        Timber.e(e, message);

                        iSenderSyncLifecycleCallback.onDiscoveringFailed(e);
                    }
                });
    }

    @Override
    public void stopDiscovering() {
        if (isDiscovering()) {
            discovering = false;
            connectionsClient.stopDiscovery();
        }
    }

    @Override
    public boolean isDiscovering() {
        return discovering;
    }

    @Override
    public void closeAllEndpoints() {
        connectionsClient.stopAllEndpoints();
    }

    @Override
    public void cleanOngoingConnectionResources() {
        // Todo: rename this to reset lost connection resources
    }

    @Override
    public void sendMessage(@NonNull String message) {
        if (endpointIdConnected != null) {
            connectionsClient.sendPayload(endpointIdConnected, Payload.fromBytes(message.getBytes()));
        }
    }

    @Override
    public void connectedTo(@NonNull String endpointId) {
        endpointIdConnected = endpointId;
    }


    @NonNull
    @Override
    public String getAppPackageName() {
        return appPackageName;
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Timber.i("Connection initiated %s", endpointId);
        // This is in advertising mode
        connectionsClient.acceptConnection(endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                Timber.i("Received a payload from %s", endpointId);
                if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
                    // Show a simple message of the text sent
                    String message = new String(payload.asBytes());
                    showToast(message);

                    if (context instanceof P2pModeSelectContract.View) {
                        ((P2pModeSelectContract.View) context).displayMessage(endpointId + ": " + message);
                    }
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        });
    }

    @Override
    public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        Timber.i("Connection result : %s", endpointId);

        if (connectionResolution.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_OK) {
            endpointIdConnected = endpointId;
        }
    }

    @Override
    public void onDisconnected(@NonNull String s) {
        Timber.i("Disconnected: %s", s);
    }

    @Override
    public void cleanupResources() {
        connectionsClient = null;
        context = null;
    }

    public void showToast(@NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG)
                .show();
    }
}
