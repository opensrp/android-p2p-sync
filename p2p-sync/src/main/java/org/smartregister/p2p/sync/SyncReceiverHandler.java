package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.connection.Payload;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.model.dao.P2pReceivedHistoryDao;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.SyncDataConverterUtil;

import java.util.HashMap;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/04/2019
 */

public class SyncReceiverHandler {

    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;
    private boolean awaitingManifestReceipt = true;
    private HashMap<Long, SyncPackageManifest> awaitingPayloadManifests = new HashMap<>();

    public SyncReceiverHandler(@NonNull P2pModeSelectContract.ReceiverPresenter receiverPresenter) {
        this.receiverPresenter = receiverPresenter;
    }

    public void processPayload(@NonNull String endpointId, @NonNull Payload payload) {
        // TODO: Handle when the manifest is present in case there was an error on the sender
        // We should also give the sender the powers to decide when to close the connection and not us
        if (payload.getType() == Payload.Type.BYTES && null != payload.asBytes()
                && new String(payload.asBytes()).equals(Constants.Connection.SYNC_COMPLETE)) {
            // This will only happen after the last payload has been received on the other side
            // An abort is performed as just a disconnect
            stopTransferAndReset(false);
        } else if (awaitingManifestReceipt) {
            processManifest(endpointId, payload);
        } else {
            processRecords(endpointId, payload);
        }
    }

    public void processManifest(@NonNull String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            try {
                SyncPackageManifest syncPackageManifest = new Gson().fromJson(new String(payload.asBytes()), SyncPackageManifest.class);
                awaitingPayloadManifests.put(syncPackageManifest.getPayloadId(), syncPackageManifest);

                awaitingManifestReceipt = false;
            } catch (JsonParseException e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_received_invalid_manifest_from_endpoint), endpointId);
            }
        } else {
            Timber.e(new Exception(), receiverPresenter.getView().getString(R.string.log_receive_invalid_manifest_from_endpoint), endpointId);
        }
    }

    public void processRecords(@NonNull String endpointId, @NonNull Payload payload) {
        if (awaitingPayloadManifests.containsKey(payload.getId())) {
            awaitingManifestReceipt = true;
            SyncPackageManifest payloadManifest = awaitingPayloadManifests.get(payload.getId());

            if (payloadManifest.getDataType().getType() == DataType.Type.NON_MEDIA) {
                processNonMediaData(payload);
            } else {
                processMediaData(payload);
            }
        } else {
            Timber.e(new Exception(), receiverPresenter.getView().getString(R.string.log_received_payload_without_corresponding_manifest));
        }
    }

    private void processNonMediaData(@NonNull final Payload payload) {
        Tasker.run(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                JSONArray jsonArray = new Gson()
                        .fromJson(SyncDataConverterUtil.readInputStreamAsString(payload.asStream().asInputStream()), JSONArray.class);
                SyncPackageManifest syncPackageManifest = awaitingPayloadManifests.get(payload.getId());
                long lastRecordId = P2PLibrary.getInstance().getReceiverTransferDao()
                        .receiveJson(syncPackageManifest.getDataType(), jsonArray);

                updateLastRecord(syncPackageManifest.getDataType().getName(),lastRecordId);
                return lastRecordId;
            }
        }, new GenericAsyncTask.OnFinishedCallback<Long>() {
            @Override
            public void onSuccess(@Nullable Long result) {
                if (result != null) {
                    // We should save the last ID here and probably keep track of the next batch that we are to receive
                    awaitingPayloadManifests.remove(payload.getId());
                } else {
                    Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_media_data));
                    stopTransferAndReset(true);
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_error_occurred_processing_media_data));
                stopTransferAndReset(true);
            }
        });
    }

    private void updateLastRecord(@NonNull String entityName, long lastRecordId) {
        SendingDevice sendingDevice = receiverPresenter.getSendingDevice();
        if (sendingDevice != null) {
            P2pReceivedHistoryDao p2pReceivedHistoryDao = P2PLibrary.getInstance().getDb()
                    .p2pReceivedHistoryDao();

            P2pReceivedHistory receivedHistory = p2pReceivedHistoryDao
                    .getHistory(sendingDevice.getDeviceId(), entityName);

            if (receivedHistory == null) {
                receivedHistory = new P2pReceivedHistory();
                receivedHistory.setSendingDeviceId(sendingDevice.getDeviceId());
                receivedHistory.setEntityType(entityName);
                receivedHistory.setLastRecordId(lastRecordId);

                p2pReceivedHistoryDao
                        .addReceivedHistory(receivedHistory);
            } else {
                receivedHistory.setLastRecordId(lastRecordId);

                p2pReceivedHistoryDao
                        .updateReceivedHistory(receivedHistory);
            }
        }
    }

    private void processMediaData(@NonNull final Payload payload) {
        Tasker.run(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                SyncPackageManifest syncPackageManifest = awaitingPayloadManifests.get(payload.getId());
                long lastRecordId = P2PLibrary.getInstance().getReceiverTransferDao()
                        .receiveMultimedia(syncPackageManifest.getDataType(), payload.asStream().asInputStream());

                if (lastRecordId > -1) {
                    updateLastRecord(syncPackageManifest.getDataType().getName(),lastRecordId);
                    return lastRecordId;
                } else {
                    return null;
                }
            }
        }, new GenericAsyncTask.OnFinishedCallback<Long>() {
            @Override
            public void onSuccess(@Nullable Long result) {
                if (result != null) {
                    // We should save the last ID here and probably keep track of the next batch that we are to receive
                    awaitingPayloadManifests.remove(payload.getId());
                } else {
                    Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_non_media_data));
                    // We should not continue
                    stopTransferAndReset(true);
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_error_occurred_processing_media_data));
                // We should not continue
                stopTransferAndReset(true);
            }
        });
    }

    private void stopTransferAndReset(boolean startAdvertising) {
        DiscoveredDevice peerDevice = receiverPresenter.getCurrentPeerDevice();
        if (peerDevice != null) {
            receiverPresenter.disconnectAndReset(peerDevice.getEndpointId(), startAdvertising);
        }
    }
}
