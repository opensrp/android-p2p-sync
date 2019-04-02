package org.smartregister.p2p.presenter;

import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;

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
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class})
public class P2PSenderPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    @Mock
    private P2PAuthorizationService authorizationService;

    private P2PSenderPresenter p2PSenderPresenter;

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application
                ,"password","username", authorizationService
                , Mockito.mock(ReceiverTransferDao.class), Mockito.mock(SenderTransferDao.class)));
        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int resId = invocation.getArgument(0);
                return RuntimeEnvironment.application.getString(resId);
            }
        })
                .when(view)
                .getString(Mockito.anyInt());

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
                .prepareForDiscovering(ArgumentMatchers.eq(false));
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
                .prepareForDiscovering(ArgumentMatchers.eq(false));
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
                .prepareForDiscovering(ArgumentMatchers.eq(false));
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
                .prepareForDiscovering(ArgumentMatchers.eq(false));
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onConnectionAuthorizedShouldChangeConnectionStateToAuthorized() {
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", new DiscoveredDevice("endpointid"
                , new DiscoveredEndpointInfo("endpointid", "endpoint-name")));

        p2PSenderPresenter.onConnectionAuthorized();
        
        assertEquals(ConnectionLevel.AUTHORIZED
                , ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
    }

    @Test
    public void onConnectionAuthorizationRejectedShouldResetState() {
        String endpointId = "endpointId";
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, Mockito.mock(ConnectionInfo.class));

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHENTICATED);

        Mockito.doNothing()
                .when(p2PSenderPresenter)
                .prepareForDiscovering(Mockito.anyBoolean());

        p2PSenderPresenter.onConnectionAuthorizationRejected("Incompatible app version");

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(endpointId);
        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq((String) null));
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .prepareForDiscovering(ArgumentMatchers.eq(false));

        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
    }

    @Test
    public void performAuthorizationShouldCallOnConnectionAuthorizationDetectedWhenAuthorizationPayloadIsNotBytes() {
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.STREAM)
                .when(payload)
                .getType();

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        Mockito.doReturn("name")
                .when(discoveredEndpointInfo)
                .getEndpointName();

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver"
                , new DiscoveredDevice("id", discoveredEndpointInfo));

        p2PSenderPresenter.performAuthorization(payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .onConnectionAuthorizationRejected(Mockito.anyString());
    }

    @Test
    public void performAuthorizationShouldCallOnConnectionAuthorizationDetectedWhenAuthorizationPayloadIsInvalid() {
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn("sip sup".getBytes())
                .when(payload)
                .asBytes();

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        Mockito.doReturn("name")
                .when(discoveredEndpointInfo)
                .getEndpointName();

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver"
                , new DiscoveredDevice("id", discoveredEndpointInfo));
        p2PSenderPresenter.performAuthorization(payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .onConnectionAuthorizationRejected(Mockito.anyString());
    }

    @Test
    public void performAuthorizationShouldAuthorizationServiceWhenAuthorizationPayloadIsValid() {
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Map<String, Object> authorizationDetails = new HashMap<>();
        authorizationDetails.put("app-version", 9.9);
        String payloadString = new Gson().toJson(authorizationDetails);

        Mockito.doReturn(payloadString.getBytes())
                .when(payload)
                .asBytes();

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        Mockito.doReturn("name")
                .when(discoveredEndpointInfo)
                .getEndpointName();

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver"
                , new DiscoveredDevice("id", discoveredEndpointInfo));

        p2PSenderPresenter.performAuthorization(payload);

        Mockito.verify(authorizationService, Mockito.times(1))
                .authorizeConnection(ArgumentMatchers.any(Map.class)
                        , ArgumentMatchers.any(P2PAuthorizationService.AuthorizationCallback.class));
    }

    @Test
    public void onPayloadReceivedShouldCallPerformAuthorizationWhenConnectionLevelIsAuthenticated() {
        String endpointId = "endpoint id";

        Payload payload = Mockito.mock(Payload.class);

        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHENTICATED);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        Mockito.doReturn("name")
                .when(discoveredEndpointInfo)
                .getEndpointName();

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver"
                , new DiscoveredDevice("id", discoveredEndpointInfo));
        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .performAuthorization(ArgumentMatchers.eq(payload));
    }

    @Test
    public void onPayloadTransferUpdateShouldChangeConnectionLevelToSentHashKeyWhenHashKeyPayloadStatusUpdateIsSuccess() {
        long payloadId = 9293;
        String endpointId = "endpointid";

        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(PayloadTransferUpdate.Status.SUCCESS)
                .when(update)
                .getStatus();

        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        ReflectionHelpers.setField(p2PSenderPresenter, "hashKeyPayloadId", payloadId);
        p2PSenderPresenter.onPayloadTransferUpdate(endpointId, update);

        assertEquals(ConnectionLevel.SENT_HASH_KEY, ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
    }

    @Test
    public void onPayloadTransferUpdateShouldResetHashKeyPayloadIdWhenHashKeyPayloadStatusUpdateIsFailure() {
        long payloadId = 9293;
        String endpointId = "endpointid";

        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(PayloadTransferUpdate.Status.FAILURE)
                .when(update)
                .getStatus();

        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        ReflectionHelpers.setField(p2PSenderPresenter, "hashKeyPayloadId", payloadId);
        p2PSenderPresenter.onPayloadTransferUpdate(endpointId, update);

        assertEquals(0l, ReflectionHelpers.getField(p2PSenderPresenter, "hashKeyPayloadId"));
    }
}