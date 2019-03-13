package org.smartregister.p2p.interactor;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.callback.EndPointDiscoveryCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
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

    public P2pModeSelectInteractor(@NonNull Context context) {
        this.context = context;
        this.appPackageName = context.getApplicationContext().getPackageName();

        connectionsClient = Nearby.getConnectionsClient(context);
    }

    @NonNull
    @Override
    public String getAdvertisingUsername() {
        P2PLibrary p2PLibrary = P2PLibrary.getInstance();
        return p2PLibrary.getUsername();
    }

    @NonNull
    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient
                .startAdvertising(getAdvertisingUsername(), getAppPackageName(), this, advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        advertising = true;
                        // For now this issue does not deal with this
                        Timber.i("Advertising has been started successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING)) {
                            advertising = false;
                        }
                        Timber.e(e, "Advertising could not be started - FAILED");
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
    public void startDiscovering() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder()
                .setStrategy(Constants.STRATEGY)
                .build();

        connectionsClient.startDiscovery(getAppPackageName(), new EndPointDiscoveryCallback(), discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        discovering = true;
                        Timber.i("Discovery has been started successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!(e instanceof ApiException &&
                                ((ApiException) e).getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING)) {
                            discovering = false;
                        }
                        Timber.e(e, "Discovery could not be started - FAILED");
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

    @NonNull
    @Override
    public String getAppPackageName() {
        return appPackageName;
    }

    @Override
    public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
        Timber.i("Connection initiated %s", s);
    }

    @Override
    public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
        Timber.i("Connection result : %s", s);
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
}
