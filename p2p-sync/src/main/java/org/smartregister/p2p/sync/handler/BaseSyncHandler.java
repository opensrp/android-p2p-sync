package org.smartregister.p2p.sync.handler;

import android.support.annotation.NonNull;

import org.smartregister.p2p.model.DataType;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 07/05/2019
 */

public class BaseSyncHandler {

    protected HashMap<String, Integer> transferProgress = new HashMap<>();

    protected synchronized void updateTransferProgress(@NonNull DataType dataType, int recordsTransferred) {
        String dataTypeName = dataType.getName();

        if (transferProgress.containsKey(dataTypeName)) {
            transferProgress.put(dataTypeName, transferProgress.get(dataTypeName) + recordsTransferred);
        } else {
            transferProgress.put(dataTypeName, recordsTransferred);
        }
    }
}
