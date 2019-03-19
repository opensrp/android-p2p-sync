package org.smartregister.p2p.sync;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */

@RunWith(RobolectricTestRunner.class)
public class SyncConnectionLifecycleCallbackTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SyncLifecycleCallback syncLifecycleCallback;
    private SyncConnectionLifecycleCallback syncConnectionLifecycleCallback;

    @Before
    public void setUp() throws Exception {
        syncConnectionLifecycleCallback = new SyncConnectionLifecycleCallback(syncLifecycleCallback);
    }

    @Test
    public void onConnectionInitiatedShouldCallSyncLifecycleCallbackOnConnectionInitiated() {
        String endpointId = "id";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        syncConnectionLifecycleCallback.onConnectionInitiated(endpointId, connectionInfo);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionInitiated(ArgumentMatchers.eq(endpointId)
                        , Mockito.eq(connectionInfo));
    }

    @Test
    public void onConnectionResultShouldCallConnectionAcceptedWhenConnectionResultAndStatusCodeIsOk() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        Status status = Mockito.mock(Status.class);

        Mockito.doReturn(ConnectionsStatusCodes.STATUS_OK)
                .when(status)
                .getStatusCode();

        Mockito.doReturn(status)
                .when(connectionResolution)
                .getStatus();

        syncConnectionLifecycleCallback.onConnectionResult(endpointId, connectionResolution);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionAccepted(ArgumentMatchers.eq(endpointId)
                        , ArgumentMatchers.eq(connectionResolution));
    }

    @Test
    public void onConnectionResultShouldCallConnectionRejectedWhenConnectionResultAndStatusCodeIsConnectionRejected() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        Status status = Mockito.mock(Status.class);

        Mockito.doReturn(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
                .when(status)
                .getStatusCode();

        Mockito.doReturn(status)
                .when(connectionResolution)
                .getStatus();

        syncConnectionLifecycleCallback.onConnectionResult(endpointId, connectionResolution);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionRejected(ArgumentMatchers.eq(endpointId)
                        , ArgumentMatchers.eq(connectionResolution));
    }

    @Test
    public void onConnectionResultShouldCallConnectionUnknownErrorWhenConnectionResultAndStatusCodeIsStatusError() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        Status status = Mockito.mock(Status.class);

        Mockito.doReturn(ConnectionsStatusCodes.STATUS_ERROR)
                .when(status)
                .getStatusCode();

        Mockito.doReturn(status)
                .when(connectionResolution)
                .getStatus();

        syncConnectionLifecycleCallback.onConnectionResult(endpointId, connectionResolution);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionUnknownError(ArgumentMatchers.eq(endpointId)
                        , ArgumentMatchers.eq(connectionResolution));
    }

    @Test
    public void onConnectionResultShouldCallConnectionAcceptedWhenConnectionResultAndStatusCodeIsUnknown() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        Status status = Mockito.mock(Status.class);

        Mockito.doReturn(100)
                .when(status)
                .getStatusCode();

        Mockito.doReturn(status)
                .when(connectionResolution)
                .getStatus();

        syncConnectionLifecycleCallback.onConnectionResult(endpointId, connectionResolution);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionUnknownError(ArgumentMatchers.eq(endpointId)
                        , ArgumentMatchers.eq(connectionResolution));
    }

    @Test
    public void onDisconnectedShouldCallConnectionBrokenWhenConnectionDisconnected() {
        String endpointId = "id";

        syncConnectionLifecycleCallback.onDisconnected(endpointId);

        Mockito.verify(syncLifecycleCallback, Mockito.times(1))
                .onConnectionBroken(ArgumentMatchers.eq(endpointId));
    }
}