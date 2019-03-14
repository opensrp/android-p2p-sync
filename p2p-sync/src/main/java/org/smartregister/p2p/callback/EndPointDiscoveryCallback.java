package org.smartregister.p2p.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.SenderConnectionLifecycleCallback;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 12/03/2019
 */

public class EndPointDiscoveryCallback extends EndpointDiscoveryCallback {

    private P2pModeSelectContract.View view;
    private P2pModeSelectContract.Interactor interactor;
    private P2pModeSelectContract.Presenter presenter;

    public EndPointDiscoveryCallback(@NonNull P2pModeSelectContract.Interactor interactor, @NonNull P2pModeSelectContract.View view
            , @NonNull P2pModeSelectContract.Presenter presenter) {
        this.view = view;
        this.interactor = interactor;
        this.presenter = presenter;
    }

    @Override
    public void onEndpointFound(@NonNull final String endpointId, @NonNull final DiscoveredEndpointInfo discoveredEndpointInfo) {
        Timber.i("Endpoint found : %s   Endpoint info: (%s, %s)", endpointId, discoveredEndpointInfo.getEndpointName(), discoveredEndpointInfo.getServiceId());

        // First stop discovering
        interactor.stopDiscovering();
        view.removeDiscoveringProgressDialog();

        interactor.requestConnection(endpointId, new OnResultCallback() {
            @Override
            public void onSuccess(@Nullable Object object) {
                // Just show a success
                view.showToast("CONNECTION REQUEST WAS SUCCESSFUL", Toast.LENGTH_LONG);
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                // Show the user an error trying to connect device XYZ
                view.showToast("COULD NOT INTIATE CONNECTION REQUEST TO THE DEVICE", Toast.LENGTH_LONG);
                presenter.startDiscoveringMode();
            }
        }, new SenderConnectionLifecycleCallback(view, presenter, interactor, discoveredEndpointInfo));
    }

    @Override
    public void onEndpointLost(@NonNull String s) {
        Timber.e("Endpoint lost %s", s);
    }
}
