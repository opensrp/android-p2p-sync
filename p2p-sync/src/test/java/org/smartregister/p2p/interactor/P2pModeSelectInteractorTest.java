package org.smartregister.p2p.interactor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
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

    private P2pModeSelectInteractor interactor;

    @Before
    public void setUp() {
        interactor = new P2pModeSelectInteractor(RuntimeEnvironment.application);
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
    public void startAdvertisingShouldChangeAdvertisingFlag() {
        P2PLibrary.init(new P2PLibrary.ReceiverOptions(""));
        interactor.startAdvertising(Mockito.mock(IReceiverSyncLifecycleCallback.class));

        assertTrue((boolean) ReflectionHelpers.getField(interactor, "advertising"));
    }

    @Test
    public void startDiscoveringShouldChangeDiscoveringFlag() {
        P2PLibrary.init(new P2PLibrary.ReceiverOptions(""));
        interactor.startDiscovering(new SenderSyncLifecycleCallback(
                Mockito.mock(P2pModeSelectContract.View.class)
                , Mockito.mock(P2pModeSelectContract.Presenter.class)
                , interactor));

        assertTrue((boolean) ReflectionHelpers.getField(interactor, "discovering"));
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