package org.smartregister.p2p.sync;


import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;

import org.smartregister.p2p.authenticator.BaseSyncConnectionAuthenticator;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public interface SyncLifecycleCallback extends BaseSyncConnectionAuthenticator.AuthenticationCallback {

    void onConnectionInitiated(@NonNull final String endpointId, @NonNull final ConnectionInfo connectionInfo);

    void onConnectionAccepted(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionRejected(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionUnknownError(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution);

    void onConnectionBroken(@NonNull String endpointId);

    void onDisconnected(@NonNull String endpointId);

    interface SyncConnectionLifecycleCallback {

    }
}
