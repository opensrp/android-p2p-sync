package org.smartregister.p2p.presenter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.smartregister.p2p.contract.P2pModeSelectContract;

import static org.junit.Assert.*;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

@RunWith(MockitoJUnitRunner.class)
public class P2pModeSelectPresenterTest {

    @Mock
    private P2pModeSelectContract.View view;
    private P2pModeSelectContract.Presenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new P2pModeSelectPresenter(view);
    }

    @Test
    public void onSendButtonClickedShouldDisableButtons() {
        presenter.onSendButtonClicked();

        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(false);
    }

    @Test
    public void onReceiveButtonClickedShouldShowProgressDialog() {
        presenter.onReceiveButtonClicked();

        Mockito.verify(view, Mockito.times(1))
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));
    }

    @Test
    public void onReceiveButtonClickedShouldDisableButtons() {
        presenter.onReceiveButtonClicked();

        Mockito.verify(view)
                .enableSendReceiveButtons(false);
    }
}