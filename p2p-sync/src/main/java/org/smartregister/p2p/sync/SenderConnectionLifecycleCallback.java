package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class SenderConnectionLifecycleCallback extends ConnectionLifecycleCallback {

    private ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback;

    public SenderConnectionLifecycleCallback(@NonNull ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback) {
        this.iSenderSyncLifecycleCallback = iSenderSyncLifecycleCallback;
    }

    @Override
    public void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo) {
        iSenderSyncLifecycleCallback.onConnectionInitiated(endpointId, connectionInfo);
    }

    @Override
    public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
        switch(connectionResolution.getStatus().getStatusCode()) {
            case ConnectionsStatusCodes.STATUS_OK:
                // The start of the connection
                iSenderSyncLifecycleCallback.onConnectedAccepted(endpointId, connectionResolution);
                break;

            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                // The receiver rejected the connection
                iSenderSyncLifecycleCallback.onConnectionRejected(endpointId, connectionResolution);
                break;

            case ConnectionsStatusCodes.STATUS_ERROR:
                // The connection broke before it was able to be accepted
                iSenderSyncLifecycleCallback.onConnectionUnknownError(endpointId, connectionResolution);
                break;
        }
    }

    @Override
    public void onDisconnected(@NonNull String endpointId) {
        // This will not be handled here
        iSenderSyncLifecycleCallback.onConnectionBroken(endpointId);
    }

}
