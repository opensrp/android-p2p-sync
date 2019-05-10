package org.smartregister.p2p.sync.handler;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.SuccessfulTransferFragment;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.model.SendingDevice;
import org.smartregister.p2p.model.dao.P2pReceivedHistoryDao;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.data.ProcessedChunk;
import org.smartregister.p2p.sync.data.SyncPackageManifest;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.NearbyStorageUtil;
import org.smartregister.p2p.util.SyncDataConverterUtil;

import java.util.HashMap;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/04/2019
 */

public class SyncReceiverHandler extends BaseSyncHandler {

    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;
    private boolean awaitingManifestReceipt = true;
    private HashMap<Long, SyncPackageManifest> awaitingPayloadManifests = new HashMap<>();
    private SimpleArrayMap<Long, ProcessedChunk> awaitingPayloads = new SimpleArrayMap<>();

    public SyncReceiverHandler(@NonNull P2pModeSelectContract.ReceiverPresenter receiverPresenter) {
        this.receiverPresenter = receiverPresenter;
    }

    public void processPayload(@NonNull final String endpointId, @NonNull final Payload payload) {
        // TODO: Handle when the manifest is present in case there was an error on the sender
        // We should also give the sender the powers to decide when to close the connection and not us
        Timber.e("Received payload from endpoint %s of ID %d and Type %d", endpointId, payload.getId(), payload.getType());
        if (payload.getType() == Payload.Type.BYTES && null != payload.asBytes()
                && new String(payload.asBytes()).equals(Constants.Connection.SYNC_COMPLETE)) {
            // This will only happen after the last payload has been received on the other side
            // An abort is performed as just a disconnect
            stopTransferAndReset(false);
            receiverPresenter.getView().showSyncCompleteFragment(new SuccessfulTransferFragment.OnCloseClickListener() {
                @Override
                public void onCloseClicked() {
                    receiverPresenter.getView().showP2PModeSelectFragment();
                }
            }, SyncDataConverterUtil.generateSummaryReport(receiverPresenter.getView().getContext(), getTransferProgress()));
        } else if (awaitingManifestReceipt) {
            processManifest(endpointId, payload);
        } else {
            processPayloadChunk(endpointId, payload);
        }
    }

