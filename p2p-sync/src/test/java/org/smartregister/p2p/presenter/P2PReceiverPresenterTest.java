package org.smartregister.p2p.presenter;

import android.Manifest;
import android.content.DialogInterface;
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
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
public class P2PReceiverPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;

    private P2PReceiverPresenter p2PReceiverPresenter;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(RuntimeEnvironment.application)
                .when(view)
                .getContext();

        p2PReceiverPresenter = Mockito.spy(new P2PReceiverPresenter(view, interactor));
    }

    @Test
    public void onReceiveButtonClickedShouldCallPrepareForAdvertising() {
        p2PReceiverPresenter.onReceiveButtonClicked();

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .prepareForAdvertising(false);
    }

    @Test
    public void prepareAdvertisingShouldRequestPermissionsWhenPermissionsAreNotGranted() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        p2PReceiverPresenter.prepareForAdvertising(false);

        Mockito.verify(view, Mockito.times(1))
                .requestPermissions(unauthorizedPermissions);
        Mockito.verify(view, Mockito.times(1))
                .addOnActivityRequestPermissionHandler(Mockito.any(OnActivityRequestPermissionHandler.class));
    }

    @Test
    public void prepareAdvertisingShouldCallItselfWhenPermissionsGrantedExplicitlyByUserOnView() {
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

                onActivityRequestPermissionHandler.handlePermissionResult(new String[]{""}, new int[]{0});
                return null;
            }
        }).when(view)
                .addOnActivityRequestPermissionHandler(Mockito.any(OnActivityRequestPermissionHandler.class));

        p2PReceiverPresenter.prepareForAdvertising(false);

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .prepareForAdvertising(true);
        Mockito.verify(view, Mockito.times(1))
                .removeOnActivityRequestPermissionHandler((OnActivityRequestPermissionHandler) sensitiveObjects.get(0));
    }

    @Test
    public void prepareAdvertisingShouldCallStartAdvertisingModeWhenPermissionsAreGrantedAndLocationEnabled() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doReturn(true)
                .when(view)
                .isLocationEnabled();

        p2PReceiverPresenter.prepareForAdvertising(false);

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .startAdvertisingMode();
    }

    @Test
    public void prepareAdvertisingShouldRequestLocationEnableWhenPermissionsAreGrantedAndLocationDisabled() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doReturn(false)
                .when(view)
                .isLocationEnabled();

        p2PReceiverPresenter.prepareForAdvertising(false);

        Mockito.verify(view, Mockito.times(1))
                .requestEnableLocation(Mockito.any(P2pModeSelectContract.View.OnLocationEnabled.class));
    }

    @Test
    public void prepareAdvertisingShouldCallNothingWhenPermissionsAreNotGrantedAndReturnedFromRequestingPermissions() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        p2PReceiverPresenter.prepareForAdvertising(false);

        Mockito.verify(view, Mockito.times(1))
                .requestPermissions(unauthorizedPermissions);
    }

    @Test
    public void startAdvertisingModeShouldDisableButtonsWhenAdvertisingIsFalse() {
        // The interactor.advertising is false by default
        p2PReceiverPresenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(false);
    }

    @Test
    public void startAdvertisingModeShouldCallNothingWhenAdvertisingIsTrue() {
        Mockito.doReturn(true)
                .when(interactor)
                .isAdvertising();

        p2PReceiverPresenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(0))
                .enableSendReceiveButtons(false);
    }

    @Test
    public void startAdvertisingModeShouldShowProgressDialogBarWhenAdvertisingIsFalse() {
        // interactor.advertising is false by default
        p2PReceiverPresenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(1))
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));
    }

    @Test
    public void startAdvertisingModeShouldCallInteractorAdvertisingWhenAdvertisingIsFalse() {
        // interactor.advertising is false by default
        p2PReceiverPresenter.startAdvertisingMode();
        Mockito.verify(interactor, Mockito.times(1))
                .startAdvertising(Mockito.any(IReceiverSyncLifecycleCallback.class));
    }

    @Test
    public void cancelDialogShouldCallStopAdvertisingWhenClicked(){
        // interactor.advertising is false by default
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback = invocation.getArgument(0);
                dialogCancelCallback.onCancelClicked(new DialogInterface() {
                    @Override
                    public void cancel() {
                        // Do nothing
                    }

                    @Override
                    public void dismiss() {
                        // Do nothing
                    }
                });

                return null;
            }
        }).when(view)
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));


        p2PReceiverPresenter.startAdvertisingMode();
        Mockito.verify(interactor, Mockito.times(1))
                .stopAdvertising();
    }

    @Test
    public void cancelDialogShouldCallDialogIDismissWhenClicked(){
        // interactor.advertising is false by default
        final DialogInterface dialogInterface = Mockito.mock(DialogInterface.class);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback = invocation.getArgument(0);
                dialogCancelCallback.onCancelClicked(dialogInterface);

                return null;
            }
        }).when(view)
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));


        p2PReceiverPresenter.startAdvertisingMode();
        Mockito.verify(dialogInterface, Mockito.times(1))
                .dismiss();
    }

    @Test
    public void cancelDialogShouldEnableButtonsWhenClicked(){
        // interactor.advertising is false by default
        final DialogInterface dialogInterface = Mockito.mock(DialogInterface.class);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                P2pModeSelectContract.View.DialogCancelCallback dialogCancelCallback = invocation.getArgument(0);
                dialogCancelCallback.onCancelClicked(dialogInterface);

                return null;
            }
        }).when(view)
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));


        p2PReceiverPresenter.startAdvertisingMode();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
    }

    @Test
    public void onAdvertisingFailedShouldResetUI() {
        p2PReceiverPresenter.onAdvertisingFailed(Mockito.mock(Exception.class));

        Mockito.verify(view, Mockito.times(1))
                .removeReceiveProgressDialog();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
    }

    @Test
    public void onConnectionInitiatedShouldDoNothingWhenNoAnotherConnectionIsBeingNegotiated() {
        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", Mockito.mock(DiscoveredDevice.class));
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));
        p2PReceiverPresenter.onConnectionInitiated("id", Mockito.mock(ConnectionInfo.class));

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

        p2PReceiverPresenter.onConnectionInitiated("id", connectionInfo);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

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

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));


        p2PReceiverPresenter.onConnectionAccepted(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(interactor, Mockito.times(1))
                .connectedTo(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void onConnectionRejectedShouldRestartAdvertisingModeAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onConnectionRejected(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));
    }

    @Test
    public void onConnectionUnknownErrorShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onConnectionUnknownError(endpointId, Mockito.mock(ConnectionResolution.class));

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));
    }

    @Test
    public void onConnectionBrokenShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onConnectionBroken(endpointId);

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));
    }

    @Test
    public void onDisconnectedShouldRestartAdvertisingAndResetState() {
        String endpointId = "id";

        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onDisconnected(endpointId);

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .startAdvertisingMode();
        assertNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));
    }

    @Test
    public void onAuthenticationSuccessfulShouldAcceptConnectWhenNegotiatingConnectionWithSender() {
        String endpointId = "id";
        DiscoveredDevice discoveredDevice = Mockito.mock(DiscoveredDevice.class);

        Mockito.doReturn(endpointId)
                .when(discoveredDevice)
                .getEndpointId();

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onAuthenticationSuccessful();
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

        P2PReceiverPresenter spiedCallback = Mockito.spy(p2PReceiverPresenter);

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

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onAuthenticationFailed(new Exception());

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

        ReflectionHelpers.setField(p2PReceiverPresenter, "currentSender", discoveredDevice);
        assertNotNull(ReflectionHelpers.getField(p2PReceiverPresenter, "currentSender"));

        p2PReceiverPresenter.onAuthenticationCancelled("");

        Mockito.verify(interactor, Mockito.times(1))
                .rejectConnection(ArgumentMatchers.eq(endpointId));
    }
}