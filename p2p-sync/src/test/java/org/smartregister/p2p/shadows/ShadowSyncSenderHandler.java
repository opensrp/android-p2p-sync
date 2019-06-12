package org.smartregister.p2p.shadows;

import android.support.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.p2p.sync.handler.SyncSenderHandler;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-12
 */

@Implements(SyncSenderHandler.class)
public class ShadowSyncSenderHandler {

    @Implementation
    public void startNewThread(@NonNull Runnable runnable) {
        runnable.run();
    }
}
