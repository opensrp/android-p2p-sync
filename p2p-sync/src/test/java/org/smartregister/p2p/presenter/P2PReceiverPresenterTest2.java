package org.smartregister.p2p.presenter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.AppDatabase;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.model.dao.P2pReceivedHistoryDao;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.model.dao.SendingDeviceDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.shadows.ShadowTasker;
import org.smartregister.p2p.util.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This test has ShadowTasker enabled. This enables AsyncTasks to run in this test
 *
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class, ShadowTasker.class})
public class P2PReceiverPresenterTest2 {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.Interactor interactor;
    @Mock
    private P2PAuthorizationService p2PAuthorizationService;

    private P2PReceiverPresenter p2PReceiverPresenter;

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application
                ,"password", "username", p2PAuthorizationService
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

        p2PReceiverPresenter = Mockito.spy(new P2PReceiverPresenter(view, interactor));
    }

    @Test
    public void checkIfDeviceKeyHasChangedShouldClearDeviceHistoryAndUpdateDeviceKeyWhenAppLifetimeKeyIsDifferent() {
        String deviceId = "deviceId";
        String appLifetimeKey = "app-lifetime-key";
        String endpointId = "endpoint-id";

        SendingDevice sendingDevice = new SendingDevice();
        sendingDevice.setAppLifetimeKey("some-other-key");
        sendingDevice.setDeviceId(deviceId);

        Map<String, Object> deviceDetails = new HashMap<>();
        deviceDetails.put(Constants.BasicDeviceDetails.KEY_DEVICE_ID, deviceId);
        deviceDetails.put(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY, appLifetimeKey);

        AppDatabase appDatabase = Mockito.spy(AppDatabase.getInstance(RuntimeEnvironment.application, "password"));

        ShadowAppDatabase.setInstance(appDatabase);

        SendingDeviceDao sendingDeviceDao = Mockito.mock(SendingDeviceDao.class);
        P2pReceivedHistoryDao p2pReceivedHistoryDao = Mockito.mock(P2pReceivedHistoryDao.class);

        Mockito.doReturn(sendingDeviceDao)
                .when(appDatabase)
                .sendingDeviceDao();

        Mockito.doReturn(p2pReceivedHistoryDao)
                .when(appDatabase)
                .p2pReceivedHistoryDao();

        Mockito.doReturn(sendingDevice)
                .when(sendingDeviceDao)
                .getSendingDevice(Mockito.eq(deviceId));

        ReflectionHelpers.callInstanceMethod(p2PReceiverPresenter, "checkIfDeviceKeyHasChanged"
                , ReflectionHelpers.ClassParameter.from(Map.class, deviceDetails)
                , ReflectionHelpers.ClassParameter.from(String.class, endpointId));

        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .clearDeviceHistoryAndUpdateDeviceKey(Mockito.eq(sendingDevice), Mockito.eq(appLifetimeKey));
        Mockito.verify(p2PReceiverPresenter, Mockito.times(1))
                .sendLastReceivedRecords(Mockito.any(List.class));
    }

}