package org.smartregister.p2p.presenter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 22/03/2019
 */

@RunWith(RobolectricTestRunner.class)
public class BaseP2pModeSelectPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    private P2pModeSelectPresenter p2PModeSelectPresenter;

    @Before
    public void setUp() throws Exception {
        p2PModeSelectPresenter = new P2pModeSelectPresenter(view, interactor);
    }

    @Test
    public void sendTextMessageShouldCallInteractorSendMessage() {
        String message = "Hello world";
        p2PModeSelectPresenter.setCurrentDevice(new DiscoveredDevice("endpointid", Mockito.mock(DiscoveredEndpointInfo.class)));
        p2PModeSelectPresenter.sendTextMessage(message);

        Mockito.verify(interactor, Mockito.times(1))
                .sendMessage(ArgumentMatchers.eq(message));
    }

    @Test
    public void onStopShouldDismissDialogsAndCallInteractorStoppingAdvertisingAndDiscovering() {
        p2PModeSelectPresenter.onStop();

        Mockito.verify(view, Mockito.times(1))
                .dismissAllDialogs();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
        Mockito.verify(interactor, Mockito.times(1))
                .stopAdvertising();
        Mockito.verify(interactor, Mockito.times(1))
                .stopDiscovering();
        Mockito.verify(interactor, Mockito.times(0))
                .disconnectFromEndpoint(Mockito.anyString());
        Mockito.verify(interactor, Mockito.times(1))
                .cleanupResources();
    }

    @Test
    public void onStopShouldDisconnectFromConnectedEndpoint() {
        String endpointId = "endpointId";
        DiscoveredDevice discoveredDevice = new DiscoveredDevice(endpointId, Mockito.mock(DiscoveredEndpointInfo.class));

        p2PModeSelectPresenter.setCurrentDevice(discoveredDevice);
        p2PModeSelectPresenter.onStop();

        Mockito.verify(interactor, Mockito.times(1))
                .disconnectFromEndpoint(ArgumentMatchers.eq(endpointId));
    }

    @Test
    public void keepScreenOnShouldAddWindowFlagWhenCalledFirstTime() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRemoveWindowFlagWhenCalledWithFalseAfterSingleTrueCall() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRetainWindowFlagWhenGivenFalseAfterMultipleTrueCalls() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @Test
    public void keepScreenOnShouldRemoveWindowFlagWhenCalledWithFalseSameNumberOfTimesCalledWithTrue() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        Mockito.doReturn(activity)
                .when(view)
                .getContext();

        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        Assert.assertTrue(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(true);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        p2PModeSelectPresenter.keepScreenOn(false);
        Assert.assertFalse(Shadows.shadowOf(activity.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    private class P2pModeSelectPresenter extends BaseP2pModeSelectPresenter {

        private DiscoveredDevice currentDevice;

        protected P2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
            super(view, p2pModeSelectInteractor);
        }

        @Nullable
        @Override
        public DiscoveredDevice getCurrentPeerDevice() {
            return currentDevice;
        }

        @Override
        public void setCurrentDevice(@Nullable DiscoveredDevice discoveredDevice) {
            currentDevice = discoveredDevice;
        }

        @Override
        public void disconnectAndReset(@NonNull String endpointId) {
            // Do nothing in this implementation
        }
    }
}