    public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        // Do since we are using ParcelFileDescriptor for STREAM data type & BYTES which is sent at once
        Timber.e("Received payload transfer update %d with %,d bytes transfer | PayloadId %d | Total Bytes %,d | From endpoint %s"
                , update.getStatus(), update.getBytesTransferred(), update.getPayloadId()
                , update.getTotalBytes(), endpointId);
        if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
            long payloadId = update.getPayloadId();
            if (awaitingPayloadManifests.get(payloadId) != null) {
                finishProcessingData(endpointId, payloadId);
                sendPayloadReceived(payloadId);
            }
        }
    }

    public void processManifest(@NonNull String endpointId, @NonNull Payload payload) {
        if (payload.getType() == Payload.Type.BYTES && payload.asBytes() != null) {
            try {
                SyncPackageManifest syncPackageManifest = new Gson().fromJson(new String(payload.asBytes()), SyncPackageManifest.class);
                awaitingPayloadManifests.put(syncPackageManifest.getPayloadId(), syncPackageManifest);

                awaitingManifestReceipt = false;
                receiverPresenter.getView().updateProgressDialog(String.format(receiverPresenter.getView().getString(R.string.receiving_progress_text)
                        , syncPackageManifest.getRecordsSize(), syncPackageManifest.getDataType().getName()), "");
            } catch (JsonParseException e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_received_invalid_manifest_from_endpoint), endpointId);
            }
        } else {
            Timber.e(new Exception(), receiverPresenter.getView().getString(R.string.log_receive_invalid_manifest_from_endpoint), endpointId);
        }
    }

    public void processPayloadChunk(@NonNull String endpointId, @NonNull Payload payload) {
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

    public void finishProcessingData(String endpointId, long payloadId) {
        ProcessedChunk processedChunk = awaitingPayloads.get(payloadId);
        SyncPackageManifest payloadManifest = awaitingPayloadManifests.get(payloadId);

        if (processedChunk != null && payloadManifest != null) {
            if (payloadManifest.getDataType().getType() == DataType.Type.NON_MEDIA) {
                finishProcessNonMediaData(payloadId);
            } else {
                finishProcessingMediaData(payloadId);
            }
        } else {
            // This is probably a manifest
            Timber.e("Ignored finishProcessingData for payload of id %d from endpoint %s because it's Manifest or its ProcessedChunk cannot be found", payloadId, endpointId);
        }
    }

    private void processNonMediaData(@NonNull final Payload payload) {
        final long payloadId = payload.getId();
        awaitingPayloads.put(payloadId, new ProcessedChunk(payload.getType(), ""));
        Tasker.run(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                String jsonData = SyncDataConverterUtil.readInputStreamAsString(payload.asStream().asInputStream());
                ProcessedChunk processedChunk = awaitingPayloads.get(payloadId);

                jsonData = processedChunk.getJsonData() + jsonData;
                processedChunk.setJsonData(jsonData);

                Timber.e("Final JSONDATA size %,d", jsonData.length());

                awaitingPayloads.put(payloadId, processedChunk);
                return payloadId;
            }
        }, new GenericAsyncTask.OnFinishedCallback<Long>() {
            @Override
            public void onSuccess(@Nullable Long result) {
                if (result != null) {
                    Timber.e("Finished processing chunk for payload %d", payloadId);
                } else {
                    Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_non_media_data));
                    stopTransferAndReset(true);
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_error_occurred_processing_non_media_data));
                stopTransferAndReset(true);
            }
        }, AsyncTask.SERIAL_EXECUTOR);
    }

    private void finishProcessNonMediaData(final long payloadId) {
        Tasker.run(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                JSONArray jsonArray = new JSONArray(awaitingPayloads.get(payloadId).getJsonData());
                SyncPackageManifest syncPackageManifest = awaitingPayloadManifests.get(payloadId);

                int recordsSize = jsonArray.length();

                updateTransferProgress(syncPackageManifest.getDataType().getName(), recordsSize);
                logTransfer(false, syncPackageManifest.getDataType().getName(), receiverPresenter.getCurrentPeerDevice(), recordsSize);

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
                    awaitingPayloadManifests.remove(payloadId);
                } else {
                    Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_non_media_data));
                    stopTransferAndReset(true);
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, receiverPresenter.getView().getString(R.string.log_error_occurred_processing_non_media_data));
                stopTransferAndReset(true);
            }
        }, AsyncTask.SERIAL_EXECUTOR);

    }

    private synchronized void updateLastRecord(@NonNull String entityName, long lastRecordId) {
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
        final long payloadId = payload.getId();
        ProcessedChunk processedChunk = awaitingPayloads.get(payloadId);

        if (processedChunk == null) {
            awaitingPayloads.put(payloadId, new ProcessedChunk(payload.getType(), payload));
        }
    }

    private void finishProcessingMediaData(@NonNull long payloadId) {
        ProcessedChunk processedChunk = awaitingPayloads.get(payloadId);
        final Payload payload = processedChunk.getFileData();

        if (payload != null) {
            Tasker.run(new Callable<Long>() {

                @Override
                public Long call() throws Exception {
                    SyncPackageManifest syncPackageManifest = awaitingPayloadManifests.get(payload.getId());

                    updateTransferProgress(syncPackageManifest.getDataType().getName(), 1);
                    logTransfer(false, syncPackageManifest.getDataType().getName(), receiverPresenter.getCurrentPeerDevice(), 1);

                    HashMap<String, Object> payloadDetails = syncPackageManifest.getPayloadDetails();
                    long fileRecordId = payloadDetails != null ? (new Double((double) payloadDetails.get("fileRecordId"))).longValue() : 0l;
                    long lastRecordId = P2PLibrary.getInstance().getReceiverTransferDao()
                            .receiveMultimedia(syncPackageManifest.getDataType(), payload.asFile().asJavaFile()
                                    , payloadDetails, fileRecordId);

                    if (lastRecordId > -1) {
                        updateLastRecord(syncPackageManifest.getDataType().getName(), lastRecordId);
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
                        Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_media_data));
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
            }, AsyncTask.SERIAL_EXECUTOR);
        } else {
            Timber.e(receiverPresenter.getView().getString(R.string.log_error_occurred_processing_media_data));
            stopTransferAndReset(true);
        }
    }

    public void sendPayloadReceived(long payloadId) {
        receiverPresenter.sendTextMessage(Constants.Connection.PAYLOAD_RECEIVED + payloadId);
    }

    private void stopTransferAndReset(boolean startAdvertising) {
        NearbyStorageUtil.deleteFilesInNearbyFolder(receiverPresenter.getView().getContext());
        DiscoveredDevice peerDevice = receiverPresenter.getCurrentPeerDevice();
        if (peerDevice != null) {
            receiverPresenter.disconnectAndReset(peerDevice.getEndpointId(), startAdvertising);
        }
    }
}
