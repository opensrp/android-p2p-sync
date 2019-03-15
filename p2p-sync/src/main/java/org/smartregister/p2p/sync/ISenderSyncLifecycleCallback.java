package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.smartregister.p2p.util.BaseSyncConnectionAuthenticator;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */

public interface ISenderSyncLifecycleCallback extends SyncLifecycleCallback
        , BaseSyncConnectionAuthenticator.AuthenticationCallback {

    void onStartedDiscovering(@NonNull Object object);

    void onDiscoveringFailed(@NonNull Exception exception);

    void onDeviceFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo);

    void onRequestConnectionSuccessful(@Nullable Object result);

    void onRequestConnectionFailed(@NonNull Exception exception);

    void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update);
}
