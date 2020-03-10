package org.smartregister.p2p;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.SyncFinishedCallback;
import org.smartregister.p2p.contract.RecalledIdentifier;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;

public class OptionsTest {

    private P2PLibrary.Options options;

    @Mock
    private Context context;

    @Mock
    private String dbPassphrase;

    @Mock
    private String username;

    @Mock
    private P2PAuthorizationService p2PAuthorizationService;

    @Mock
    private ReceiverTransferDao receiverTransferDao;

    @Mock
    private SenderTransferDao senderTransferDao;

    @Before
    public void setUp() {
        options = new P2PLibrary.Options(context, dbPassphrase, username, p2PAuthorizationService, receiverTransferDao, senderTransferDao);
    }

    @Test
    public void testOptionsConfig() {
        Assert.assertEquals(ReflectionHelpers.getField(options, "context"), context);
        Assert.assertEquals(ReflectionHelpers.getField(options, "dbPassphrase"), dbPassphrase);
        Assert.assertEquals(ReflectionHelpers.getField(options, "username"), username);
        Assert.assertEquals(ReflectionHelpers.getField(options, "p2PAuthorizationService"), p2PAuthorizationService);
        Assert.assertEquals(ReflectionHelpers.getField(options, "receiverTransferDao"), receiverTransferDao);
        Assert.assertEquals(ReflectionHelpers.getField(options, "senderTransferDao"), senderTransferDao);

        RecalledIdentifier recalledIdentifier = Mockito.mock(RecalledIdentifier.class);
        options.setRecalledIdentifier(recalledIdentifier);
        Assert.assertEquals(options.getRecalledIdentifier(), recalledIdentifier);

        SyncFinishedCallback callback = Mockito.mock(SyncFinishedCallback.class);
        options.setSyncFinishedCallback(callback);
        Assert.assertEquals(callback, options.getSyncFinishedCallback());

        long deviceMaxRetryConnectionDuration = 12345L;
        options.setDeviceMaxRetryConnectionDuration(deviceMaxRetryConnectionDuration);
        Assert.assertEquals(deviceMaxRetryConnectionDuration, options.getDeviceMaxRetryConnectionDuration());

    }
}
