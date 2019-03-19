package org.smartregister.p2p.sync;

import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
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
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;

import static org.junit.Assert.*;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
public class SenderSyncLifecycleCallbackTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    @Mock
    private P2pModeSelectContract.Presenter presenter;

    private SenderSyncLifecycleCallback senderSyncLifecycleCallback;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        senderSyncLifecycleCallback = new SenderSyncLifecycleCallback(view, presenter, interactor);
    }

    @Test
    public void onDiscoveringFailedShouldResetUI() {
        senderSyncLifecycleCallback.onDiscoveringFailed(new Exception());

        Mockito.verify(view, Mockito.times(1))
                .removeDiscoveringProgressDialog();

        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
    }

    @Test
    public void onDeviceFoundShouldStopDiscoveringAndRequestConnectionWhenNoOtherConnectionIsBeingNegotiated() {
        String endpointId = "id";
        String deviceName = "SAMSUNG SM78";
        String serviceId = "com.example.app";

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        Mockito.doReturn(serviceId)
                .when(discoveredEndpointInfo)
                .getServiceId();

        senderSyncLifecycleCallback.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(interactor, Mockito.times(1))
                .stopDiscovering();

        Mockito.verify(view, Mockito.times(1))
                .removeDiscoveringProgressDialog();

        Mockito.verify(interactor, Mockito.times(1))
                .requestConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(OnResultCallback.class)
                        , Mockito.any(ConnectionLifecycleCallback.class));

        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }

    @Test
    public void onDeviceFoundShouldDoNothingWhenAnotherConnectionIsBeingNegotiated() {
        String endpointId = "id";

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(interactor, Mockito.times(0))
                .stopDiscovering();

        Mockito.verify(view, Mockito.times(0))
                .removeDiscoveringProgressDialog();

        Mockito.verify(interactor, Mockito.times(0))
                .requestConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(OnResultCallback.class)
                        , Mockito.any(ConnectionLifecycleCallback.class));
    }

    @Test
    public void onDeviceFoundShouldCallThisOnRequestConnectionSuccessfulWhenNoOtherConnectionIsBeingNegotiatedAndConnectionRequestIsSuccessful() {
        String endpointId = "id";
        String deviceName = "SAMSUNG SM78";
        String serviceId = "com.example.app";

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        Mockito.doReturn(serviceId)
                .when(discoveredEndpointInfo)
                .getServiceId();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((OnResultCallback) invocation.getArgument(1))
                        .onSuccess(null);

                return null;
            }
        })
                .when(interactor)
                .requestConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(OnResultCallback.class)
                        , Mockito.any(ConnectionLifecycleCallback.class));

        SenderSyncLifecycleCallback spiedCallback = Mockito.spy(senderSyncLifecycleCallback);

        spiedCallback.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(spiedCallback, Mockito.times(1))
                .onRequestConnectionSuccessful(Mockito.any());
    }

    @Test
    public void onDeviceFoundShouldCallThisOnRequestConnectionFailedWhenNoOtherConnectionIsBeingNegotiatedAndConnectionRequestFails() {
        String endpointId = "id";
        String deviceName = "SAMSUNG SM78";
        String serviceId = "com.example.app";

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        Mockito.doReturn(serviceId)
                .when(discoveredEndpointInfo)
                .getServiceId();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((OnResultCallback) invocation.getArgument(1))
                        .onFailure(new Exception());

                return null;
            }
        })
                .when(interactor)
                .requestConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(OnResultCallback.class)
                        , Mockito.any(ConnectionLifecycleCallback.class));

        SenderSyncLifecycleCallback spiedCallback = Mockito.spy(senderSyncLifecycleCallback);

        spiedCallback.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(spiedCallback, Mockito.times(1))
                .onRequestConnectionFailed(Mockito.any(Exception.class));
    }

    @Test
    public void onRequestConnectionFailedShouldResetStateAndStartDiscoveringMode() {
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onRequestConnectionFailed(new Exception());

        Mockito.verify(presenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }

    @Test
    public void onConnectionInitiatedShouldUpdateDiscoveredDeviceInfoAndShowQRCodeScanningDialogWhenNegotiatingDeviceConnection() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onConnectionInitiated(endpointId, connectionInfo);

        assertEquals(connectionInfo, ((DiscoveredDevice)ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"))
                .getConnectionInfo());

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeScanningDialog(Mockito.any(QRCodeScanningDialog.QRCodeScanDialogCallback.class));
    }

    @Test
    public void onConnectionInitiatedShouldDoNothingWhenNotNegotiatingDeviceConnection() {
        String endpointId = "id";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onConnectionInitiated(endpointId, connectionInfo);

        Mockito.verify(view, Mockito.times(0))
                .showQRCodeScanningDialog(Mockito.any(QRCodeScanningDialog.QRCodeScanDialogCallback.class));
    }

    @Test
    public void onAuthenticationSuccessfulShouldAcceptConnectionWhenNegotiatingDeviceConnection() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
        senderSyncLifecycleCallback.onAuthenticationSuccessful();

        Mockito.verify(interactor, Mockito.times(1))
                .acceptConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(PayloadCallback.class));
        Mockito.verify(view, Mockito.times(1))
                .showToast(ArgumentMatchers.eq("Authentication successful! Receiver can accept connection")
                        , ArgumentMatchers.eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onAuthenticationFailedShouldRejectConnectionWhenNegotiatingDeviceConnection() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onAuthenticationFailed(new Exception());

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
        Mockito.verify(view, Mockito.times(1))
                .showToast(ArgumentMatchers.eq("Authentication failed! The connection has been rejected")
                        , ArgumentMatchers.eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onAuthenticationCancelledShouldRejectConnectionWhenNegotiatingDeviceConnection() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onAuthenticationCancelled("");

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionAcceptedShouldRegisterReceiverDeviceWithInteractor() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);

        senderSyncLifecycleCallback.onConnectionAccepted(endpointId, connectionResolution);

        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionRejectedShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onConnectionRejected(endpointId, connectionResolution);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }

    @Test
    public void onConnectionUnknownErrorShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onConnectionUnknownError(endpointId, connectionResolution);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }

    @Test
    public void onConnectionBrokenShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onConnectionBroken(endpointId);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }

    @Test
    public void onDisconnectedShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(senderSyncLifecycleCallback, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));

        senderSyncLifecycleCallback.onDisconnected(endpointId);

        Mockito.verify(presenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(senderSyncLifecycleCallback, "currentReceiver"));
    }
}