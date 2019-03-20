package org.smartregister.p2p.interactor;

import com.google.android.gms.internal.nearby.zzbd;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

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
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.shadows.Shadowzzbd;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.SenderSyncLifecycleCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {Shadowzzbd.class})
public class P2pModeSelectInteractorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private P2pModeSelectInteractor interactor;

    @Mock
    private zzbd mockedZzbd;

    private String username = "possum";

    @Before
    public void setUp() {
        interactor = new P2pModeSelectInteractor(RuntimeEnvironment.application);
        ConnectionsClient connectionsClient = ReflectionHelpers.getField(interactor, "connectionsClient");
        Shadowzzbd shadowzzbd = Shadow.extract(connectionsClient);
        shadowzzbd.setMockZzbd(mockedZzbd);

        P2PLibrary.init(new P2PLibrary.ReceiverOptions(username));
    }

    @Test
    public void getAppPackageName() {
        assertEquals("org.smartregister.p2p", interactor.getAppPackageName());
    }

    @Test
    public void stopAdvertisingShouldSetAdvertisingFalseIfAlreadyAdvertising() {
        ReflectionHelpers.setField(interactor, "advertising", true);
        interactor.stopAdvertising();
        assertFalse(interactor.isAdvertising());

        assertTrue(Shadowzzbd.instance.stopAdvertisingCalled);
    }

    @Test
    public void startAdvertisingShouldSetAdvertisingFlagToTrueWhenSuccessful() {
        interactor.startAdvertising(Mockito.mock(IReceiverSyncLifecycleCallback.class));

        assertTrue((boolean) ReflectionHelpers.getField(interactor, "advertising"));
    }

    @Test
    public void startAdvertisingShouldSetAdvertisingFlagToFalseAndCallOnAdvertisingFailedWhenFailsAndNotAdvertising() {
        P2pModeSelectInteractor spiedInteractor = Mockito.spy(interactor);

        ConnectionsClient connectionsClient = Mockito.mock(ConnectionsClient.class);
        ReflectionHelpers.setField(spiedInteractor, "connectionsClient", connectionsClient);

        final Task<Void> startAdvertisingTask = Mockito.mock(Task.class);
        Mockito.doReturn(startAdvertisingTask)
                .when(connectionsClient)
                .startAdvertising(ArgumentMatchers.eq(username)
                        , ArgumentMatchers.eq("org.smartregister.p2p")
                        , ArgumentMatchers.any(ConnectionLifecycleCallback.class)
                        , ArgumentMatchers.any(AdvertisingOptions.class));

        Mockito.doReturn(startAdvertisingTask)
                .when(startAdvertisingTask)
                .addOnSuccessListener(Mockito.any(OnSuccessListener.class));
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((OnFailureListener) invocation.getArgument(0))
                        .onFailure(new Exception());

                return startAdvertisingTask;
            }
        })
                .when(startAdvertisingTask)
                .addOnFailureListener(Mockito.any(OnFailureListener.class));

        IReceiverSyncLifecycleCallback iReceiverSyncLifecycleCallback = Mockito.mock(IReceiverSyncLifecycleCallback.class);
        spiedInteractor.startAdvertising(iReceiverSyncLifecycleCallback);

        assertFalse((boolean) ReflectionHelpers.getField(spiedInteractor, "advertising"));
        Mockito.verify(iReceiverSyncLifecycleCallback, Mockito.times(1))
                .onAdvertisingFailed(Mockito.any(Exception.class));
        Mockito.verify(spiedInteractor, Mockito.times(1))
                .showToast(
                        ArgumentMatchers.eq(RuntimeEnvironment.application.getString(R.string.advertising_could_not_be_started))
                );
    }

    @Test
    public void startDiscoveringShouldChangeDiscoveringFlag() {
        interactor.startDiscovering(new SenderSyncLifecycleCallback(
                Mockito.mock(P2pModeSelectContract.View.class)
                , Mockito.mock(P2pModeSelectContract.Presenter.class)
                , interactor));

        assertTrue((boolean) ReflectionHelpers.getField(interactor, "discovering"));
    }

    @Test
    public void startDiscoveringShouldCallOnDeviceFoundWhenEndpointFound() {
        ConnectionsClient connectionsClient = Mockito.mock(ConnectionsClient.class);
        final Task<Void> startDiscoveryTask = Mockito.mock(Task.class);
        Mockito.doReturn(startDiscoveryTask)
                .when(startDiscoveryTask)
                .addOnSuccessListener(Mockito.any(OnSuccessListener.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((EndpointDiscoveryCallback) invocation.getArgument(1))
                        .onEndpointFound("id", Mockito.mock(DiscoveredEndpointInfo.class));

                return startDiscoveryTask;
            }
        })
                .when(connectionsClient)
                .startDiscovery(ArgumentMatchers.eq("org.smartregister.p2p")
                        , Mockito.any(EndpointDiscoveryCallback.class)
                        , Mockito.any(DiscoveryOptions.class));

        SenderSyncLifecycleCallback senderSyncLifecycleCallback = Mockito.mock(SenderSyncLifecycleCallback.class);

        ReflectionHelpers.setField(interactor, "connectionsClient", connectionsClient);
        interactor.startDiscovering(senderSyncLifecycleCallback);

        Mockito.verify(senderSyncLifecycleCallback, Mockito.times(1))
                .onDeviceFound(ArgumentMatchers.eq("id"), Mockito.any(DiscoveredEndpointInfo.class));
    }

    @Test
    public void startDiscoveringShouldCallOnDisconnectedWhenEndpointLost() {
        ConnectionsClient connectionsClient = Mockito.mock(ConnectionsClient.class);
        final Task<Void> startDiscoveryTask = Mockito.mock(Task.class);
        Mockito.doReturn(startDiscoveryTask)
                .when(startDiscoveryTask)
                .addOnSuccessListener(Mockito.any(OnSuccessListener.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((EndpointDiscoveryCallback) invocation.getArgument(1))
                        .onEndpointLost("id");

                return startDiscoveryTask;
            }
        })
                .when(connectionsClient)
                .startDiscovery(ArgumentMatchers.eq("org.smartregister.p2p")
                        , Mockito.any(EndpointDiscoveryCallback.class)
                        , Mockito.any(DiscoveryOptions.class));

        SenderSyncLifecycleCallback senderSyncLifecycleCallback = Mockito.mock(SenderSyncLifecycleCallback.class);

        ReflectionHelpers.setField(interactor, "connectionsClient", connectionsClient);
        interactor.startDiscovering(senderSyncLifecycleCallback);

        Mockito.verify(senderSyncLifecycleCallback, Mockito.times(1))
                .onDisconnected(ArgumentMatchers.eq("id"));
    }

    @Test
    public void stopDiscoveringShouldSetDiscoveringFalseIfAlreadyDiscovering() {
        ReflectionHelpers.setField(interactor, "discovering", true);
        interactor.stopDiscovering();
        assertFalse(interactor.isDiscovering());

        assertNotNull(Shadowzzbd.instance.methodCalls.get("stopDiscovery"));
        assertEquals(1, Shadowzzbd.instance.methodCalls.get("stopDiscovery").intValue());
    }

}