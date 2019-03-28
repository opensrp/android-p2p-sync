package org.smartregister.p2p.sample.dao;

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;

import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;

import java.io.File;
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
    public boolean receiveMultimedia(@NonNull DataType dataType, @NonNull File file) {
        return false;
    }

    @Override
    public int receiveJson(@NonNull DataType type, @NonNull JsonArray jsonArray) {
        return 0;
    }
}
