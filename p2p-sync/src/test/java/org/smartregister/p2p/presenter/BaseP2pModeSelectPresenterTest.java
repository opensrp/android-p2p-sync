package org.smartregister.p2p.presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 22/03/2019
 */

@RunWith(MockitoJUnitRunner.class)
public class BaseP2pModeSelectPresenterTest {

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
        Mockito.verify(interactor, Mockito.times(1))
                .closeAllEndpoints();
        Mockito.verify(interactor, Mockito.times(1))
                .cleanupResources();
    }

    private class P2pModeSelectPresenter extends BaseP2pModeSelectPresenter {

        protected P2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
            super(view, p2pModeSelectInteractor);
        }

        @Nullable
        @Override
        public DiscoveredDevice getCurrentPeerDevice() {
            return Mockito.mock(DiscoveredDevice.class);
        }
    }
}