package org.smartregister.p2p.model.dao;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.sync.MultiMediaData;
import org.smartregister.p2p.sync.JsonData;

import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public interface SenderTransferDao {

    @WorkerThread
    @Nullable
    TreeSet<DataType> getDataTypes();

    @WorkerThread
    @Nullable
    JsonData getJsonData(@NonNull DataType dataType, long lastRecordId, int batchSize);

    @WorkerThread
    @Nullable
    MultiMediaData getMultiMediaData(@NonNull DataType dataType, long lastRecordId);
}
