package org.smartregister.p2p.sync.handler;

import android.support.annotation.NonNull;
import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 07/05/2019
 */

public class BaseSyncHandler {

    private HashMap<String, Integer> transferProgress = new HashMap<>();

    protected synchronized void updateTransferProgress(@NonNull String dataTypeName, int recordsTransferred) {
        if (transferProgress.containsKey(dataTypeName)) {
            transferProgress.put(dataTypeName, transferProgress.get(dataTypeName) + recordsTransferred);
        } else {
            transferProgress.put(dataTypeName, recordsTransferred);
        }
    }

    public HashMap<String, Integer> getTransferProgress() {
        return transferProgress;
    }
}
