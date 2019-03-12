package org.smartregister.p2p.callback;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 12/03/2019
 */

public class EndPointDiscoveryCallback extends EndpointDiscoveryCallback {

    @Override
    public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        Timber.i("Endpoint found : %s   Endpoint info: (%s, %s)", s, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());
    }

    @Override
    public void onEndpointLost(@NonNull String s) {
        Timber.i("Endpoint lost %s", s);
    }
}
