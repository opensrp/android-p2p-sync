package org.smartregister.p2p.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 10/05/2019
 */

public interface SyncFinishedCallback {

    @UiThread
    void onSuccess(@NonNull HashMap<String, Integer> transferRecords);

    @UiThread
    void onFailure(@NonNull Exception exception, @Nullable HashMap<String, Integer> transferRecords);
}
