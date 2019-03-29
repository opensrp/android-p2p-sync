package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.P2pReceivedHistory;
import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.tasks.Tasker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SyncSenderHandler {

    private P2pModeSelectContract.SenderPresenter presenter;

    private TreeSet<DataType> dataSyncOrder;
    private HashMap<String, Long> remainingLastRecordIds = new HashMap<>();
    private List<P2pReceivedHistory> receivedHistory;
    private int batchSize;

    private boolean awaitingPayloadTransfer;
    private Payload awaitingPayload;
    private String awaitingDataTypeName;
    private long awaitingDataTypeHighestId;

    private boolean awaitingManifestTransfer;
    private long awaitingManifestId;

    private int sendMaxRetries = 3;
    private PayloadRetry payloadRetry;
    private SyncPackageManifest syncPackageManifest;

    public SyncSenderHandler(@NonNull P2pModeSelectContract.SenderPresenter presenter, @NonNull TreeSet<DataType> dataSyncOrder
            , @Nullable List<P2pReceivedHistory> receivedHistory) {
        this.presenter = presenter;
        this.dataSyncOrder = dataSyncOrder;
        this.receivedHistory = receivedHistory;
        this.batchSize = P2PLibrary.getInstance().getBatchSize();
    }

    private void generateRecordsToSend() {
        for (DataType dataType : dataSyncOrder) {
            remainingLastRecordIds.put(dataType.getName(), 0l);
        }

        if (receivedHistory != null && receivedHistory.size() > 0) {
            for (P2pReceivedHistory dataTypeHistory: receivedHistory) {
                remainingLastRecordIds.put(dataTypeHistory.getEntityType(), dataTypeHistory.getLastRecordId());
            }
        }
    }

    public void startSyncProcess() {
        generateRecordsToSend();
        sendNextManifest();
    }

    public void sendNextManifest() {
        if (!dataSyncOrder.isEmpty()) {

            final DataType dataType = dataSyncOrder.first();

            if (dataType.getType() == DataType.Type.NON_MEDIA) {
                sendJsonDataManifest(dataType);
            } else if (dataType.getType() == DataType.Type.MEDIA) {
                sendMultimediaDataManifest(dataType);
            }
        } else {
            presenter.sendSyncComplete();
        }
    }

    @VisibleForTesting
    public void sendMultimediaDataManifest(@NonNull final DataType dataType) {
        Tasker.run(new Callable<File>() {
            @Override
            public File call() throws Exception {

                long lastRecordId = remainingLastRecordIds.get(dataType.getName());
                MultiMediaData multiMediaData = P2PLibrary.getInstance().getSenderTransferDao()
                        .getMultiMediaData(dataType, lastRecordId);

                if (multiMediaData != null) {
                    File file = multiMediaData.getFile();
                    awaitingDataTypeName = dataType.getName();
                    awaitingDataTypeHighestId = multiMediaData.getRecordId();

                    return file;
                } else {
                    return null;
                }
            }
        }, new GenericAsyncTask.OnFinishedCallback<File>() {
            @Override
            public void onSuccess(@Nullable File result) {
                if (result != null && result.exists()) {
                    // Create the manifest
                    InputStream fileIs = createFileDataStream(result);

                    if (fileIs != null) {
                        awaitingPayload = Payload.fromStream(fileIs);

                        String filename = result.getName();
                        String extension = "";

                        int lastIndex = filename.lastIndexOf(".");
                        if (lastIndex > -1 && lastIndex < filename.length()) {
                            extension = filename.substring(lastIndex);
                        }

                        syncPackageManifest = new SyncPackageManifest(awaitingPayload.getId()
                                , extension
                                , dataType);

                        awaitingManifestTransfer = true;
                        awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                    } else {
                        onError(new Exception("File could not be found"));
                    }
                } else {
                    dataSyncOrder.remove(dataType);
                    sendNextManifest();
                }
            }

            @Override
            public void onError(Exception e) {
                presenter.errorOccurredSync(e);
            }
        });
    }

    @VisibleForTesting
    public void sendJsonDataManifest(@NonNull final DataType dataType) {
        Tasker.run(new Callable<String>() {
            @Override
            public String call() throws Exception {

                long lastRecordId = remainingLastRecordIds.get(dataType.getName());
                JsonData jsonData = P2PLibrary.getInstance().getSenderTransferDao()
                        .getJsonData(dataType, lastRecordId, batchSize);

                if (jsonData != null) {
                    JSONArray recordsArray = jsonData.getJsonArray();
                    remainingLastRecordIds.put(dataType.getName(), jsonData.getHighestRecordId());

                    String jsonString = new Gson().toJson(recordsArray);
                    awaitingDataTypeName = dataType.getName();
                    awaitingDataTypeHighestId = jsonData.getHighestRecordId();

                    return jsonString;
                } else {
                    return null;
                }
            }
        }, new GenericAsyncTask.OnFinishedCallback<String>() {
            @Override
            public void onSuccess(@Nullable String result) {
                if (result != null) {
                    // Create the manifest
                    awaitingPayload = Payload.fromStream(createJsonDataStream(result));

                    syncPackageManifest = new SyncPackageManifest(awaitingPayload.getId()
                            , "json"
                            , dataType);

                    awaitingManifestTransfer = true;
                    awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                } else {
                    dataSyncOrder.remove(dataType);
                    sendNextManifest();
                }
            }

            @Override
            public void onError(Exception e) {
                presenter.errorOccurredSync(e);
            }
        });
    }

    @NonNull
    private InputStream createJsonDataStream(@NonNull String json) {
        return new ByteArrayInputStream(json.getBytes());
    }

    @Nullable
    private InputStream createFileDataStream(@NonNull File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Timber.e(e);
            return null;
        }
    }

    @com.google.android.gms.common.util.VisibleForTesting
    public void sendNextPayload() {
        if (awaitingPayload != null) {
            presenter.sendPayload(awaitingPayload);
        }
    }

    public void onPayloadTransferUpdate(@NonNull PayloadTransferUpdate update) {
        if (awaitingManifestTransfer) {
            if (update.getPayloadId() == awaitingManifestId) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    awaitingManifestTransfer = false;
                    awaitingManifestId = 0;
                    payloadRetry = null;
                    syncPackageManifest = null;

                    sendNextPayload();
                } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                    // Try to resend the manifest until the max retries are done
                    if (payloadRetry == null) {
                        payloadRetry = new PayloadRetry(0l, sendMaxRetries);
                    }

                    if (payloadRetry.retries > 0) {
                        payloadRetry.retries--;

                        awaitingManifestTransfer = true;
                        awaitingManifestId = presenter.sendManifest(syncPackageManifest);
                        payloadRetry.payloadId = awaitingManifestId;
                    } else {
                        presenter.errorOccurredSync(new Exception("Manifest Payload send failed up-to " + sendMaxRetries));
                    }
                } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                    presenter.errorOccurredSync(new Exception("Manifest Payload sending has been cancelled"));
                }
            }
        } else if (awaitingPayloadTransfer) {
            if (awaitingPayload != null && update.getPayloadId() == awaitingPayload.getId()) {
                if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    awaitingPayloadTransfer = false;
                    awaitingPayload = null;

                    if (awaitingDataTypeName != null) {
                        remainingLastRecordIds.put(awaitingDataTypeName, awaitingDataTypeHighestId);
                    }

                    sendNextManifest();
                } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                    // Try to resend the payload until the max retries are done
                    if (payloadRetry == null) {
                        payloadRetry = new PayloadRetry(awaitingPayload.getId(), sendMaxRetries);
                    }

                    if (payloadRetry.retries > 0) {
                        payloadRetry.retries--;
                        sendNextPayload();
                    } else {
                        presenter.errorOccurredSync(new Exception("Payload send failed up-to " + sendMaxRetries));
                    }
                } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                    presenter.errorOccurredSync(new Exception("Payload sending has been cancelled"));
                }
            }
        }
    }

    class PayloadRetry {
        long payloadId;
        int retries;

        PayloadRetry(long payloadId, int retries) {
            this.payloadId = payloadId;
            this.retries = retries;
        }
    }

}
