package org.smartregister.p2p.sample.dao;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;

import java.io.InputStream;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SampleReceiverDao implements ReceiverTransferDao {

    @Override
    public TreeSet<DataType> getDataTypes() {
        return null;
    }

    @Override
    public long receiveMultimedia(@NonNull DataType dataType, @NonNull InputStream inputStream) {
        return 0;
    }

    @Override
    public long receiveJson(@NonNull DataType type, @NonNull JSONArray jsonArray) {
        return 4;
    }
}
