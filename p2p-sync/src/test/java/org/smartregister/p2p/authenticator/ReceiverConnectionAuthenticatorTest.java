package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeGeneratorDialog;
import org.smartregister.p2p.sync.DiscoveredDevice;

import java.util.ArrayList;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 19/03/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ReceiverConnectionAuthenticatorTest {

    @Mock
    private P2pModeSelectContract.View view;
    @Mock
    private P2pModeSelectContract.BasePresenter basePresenter;
    @Mock
    private P2pModeSelectContract.Interactor interactor;

    private ReceiverConnectionAuthenticator receiverConnectionAuthenticator;

    @Before
    public void setUp() {
        receiverConnectionAuthenticator = new ReceiverConnectionAuthenticator(view, interactor, basePresenter);
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsNull() {
        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", Mockito.mock(DiscoveredEndpointInfo.class));
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);
        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(0);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.any(Exception.class));

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticateShouldCallAuthenticationFailedCallbackWhenConnectionInfoIsValidAndConnectionIsNotIncoming() {
        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(false)
                .when(connectionInfo)
                .isIncomingConnection();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);
        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(0);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.any(Exception.class));

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.any(Exception.class));
        Assert.assertEquals("DiscoveredDevice information passed is invalid", ((Exception) innerClassResults.get(0)).getMessage());
    }

    @Test
    public void authenticateShouldShowQrCodeDialogWhenConnectionInfoIsValidAndConnectionIsIncoming() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(view, Mockito.times(1))
                .showQRCodeGeneratorDialog(ArgumentMatchers.eq(authenticationCode)
                        , ArgumentMatchers.eq(deviceName)
                        , Mockito.any(QRCodeGeneratorDialog.QRCodeAuthenticationCallback.class));
    }

    @Test
    public void authenticationShouldCallAuthenticationSuccessfulCallbackWhenQRCodeAuthenticationIsAccepted() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeGeneratorDialog.QRCodeAuthenticationCallback qrCodeAuthenticationCallback = invocation.getArgument(2);
                qrCodeAuthenticationCallback.onAccepted(Mockito.mock(DialogInterface.class));
                return null;
            }
        })
                .when(view)
                .showQRCodeGeneratorDialog(Mockito.anyString()
                        , Mockito.anyString()
                        , Mockito.any(QRCodeGeneratorDialog.QRCodeAuthenticationCallback.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationSuccessful();
    }

    @Test
    public void authenticationShouldCallAuthenticationFailedCallbackWhenQRCodeAuthenticationIsRejected() {
        String authenticationCode = "iowejncCJD";
        String deviceName = "SAMSUNG SM78";

        ConnectionInfo connectionInfo = Mockito.mock(ConnectionInfo.class);

        Mockito.doReturn(true)
                .when(connectionInfo)
                .isIncomingConnection();

        Mockito.doReturn(deviceName)
                .when(connectionInfo)
                .getEndpointName();

        Mockito.doReturn(authenticationCode)
                .when(connectionInfo)
                .getAuthenticationToken();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                QRCodeGeneratorDialog.QRCodeAuthenticationCallback qrCodeAuthenticationCallback = invocation.getArgument(2);
                qrCodeAuthenticationCallback.onRejected(Mockito.mock(DialogInterface.class));
                return null;
            }
        })
                .when(view)
                .showQRCodeGeneratorDialog(Mockito.anyString()
                        , Mockito.anyString()
                        , Mockito.any(QRCodeGeneratorDialog.QRCodeAuthenticationCallback.class));

        DiscoveredDevice discoveredDevice = new DiscoveredDevice("id", connectionInfo);
        BaseSyncConnectionAuthenticator.AuthenticationCallback authenticationCallback = Mockito.mock(BaseSyncConnectionAuthenticator.AuthenticationCallback.class);

        final ArrayList<Object> innerClassResults = new ArrayList<>();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Exception e = invocation.getArgument(0);
                innerClassResults.add(e);

                return null;
            }
        }).when(authenticationCallback)
                .onAuthenticationFailed(Mockito.any(Exception.class));

        receiverConnectionAuthenticator.authenticate(discoveredDevice, authenticationCallback);

        Mockito.verify(authenticationCallback, Mockito.times(1))
                .onAuthenticationFailed(Mockito.any(Exception.class));
        Assert.assertEquals("User rejected the connection", ((Exception) innerClassResults.get(0)).getMessage());
    }


}