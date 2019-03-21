package org.smartregister.p2p.presenter;

import android.Manifest;
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
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.sync.DiscoveredDevice;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
public class P2PSenderPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;

    private P2PSenderPresenter p2PSenderPresenter;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        p2PSenderPresenter = Mockito.spy(new P2PSenderPresenter(view, interactor));
    }

    @Test
    public void onSendButtonClickedShouldCallPrepareDiscovering() {

        p2PSenderPresenter.onSendButtonClicked();

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .prepareForDiscovering(false);
    }

    @Test
    public void prepareDiscoveringShouldCallStartDiscoveringModeWhenPermissionsGrantedAndLocationEnabled() {
        List<String> unauthorizedPermissions = new ArrayList<>();

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doReturn(true)
                .when(view)
                .isLocationEnabled();

        p2PSenderPresenter.prepareForDiscovering(false);
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
    }

    @Test
    public void prepareDiscoveringShouldCallRequestPermissionsWhenPermissionsNotGrantedAndNotReturningFromRequestingPermissions() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        p2PSenderPresenter.prepareForDiscovering(false);
        Mockito.verify(view, Mockito.times(1))
                .requestPermissions(unauthorizedPermissions);
        Mockito.verify(view, Mockito.times(1))
                .addOnActivityRequestPermissionHandler(Mockito.any(OnActivityRequestPermissionHandler.class));
    }

    @Test
    public void prepareDiscoveringShouldNotCallRequestPermissionsWhenPermissionsNotGrantedAndReturningFromRequestionPermissions() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        p2PSenderPresenter.prepareForDiscovering(true);
        Mockito.verify(view, Mockito.times(0))
                .requestPermissions(unauthorizedPermissions);
    }

    @Test
    public void prepareDiscoveringShouldRequestEnableLocationWhenPermissionsGrantedAndLocationNotEnabled() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doReturn(false)
                .when(view)
                .isLocationEnabled();

        p2PSenderPresenter.prepareForDiscovering(false);

        Mockito.verify(view, Mockito.times(1))
                .requestEnableLocation(Mockito.any(P2pModeSelectContract.View.OnLocationEnabled.class));
    }

    @Test
    public void prepareDiscoveringShouldCallItselfAfterPermissionsGrantedExplicitlyByUserOnView() {
        final ArrayList<Object> sensitiveObjects = new ArrayList<>();
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                OnActivityRequestPermissionHandler onActivityRequestPermissionHandler = invocation.getArgument(0);
                sensitiveObjects.add(onActivityRequestPermissionHandler);
                onActivityRequestPermissionHandler.handlePermissionResult(new String[] {""}, new int[]{0});
                return null;
            }
        }).when(view)
                .addOnActivityRequestPermissionHandler(Mockito.any(OnActivityRequestPermissionHandler.class));

        p2PSenderPresenter.prepareForDiscovering(false);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .prepareForDiscovering(true);
        Mockito.verify(view, Mockito.times(1))
                .removeOnActivityRequestPermissionHandler((OnActivityRequestPermissionHandler) sensitiveObjects.get(0));
    }

    @Test
    public void onDiscoveringFailedShouldResetUI() {
        p2PSenderPresenter.onDiscoveringFailed(new Exception());

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

        p2PSenderPresenter.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(interactor, Mockito.times(1))
                .stopDiscovering();

        Mockito.verify(view, Mockito.times(1))
                .removeDiscoveringProgressDialog();

        Mockito.verify(interactor, Mockito.times(1))
                .requestConnection(ArgumentMatchers.eq(endpointId)
                        , Mockito.any(OnResultCallback.class)
                        , Mockito.any(ConnectionLifecycleCallback.class));

        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onDeviceFoundShouldDoNothingWhenAnotherConnectionIsBeingNegotiated() {
        String endpointId = "id";

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onDeviceFound(endpointId, discoveredEndpointInfo);

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

        P2PSenderPresenter spiedCallback = Mockito.spy(p2PSenderPresenter);

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

        P2PSenderPresenter spiedCallback = Mockito.spy(p2PSenderPresenter);

        spiedCallback.onDeviceFound(endpointId, discoveredEndpointInfo);

        Mockito.verify(spiedCallback, Mockito.times(1))
                .onRequestConnectionFailed(Mockito.any(Exception.class));
    }

    @Test
    public void onRequestConnectionFailedShouldResetStateAndStartDiscoveringMode() {
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onRequestConnectionFailed(new Exception());

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
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

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionInitiated(endpointId, connectionInfo);

        assertEquals(connectionInfo, ((DiscoveredDevice)ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"))
                .getConnectionInfo());

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeScanningDialog(Mockito.any(QRCodeScanningDialog.QRCodeScanDialogCallback.class));
    }

    @Test
    public void onConnectionInitiatedShouldDoNothingWhenNotNegotiatingDeviceConnection() {
        String endpointId = "id";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionInitiated(endpointId, connectionInfo);

        Mockito.verify(view, Mockito.times(0))
                .showQRCodeScanningDialog(Mockito.any(QRCodeScanningDialog.QRCodeScanDialogCallback.class));
    }

    @Test
    public void onAuthenticationSuccessfulShouldAcceptConnectionWhenNegotiatingDeviceConnection() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
        p2PSenderPresenter.onAuthenticationSuccessful();

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

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onAuthenticationFailed(new Exception());

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

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onAuthenticationCancelled("");

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionAcceptedShouldRegisterReceiverDeviceWithInteractor() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);

        p2PSenderPresenter.onConnectionAccepted(endpointId, connectionResolution);

        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionRejectedShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionRejected(endpointId, connectionResolution);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onConnectionUnknownErrorShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionUnknownError(endpointId, connectionResolution);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onConnectionBrokenShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionBroken(endpointId);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onDisconnectedShouldResetStateAndStartDiscoveringMode() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onDisconnected(endpointId);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDiscoveringMode();
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }
}