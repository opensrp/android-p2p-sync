package org.smartregister.p2p.sample.dao;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.sync.JsonData;
import org.smartregister.p2p.sync.MultiMediaData;

import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SampleSenderDao implements SenderTransferDao {

    @Nullable
    @Override
    public TreeSet<DataType> getDataTypes() {
        return null;
    }

    @Nullable
    @Override
    public JsonData getJsonData(@NonNull DataType dataType, long lastRecordId, int batchSize) {
        return null;
    }

    @Nullable
    @Override
    public MultiMediaData getMultiMediaData(@NonNull DataType dataType, long lastRecordId) {
        return null;
    }
}
