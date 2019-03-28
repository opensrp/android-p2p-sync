package org.smartregister.p2p.model.dao;

import android.support.annotation.WorkerThread;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;

import java.io.File;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public interface SenderTransferDao {

    @WorkerThread
    JSONArray getJsonData(DataType dataType, long lastRecordId, int batchSize);

    @WorkerThread
    File getMultiMediaData(DataType dataType, long lastRecordId);
}
