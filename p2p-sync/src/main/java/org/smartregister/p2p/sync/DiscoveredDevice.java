package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 15/03/2019
 */
public class DiscoveredDevice {

    private String endpointId;

    @Nullable
    private DiscoveredEndpointInfo discoveredEndpointInfo;

    @Nullable
    private ConnectionInfo connectionInfo;

    public DiscoveredDevice(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        this.endpointId = endpointId;
        this.discoveredEndpointInfo = discoveredEndpointInfo;
    }

    public DiscoveredDevice(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        this.endpointId = endpointId;
        this.connectionInfo = connectionInfo;
    }

    public void setDiscoveredEndpointInfo(@NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        this.discoveredEndpointInfo = discoveredEndpointInfo;
    }

    public void setConnectionInfo(@NonNull ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public String getEndpointId() {
        return endpointId;
    }

    @Nullable
    public DiscoveredEndpointInfo getDiscoveredEndpointInfo() {
        return discoveredEndpointInfo;
    }

    @Nullable
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }
}
