package org.smartregister.p2p.contract;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;


/**
 * Provide a unique ID to be used by the app.
 * Should a Global Unique ID that can be used returned by the
 */
public interface RecalledIdentifier {

    @NonNull
    @WorkerThread
    String getUniqueID(Context context);
}
