package org.smartregister.p2p.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.Manifest;
import android.content.DialogInterface;

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
import org.smartregister.p2p.R;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.SkipQRScanDialog;
import org.smartregister.p2p.fragment.ErrorFragment;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.fragment.SyncProgressFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.shadows.ShadowTasker;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.sync.handler.SyncSenderHandler;
import org.smartregister.p2p.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class, ShadowTasker.class})
public class P2PSenderPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    @Mock
    private P2PAuthorizationService authorizationService;

    @Mock
    private ReceiverTransferDao receiverTransferDao;

    @Mock
    private SenderTransferDao senderTransferDao;

    private P2PSenderPresenter p2PSenderPresenter;

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application
                ,"password","username", authorizationService
                , receiverTransferDao, senderTransferDao));
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

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(ArgumentMatchers.eq(false));

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
    public void onConnectionInitiatedShouldUpdateDiscoveredDeviceInfoAndAcceptEndpointConnection() {
        String endpointId = "id";
        String deviceName = "Samsung SMT7834";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionInitiated(endpointId, connectionInfo);

        assertEquals(connectionInfo, ((DiscoveredDevice)ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"))
                .getConnectionInfo());

        Mockito.verify(interactor, Mockito.times(1))
                .acceptConnection(Mockito.eq(endpointId), Mockito.any(PayloadCallback.class));
    }

    @Test
    public void onConnectionInitiatedShouldDoNothingWhenNotNegotiatingDeviceConnection() {
        String endpointId = "id";
        String deviceName = "SAMSUNG SMT678";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionInitiated(endpointId, connectionInfo);

        Mockito.verify(view, Mockito.times(0))
                .showQRCodeScanningFragment(Mockito.eq(deviceName)
                        , Mockito.any(QRCodeScanningFragment.QRCodeScanDialogCallback.class));
    }

    @Test
    public void onAuthenticationSuccessfulShouldCallStartDeviceAuthorizationAndChangeConnectionLevel() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
        p2PSenderPresenter.onAuthenticationSuccessful();

        assertEquals(ConnectionLevel.AUTHENTICATED, ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startDeviceAuthorization(Mockito.eq(endpointId));
    }

    @Test
    public void onAuthenticationFailedShouldDisconnectFromEndpoint() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onAuthenticationFailed("reason", new Exception());

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onAuthenticationCancelledShouldDisconnectFromEndpointAndCallRejectDeviceOnAuthentication() {
        String endpointId = "id";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onAuthenticationCancelled("");

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(ArgumentMatchers.eq(endpointId));
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .rejectDeviceOnAuthentication(Mockito.eq(endpointId));
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
    public void onConnectionUnknownErrorShouldResetStateAndShowErrorFragment() {
        String endpointId = "id";
        ConnectionResolution connectionResolution = Mockito.mock(ConnectionResolution.class);
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        p2PSenderPresenter.onConnectionUnknownError(endpointId, connectionResolution);

        Mockito.verify(view, Mockito.times(1))
                .showErrorFragment(Mockito.eq(view.getString(R.string.connection_lost))
                        , Mockito.anyString()
                        , Mockito.any(ErrorFragment.OnOkClickCallback.class));
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onConnectionBrokenShouldResetStateAndShowSyncCompleteFragmentWithFailureStatus() {
        String endpointId = "id";
        String deviceName = "samsung SMT734";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        // Add the syncSenderHandler
        SyncSenderHandler syncSenderHandler = Mockito.mock(SyncSenderHandler.class);
        Mockito.doReturn(new HashMap<String, Long>())
                .when(syncSenderHandler)
                .getTransferProgress();
        ReflectionHelpers.setField(p2PSenderPresenter, "syncSenderHandler", syncSenderHandler);
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.SENT_RECEIVED_HISTORY);

        p2PSenderPresenter.onConnectionBroken(endpointId);

        Mockito.verify(view, Mockito.times(1))
                .showSyncCompleteFragment(ArgumentMatchers.eq(false)
                        , Mockito.eq(deviceName)
                        , Mockito.any(SyncCompleteTransferFragment.OnCloseClickListener.class)
                        , Mockito.anyString()
                        , Mockito.eq(true));
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .errorOccurredSync(Mockito.any(Exception.class));

        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
    }

    @Test
    public void onConnectionBrokenShouldCallDisconnectFromEndpointAndShowErrorFragmentWhenConnectionLevelIsNotSentReceivedHistory() {
        String endpointId = "id";
        String deviceName = "samsung SMT734";
        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, discoveredEndpointInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));

        // Add the syncSenderHandler
        SyncSenderHandler syncSenderHandler = Mockito.mock(SyncSenderHandler.class);
        Mockito.doReturn(new HashMap<String, Long>())
                .when(syncSenderHandler)
                .getTransferProgress();
        ReflectionHelpers.setField(p2PSenderPresenter, "syncSenderHandler", syncSenderHandler);
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHORIZED);

        p2PSenderPresenter.onConnectionBroken(endpointId);

        Mockito.verify(view, Mockito.times(1))
                .showErrorFragment(Mockito.anyString()
                        , Mockito.anyString()
                        , Mockito.any(ErrorFragment.OnOkClickCallback.class));
        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(Mockito.eq(endpointId));

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
    public void onConnectionAuthorizedShouldCallViewShowDevicesConnectedFragment() {
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", new DiscoveredDevice("endpointid"
                , new DiscoveredEndpointInfo("endpointid", "endpoint-name")));

        p2PSenderPresenter.onConnectionAuthorized();
        Mockito.verify(view, Mockito.times(1))
                .showDevicesConnectedFragment(Mockito.any(P2pModeSelectContract.View.OnStartTransferClicked.class));
    }

    @Test
    public void onConnectionAuthorizationRejectedShouldResetStateWhenCurrentSenderIsNotNull() {
        String endpointId = "endpointId";
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, connectionInfo);

        ReflectionHelpers.setField(p2PSenderPresenter, "currentReceiver", discoveredDevice);
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHENTICATED);

        Mockito.doNothing()
                .when(p2PSenderPresenter)
                .prepareForDiscovering(Mockito.anyBoolean());

        Mockito.doReturn("name")
                .when(connectionInfo)
                .getEndpointName();

        p2PSenderPresenter.onConnectionAuthorizationRejected("Incompatible app version");

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(endpointId);
        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq((String) null));
        Mockito.verify(view, Mockito.times(1))
                .showErrorFragment(Mockito.eq(view.getString(R.string.authorization_failed))
                        , Mockito.anyString(), Mockito.any(ErrorFragment.OnOkClickCallback.class));

        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "currentReceiver"));
        assertNull(ReflectionHelpers.getField(p2PSenderPresenter, "connectionLevel"));
    }

    @Test
    public void onConnectionAuthorizationRejectedShouldResetStateWhenCurrentSenderIsNull() {
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHENTICATED);

        Mockito.doNothing()
                .when(p2PSenderPresenter)
                .prepareForDiscovering(Mockito.anyBoolean());

        Mockito.doReturn("name")
                .when(connectionInfo)
                .getEndpointName();

        p2PSenderPresenter.onConnectionAuthorizationRejected("Incompatible app version");

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .prepareForDiscovering(ArgumentMatchers.eq(false));
        Mockito.verify(view, Mockito.times(1))
                .dismissAllDialogs();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(ArgumentMatchers.eq(true));

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

        assertEquals(Optional.of(0L), ReflectionHelpers.getField(p2PSenderPresenter, "hashKeyPayloadId"));
    }

    @Test
    public void onPayloadTransferUpdateShouldCallShowSyncCompleteFragmentWhenSyncCompleteConnectionSignalPayloadIsTransferredToReceiver() {
        String deviceName = "samsung SMT789";
        long payloadId = 9293;
        String endpointId = "endpointid";

        PayloadTransferUpdate update = Mockito.mock(PayloadTransferUpdate.class);

        Mockito.doReturn(payloadId)
                .when(interactor)
                .sendMessage(ArgumentMatchers.eq(Constants.Connection.SYNC_COMPLETE));

        Mockito.doReturn(PayloadTransferUpdate.Status.SUCCESS)
                .when(update)
                .getStatus();

        Mockito.doReturn(payloadId)
                .when(update)
                .getPayloadId();

        SyncSenderHandler syncSenderHandler = Mockito.mock(SyncSenderHandler.class);

        Mockito.doReturn(new HashMap<String, Long>())
                .when(syncSenderHandler)
                .getTransferProgress();

        ReflectionHelpers.setField(p2PSenderPresenter, "syncSenderHandler", syncSenderHandler);

        DiscoveredEndpointInfo discoveredEndpointInfo = Mockito.mock(DiscoveredEndpointInfo.class);

        Mockito.doReturn(deviceName)
                .when(discoveredEndpointInfo)
                .getEndpointName();

        p2PSenderPresenter.setCurrentDevice(new DiscoveredDevice(endpointId, discoveredEndpointInfo));

        p2PSenderPresenter.sendSyncComplete();
        p2PSenderPresenter.onPayloadTransferUpdate(endpointId, update);

        Mockito.verify(view, Mockito.times(1))
                .showSyncCompleteFragment(Mockito.eq(true)
                        , Mockito.eq(deviceName)
                        , Mockito.any(SyncCompleteTransferFragment.OnCloseClickListener.class)
                        , Mockito.anyString()
                        , Mockito.eq(true));
    }

    @Test
    public void startDiscoveringModeShouldCallKeepScreenOnWithTrue() {
        p2PSenderPresenter.startDiscoveringMode();

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(ArgumentMatchers.eq(true));
    }

    @Test
    public void startDiscoveringModeShouldCallKeepScreenOnWithFalseWhenProgressDialogCancelButtonIsClicked() {
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                P2pModeSelectContract.View.DialogCancelCallback cancelCallback = invocation.getArgument(0);

                cancelCallback.onCancelClicked(Mockito.mock(DialogInterface.class));
                return null;
            }
        }).when(view)
                .showDiscoveringProgressDialog(ArgumentMatchers.any(P2pModeSelectContract.View.DialogCancelCallback.class));

        p2PSenderPresenter.startDiscoveringMode();

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(ArgumentMatchers.eq(false));
    }

    @Test
    public void setCurrentDeviceShouldCallKeepScreenOnWithFalseWhenGivenNullDevice() {
        p2PSenderPresenter.setCurrentDevice(null);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(ArgumentMatchers.eq(false));
    }

    @Test
    public void setCurrentDeviceShouldCallKeepScreenOnWithTrueWhenGivenNonNullDevice() {
        p2PSenderPresenter.setCurrentDevice(Mockito.mock(DiscoveredDevice.class));

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(ArgumentMatchers.eq(true));
    }

    @Test
    public void sendManifestShouldCallSendMessageWhenCurrentPeerDeviceIsNotNull() {
        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(89l, ".json", new DataType("somedata", DataType.Type.NON_MEDIA, 1), 1);
        p2PSenderPresenter.setCurrentDevice(Mockito.mock(DiscoveredDevice.class));
        p2PSenderPresenter.sendManifest(syncPackageManifest);

        Mockito.verify(interactor, Mockito.times(1))
                .sendMessage(Mockito.anyString());
    }

    @Test
    public void sendPayloadShouldCallInteractorSendPayloadWhenCurrentPeerDeviceIsNotNull() {
        p2PSenderPresenter.setCurrentDevice(Mockito.mock(DiscoveredDevice.class));
        Payload payload = Mockito.mock(Payload.class);

        p2PSenderPresenter.sendPayload(payload);

        Mockito.verify(interactor, Mockito.times(1))
                .sendPayload(ArgumentMatchers.eq(payload));
    }

    @Test
    public void errorOccurredSyncShouldCallResetStateAndShowSyncCompleteFragmentWithFailureStatusWhenCurrentPeerDeviceIsNotNull() {
        String endpointId = "9sdjskjdjksd";
        String deviceName = "john travolter";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);
        p2PSenderPresenter.setCurrentDevice(discoveredDevice);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        Mockito.doReturn(deviceName)
                .when(discoveredDevice)
                .getEndpointName();

        SyncSenderHandler syncSenderHandler = Mockito.mock(SyncSenderHandler.class);
        Mockito.doReturn(new HashMap<String, Long>())
                .when(syncSenderHandler)
                .getTransferProgress();

        ReflectionHelpers.setField(p2PSenderPresenter, "syncSenderHandler", syncSenderHandler);

        p2PSenderPresenter.errorOccurredSync(new Exception("some error"));

        Mockito.verify(view, Mockito.times(1))
                .showSyncCompleteFragment(Mockito.eq(false)
                        , Mockito.eq(deviceName)
                        , Mockito.any(SyncCompleteTransferFragment.OnCloseClickListener.class)
                        , Mockito.anyString()
                        , Mockito.eq(true));
        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(Mockito.eq(endpointId));
        Mockito.verify(view, Mockito.times(1))
                .dismissAllDialogs();
    }

    @Test
    public void processReceivedHistoryShouldCallSendSyncCompleteWhenDataTypesIsNull() {
        String endpointId = "89283wklsdf";
        Payload payload = Mockito.mock(Payload.class);

        ArrayList<P2pReceivedHistory> receivedHistory = new ArrayList<>();
        receivedHistory.add(new P2pReceivedHistory());

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(new Gson().toJson(receivedHistory).getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(null)
                .when(senderTransferDao)
                .getDataTypes();

        SyncSenderHandler syncSenderHandler = Mockito.mock(SyncSenderHandler.class);

        Mockito.doReturn(new HashMap<String, Long>())
                .when(syncSenderHandler)
                .getTransferProgress();

        ReflectionHelpers.setField(p2PSenderPresenter, "syncSenderHandler", syncSenderHandler);

        p2PSenderPresenter.setCurrentDevice(Mockito.mock(DiscoveredDevice.class));
        p2PSenderPresenter.processReceivedHistory(endpointId, payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .sendSyncComplete();
    }

    @Test
    public void processReceivedHistoryShouldCallStartSyncProcessWhenDataTypesIsNotNullNorEmpty() {
        String endpointId = "89283wklsdf";
        Payload payload = Mockito.mock(Payload.class);

        ArrayList<P2pReceivedHistory> receivedHistory = new ArrayList<>();
        receivedHistory.add(new P2pReceivedHistory());

        TreeSet<DataType> dataTypes = Mockito.spy(new TreeSet<DataType>());
        dataTypes.add(new DataType("sample-type", DataType.Type.NON_MEDIA, 2));

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(new Gson().toJson(receivedHistory).getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(dataTypes)
                .when(senderTransferDao)
                .getDataTypes();

        p2PSenderPresenter.setCurrentDevice(Mockito.mock(DiscoveredDevice.class));
        p2PSenderPresenter.processReceivedHistory(endpointId, payload);

        Mockito.verify(dataTypes, Mockito.times(1))
                .first();
    }

    @Test
    public void sendAuthorizationDetailsShouldCallInteractorSendMessage() {
        String appVersion = "0.1.0";
        HashMap<String, Object> authorizationDetails = new HashMap<>();
        authorizationDetails.put("app-version", appVersion);

        p2PSenderPresenter.sendAuthorizationDetails(authorizationDetails);

        Mockito.verify(interactor, Mockito.times(1))
                .sendMessage(ArgumentMatchers.contains(appVersion));
    }

    @Test
    public void onPayloadReceivedShouldCallProcessReceivedHistoryWhenConnectionLevelIsSentHashKey() {
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.SENT_HASH_KEY);
        String endpointId = "endpointid";
        Payload payload = Mockito.mock(Payload.class);

        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .processReceivedHistory(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));
    }

    @Test
    public void onPayloadReceivedShouldCallProcessPayloadWhenConnectionLevelIsReceiptOfReceivedHistory() {
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY);
        String endpointId = "endpointid";
        Payload payload = Mockito.mock(Payload.class);

        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .processPayload(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));
    }

    @Test
    public void onPayloadReceivedShouldCallStartTransferWhenConnectionLevelIsAuthorizedAndConnectionCommandIsStartTransfer() {
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.AUTHORIZED);
        String endpointId = "endpointid";
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();
        Mockito.doReturn(Constants.Connection.START_TRANSFER.getBytes())
                .when(payload)
                .asBytes();

        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .startTransfer();
        Mockito.verify(view, Mockito.times(1))
                .showSyncProgressFragment(Mockito.anyString(), Mockito.any(SyncProgressFragment.SyncProgressDialogCallback.class));
    }

    @Test
    public void onPayloadReceivedShouldShowSkipQRScanDialogWhenConnectionLevelIsConnectBeforeAuthenticateAndConnectionCommandIsSkipQRCodeScan() {
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE);
        String endpointId = "endpointid";
        String deviceName = "john travolter";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);
        p2PSenderPresenter.setCurrentDevice(discoveredDevice);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        Mockito.doReturn(deviceName)
                .when(discoveredDevice)
                .getEndpointName();
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();
        Mockito.doReturn(Constants.Connection.SKIP_QR_CODE_SCAN.getBytes())
                .when(payload)
                .asBytes();

        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(view, Mockito.times(1))
                .showSkipQRScanDialog(Mockito.eq(Constants.PeerStatus.SENDER)
                        , Mockito.eq(deviceName)
                        , Mockito.any(SkipQRScanDialog.SkipDialogCallback.class));
        Mockito.verify(view, Mockito.times(1))
                .removeQRCodeScanningFragment();
    }

    @Test
    public void onPayloadReceivedShouldCallOnAuthenticationSuccessfulWhenConnectionLevelIsConnectBeforeAuthenticateAndConnectionCommandIsConnectionAccept() {
        ReflectionHelpers.setField(p2PSenderPresenter, "connectionLevel", ConnectionLevel.CONNECT_BEFORE_AUTHENTICATE);
        String endpointId = "endpointid";
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();
        Mockito.doReturn(Constants.Connection.CONNECTION_ACCEPT.getBytes())
                .when(payload)
                .asBytes();

        p2PSenderPresenter.onPayloadReceived(endpointId, payload);

        Mockito.verify(view, Mockito.times(1))
                .removeConnectingDialog();
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .onAuthenticationSuccessful();
    }


    @Test
    public void startDiscoveringShouldCallStopDiscoveringWhenConnectionTimeoutIsCalled() {

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Mockito.doReturn(true)
                        .when(interactor)
                        .isDiscovering();

                P2pModeSelectContract.BasePresenter.OnConnectionTimeout onConnectionTimeout = invocation.getArgument(0);
                onConnectionTimeout.connectionTimeout(Constants.CONNECTION_TIMEOUT_SECONDS, null);

                return null;
            }
        })
                .when(p2PSenderPresenter)
                .startConnectionTimeout(Mockito.any(P2pModeSelectContract.BasePresenter.OnConnectionTimeout.class));

        p2PSenderPresenter.startDiscoveringMode();

        Mockito.verify(interactor, Mockito.times(1))
                .stopDiscovering();
        Mockito.verify(p2PSenderPresenter, Mockito.times(1))
                .keepScreenOn(Mockito.eq(false));
        Mockito.verify(view, Mockito.times(1))
                .showErrorFragment(Mockito.eq(view.getString(R.string.no_nearby_devices_found))
                        , Mockito.anyString()
                        , Mockito.any(ErrorFragment.OnOkClickCallback.class));
    }
}