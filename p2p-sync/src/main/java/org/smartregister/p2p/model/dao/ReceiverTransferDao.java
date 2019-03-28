package org.smartregister.p2p.model.dao;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.gson.JsonArray;

import org.smartregister.p2p.model.DataType;

import java.io.File;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public interface ReceiverTransferDao {

    @WorkerThread
    TreeSet<DataType> getDataTypes();

    @WorkerThread
    int receiveJson(@NonNull DataType type, @NonNull JsonArray jsonArray);

    @WorkerThread
    boolean receiveMultimedia(@NonNull DataType dataType, @NonNull File file);

}
