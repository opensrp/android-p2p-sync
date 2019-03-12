package org.smartregister.p2p.presenter;

import android.Manifest;
import android.content.DialogInterface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class P2pModeSelectPresenterTest {

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    private P2pModeSelectContract.Presenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new P2pModeSelectPresenter(view, interactor);
    }

    @Test
    public void onSendButtonClickedShouldCallPrepareDiscovering() {
        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);
        spiedPresenter.onSendButtonClicked();

        Mockito.verify(spiedPresenter, Mockito.times(1))
                .prepareForDiscovering(false);
    }

    @Test
    public void onReceiveButtonClickedShouldCallPrepareForAdvertising() {
        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);

        spiedPresenter.onReceiveButtonClicked();

        Mockito.verify(spiedPresenter, Mockito.times(1))
                .prepareForAdvertising(false);
    }

    @Test
    public void startAdvertisingModeShouldDisableButtonsWhenAdvertisingIsFalse() {
        // The interactor.advertising is false by default
        presenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(false);
    }

    @Test
    public void startAdvertisingModeShouldCallNothingWhenAdvertisingIsTrue() {
        Mockito.doReturn(true)
                .when(interactor)
                .isAdvertising();

        presenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(0))
                .enableSendReceiveButtons(false);
    }

    @Test
    public void startAdvertisingModeShouldShowProgressDialogBarWhenAdvertisingIsFalse() {
        // interactor.advertising is false by default
        presenter.startAdvertisingMode();

        Mockito.verify(view, Mockito.times(1))
                .showReceiveProgressDialog(Mockito.any(P2pModeSelectContract.View.DialogCancelCallback.class));
    }

    @Test
    public void startAdvertisingModeShouldCallInteractorAdvertisingWhenAdvertisingIsFalse() {
        // interactor.advertising is false by default
        presenter.startAdvertisingMode();
        Mockito.verify(interactor, Mockito.times(1))
                .startAdvertising();
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


        presenter.startAdvertisingMode();
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


        presenter.startAdvertisingMode();
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


        presenter.startAdvertisingMode();
        Mockito.verify(view, Mockito.times(1))
                .enableSendReceiveButtons(true);
    }

    @Test
    public void prepareAdvertisingShouldRequestPermissionsWhenPermissionsAreNotGranted() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        presenter.prepareForAdvertising(false);

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

        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);

        spiedPresenter.prepareForAdvertising(false);

        Mockito.verify(spiedPresenter, Mockito.times(1))
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

        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);
        spiedPresenter.prepareForAdvertising(false);

        Mockito.verify(spiedPresenter, Mockito.times(1))
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

        presenter.prepareForAdvertising(false);

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

        presenter.prepareForAdvertising(false);

        Mockito.verify(view, Mockito.times(1))
                .requestPermissions(unauthorizedPermissions);
    }

    @Test
    public void prepareDiscoveringShouldCallStartDiscoveringModeWhenPermissionsGrantedAndLocationEnabled() {
        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);

        List<String> unauthorizedPermissions = new ArrayList<>();

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        Mockito.doReturn(true)
                .when(view)
                .isLocationEnabled();

        spiedPresenter.prepareForDiscovering(false);
        Mockito.verify(spiedPresenter, Mockito.times(1))
                .startDiscoveringMode();
    }

    @Test
    public void prepareDiscoveringShouldCallRequestPermissionsWhenPermissionsNotGrantedAndNotReturningFromRequestingPermissions() {
        List<String> unauthorizedPermissions = new ArrayList<>();
        unauthorizedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        Mockito.doReturn(unauthorizedPermissions)
                .when(view)
                .getUnauthorisedPermissions();

        presenter.prepareForDiscovering(false);
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

        presenter.prepareForDiscovering(true);
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

        presenter.prepareForDiscovering(false);

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

        P2pModeSelectContract.Presenter spiedPresenter = Mockito.spy(presenter);
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

        spiedPresenter.prepareForDiscovering(false);

        Mockito.verify(spiedPresenter, Mockito.times(1))
                .prepareForDiscovering(true);
        Mockito.verify(view, Mockito.times(1))
                .removeOnActivityRequestPermissionHandler((OnActivityRequestPermissionHandler) sensitiveObjects.get(0));
    }


}