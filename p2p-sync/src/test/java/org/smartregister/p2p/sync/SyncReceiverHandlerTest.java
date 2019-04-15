package org.smartregister.p2p.sync;

import com.google.android.gms.nearby.connection.Payload;
import com.google.gson.Gson;

import org.json.JSONArray;
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
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.shadows.ShadowTasker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 04/04/2019
 */

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppDatabase.class, ShadowTasker.class})
public class SyncReceiverHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SyncReceiverHandler syncReceiverHandler;
    @Mock
    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;

    @Mock
    private P2pModeSelectContract.View view;

    @Mock
    private P2PAuthorizationService authorizationService;
    @Mock
    private SenderTransferDao senderTransferDao;
    @Mock
    private ReceiverTransferDao receiverTransferDao;

    private DataType event = new DataType("event", DataType.Type.NON_MEDIA, 1);
    private DataType client = new DataType("client", DataType.Type.NON_MEDIA, 2);
    private DataType profilePic = new DataType("profile-pic", DataType.Type.MEDIA, 3);

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application, "some password", "username"
                , authorizationService, receiverTransferDao, senderTransferDao));

        Mockito.doReturn(view)
                .when(receiverPresenter)
                .getView();

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int resId = invocation.getArgument(0);
                return RuntimeEnvironment.application.getString(resId);
            }
        })
                .when(view)
                .getString(Mockito.anyInt());

        syncReceiverHandler = Mockito.spy(new SyncReceiverHandler(receiverPresenter));
    }

    @Test
    public void processPayloadShouldCallProcessManifestWhenAwaitingManifestReceiptIsTrue() {
        String endpointId = "id";
        long payloadId = 89834l;
        Payload payload = Mockito.mock(Payload.class);

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event);

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        Mockito.doReturn(new Gson().toJson(syncPackageManifest).getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        syncReceiverHandler.processPayload(endpointId, payload);

        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .processManifest(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));

        assertFalse((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
    }

    @Test
    public void processPayloadShouldCallProcessRecordsWhenAwaitingManifestReceiptIsFalse() {
        String endpointId = "id";
        Payload payload = Mockito.mock(Payload.class);

        ReflectionHelpers.setField(syncReceiverHandler, "awaitingManifestReceipt", false);

        syncReceiverHandler.processPayload(endpointId, payload);
        Mockito.verify(syncReceiverHandler, Mockito.times(1))
                .processRecords(ArgumentMatchers.eq(endpointId), ArgumentMatchers.eq(payload));
    }

    @Test
    public void processManifestShouldRetrieveManifestAndChangeAwaitingItemWhenPayloadIsBytes() {
        String endpointId = "id";
        long payloadId = 829832l;
        Payload payload = Mockito.mock(Payload.class);

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event);
        String jsonPackageManifest = new Gson().toJson(syncPackageManifest);

        Mockito.doReturn(jsonPackageManifest.getBytes())
                .when(payload)
                .asBytes();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        syncReceiverHandler.processManifest(endpointId, payload);

        assertFalse((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
        SyncPackageManifest finalSyncPackageManifest = ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .get(payloadId);

        assertEquals(jsonPackageManifest, new Gson().toJson(finalSyncPackageManifest));
    }

    @Test
    public void processManifestShouldDoNothingWhenPayloadTypeIsStream() {
        String endpointId = "id";
        Payload payload = Mockito.mock(Payload.class);

        Mockito.doReturn(Payload.Type.STREAM)
                .when(payload)
                .getType();

        syncReceiverHandler.processManifest(endpointId, payload);

        assertTrue((boolean) ReflectionHelpers.getField(syncReceiverHandler, "awaitingManifestReceipt"));
    }

    @Test
    public void processRecordsShouldCallProcessNonMediaDataWhenPayloadCorrespondsToAwaitingManifestAndDataTypeIsNonMedia() {
        String endpointId = "id";
        long payloadId = 923823l;
        Payload payload = Mockito.mock(Payload.class);

        JSONArray jsonArray = new JSONArray();

        Payload.Stream payloadStream = Mockito.mock(Payload.Stream.class);

        Mockito.doReturn(new ByteArrayInputStream(jsonArray.toString().getBytes()))
                .when(payloadStream)
                .asInputStream();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", event);
        Mockito.doReturn(payloadStream)
                .when(payload)
                .asStream();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .put(syncPackageManifest.getPayloadId(), syncPackageManifest);
        syncReceiverHandler.processRecords(endpointId, payload);

        Mockito.verify(receiverTransferDao, Mockito.times(1))
                .receiveJson(ArgumentMatchers.eq(event), Mockito.any(JSONArray.class));
        assertEquals(0, ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests")).size());
    }

    @Test
    public void processRecordsShouldCallProcessMediaDataWhenPayloadCorrespondsToAwaitingManifestAndDataTypeIsMedia() {
        String endpointId = "id";
        long payloadId = 923823l;
        Payload payload = Mockito.mock(Payload.class);

        JSONArray jsonArray = new JSONArray();

        Payload.Stream payloadStream = Mockito.mock(Payload.Stream.class);
        InputStream is = new ByteArrayInputStream(new Gson().toJson(jsonArray).getBytes());

        Mockito.doReturn(is)
                .when(payloadStream)
                .asInputStream();

        SyncPackageManifest syncPackageManifest = new SyncPackageManifest(payloadId, "json", profilePic);
        Mockito.doReturn(payloadStream)
                .when(payload)
                .asStream();

        Mockito.doReturn(Payload.Type.BYTES)
                .when(payload)
                .getType();

        Mockito.doReturn(payloadId)
                .when(payload)
                .getId();

        ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests"))
                .put(syncPackageManifest.getPayloadId(), syncPackageManifest);
        syncReceiverHandler.processRecords(endpointId, payload);

        Mockito.verify(receiverTransferDao, Mockito.times(1))
                .receiveMultimedia(ArgumentMatchers.eq(profilePic), ArgumentMatchers.eq(is));
        assertEquals(0, ((HashMap<Long, SyncPackageManifest>) ReflectionHelpers.getField(syncReceiverHandler, "awaitingPayloadManifests")).size());
    }
}