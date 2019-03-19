package org.smartregister.p2p.sync;

import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeGeneratorDialog;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
public class ReceiverSyncLifecycleCallbackTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Presenter presenter;
    @Mock
    private P2pModeSelectContract.Interactor interactor;

    private ReceiverSyncLifecycleCallback receiverSyncLifecycleCallback;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        receiverSyncLifecycleCallback = new ReceiverSyncLifecycleCallback(view, presenter, interactor);
    }

    @Test
    public void onAdvertisingFailedShouldResetUI() {
        receiverSyncLifecycleCallback.onAdvertisingFailed(Mockito.mock(Exception.class));

        Mockito.verify(view, Mockito.times(1))
                .removeReceiveProgressDialog();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
    }

    @Test
    public void onConnectionInitiatedShouldDoNothingWhenNoAnotherConnectionIsBeingNegotiated() {
        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", Mockito.mock(DiscoveredDevice.class));
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));
        receiverSyncLifecycleCallback.onConnectionInitiated("id", Mockito.mock(ConnectionInfo.class));

        Mockito.verify(interactor, Mockito.times(0))
                .stopAdvertising();

        Mockito.verify(view, Mockito.times(0))
                .removeReceiveProgressDialog();
    }

    @Test
    public void onConnectionInitiatedShouldStopAdvertisingAndAuthenticateConnectionWhenNoOtherConnectionIsBeingNegotiated() {
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        receiverSyncLifecycleCallback.onConnectionInitiated("id", connectionInfo);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        Mockito.verify(interactor, Mockito.times(1))
                .stopAdvertising();

        Mockito.verify(view, Mockito.times(1))
                .removeReceiveProgressDialog();

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeGeneratorDialog(ArgumentMatchers.eq(authenticationCode)
                        , ArgumentMatchers.eq(deviceName)
                        , Mockito.any(QRCodeGeneratorDialog.QRCodeAuthenticationCallback.class));
    }

    @Test
    public void onConnectionAcceptedShouldRegisterSenderDeviceWithInteractor() {
        String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));


        receiverSyncLifecycleCallback.onConnectionAccepted(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionRejectedShouldRestartAdvertisingModeAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onConnectionRejected(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));
    }

    @Test
    public void onConnectionUnknownErrorShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onConnectionUnknownError(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));
    }

    @Test
    public void onConnectionBrokenShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onConnectionBroken(endpointId);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));
    }

    @Test
    public void onDisconnectedShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onDisconnected(endpointId);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));
    }

    @Test
    public void onAuthenticationSuccessfulShouldAcceptConnectWhenNegotiatingConnectionWithSender() {
        String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onAuthenticationSuccessful();
        Mockito.verify(interactor, Mockito.times(1))
                .acceptConnection(ArgumentMatchers.eq(endpointId), Mockito.any(PayloadCallback.class));
    }

    @Test
    public void onAuthenticationSuccessfulShouldRegisterCurrentInstanceAsPayloadListener() {
        final String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReceiverSyncLifecycleCallback spiedCallback = Mockito.spy(receiverSyncLifecycleCallback);

        ReflectionHelpers.setField(spiedCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(spiedCallback, "currentSender"));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PayloadCallback) invocation.getArgument(1))
                        .onPayloadReceived(endpointId, Mockito.mock(Payload.class));

                return null;
            }
        })
                .when(interactor)
                .acceptConnection(ArgumentMatchers.eq(endpointId), Mockito.any(PayloadCallback.class));

        spiedCallback.onAuthenticationSuccessful();
        Mockito.verify(spiedCallback, Mockito.times(1))
                .onPayloadReceived(ArgumentMatchers.eq(endpointId), Mockito.any(Payload.class));
    }

    @Test
    public void onAuthenticationFailedShouldRejectConnectionWhenNegotiatingWithASender() {
        final String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onAuthenticationFailed(new Exception());

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
        Mockito.verify(view, Mockito.times(1))
                .showToast(ArgumentMatchers.eq("Authentication failed! The connection has been rejected")
                        , ArgumentMatchers.eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onAuthenticationCancelledShouldRejectConnectionWhenNegotiatingWithASender() {
        final String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReflectionHelpers.setField(receiverSyncLifecycleCallback, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(receiverSyncLifecycleCallback, "currentSender"));

        receiverSyncLifecycleCallback.onAuthenticationCancelled("");

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
    }
}