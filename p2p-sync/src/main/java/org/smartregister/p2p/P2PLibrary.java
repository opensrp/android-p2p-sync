package org.smartregister.p2p;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public final class P2PLibrary {

    private static P2PLibrary instance;
    private Context context;
    private ReceiverOptions receiverOptions;

    @Nullable
    public static P2PLibrary getInstance() {
        return instance;
    }

    public static void init(@NonNull Context context, @NonNull ReceiverOptions receiverOptions) {
        instance = new P2PLibrary(context, receiverOptions);
    }

    private P2PLibrary(@NonNull Context context, @NonNull ReceiverOptions receiverOptions) {
        this.context = context;
        this.receiverOptions = receiverOptions;

        // We should not override the host applications Timber trees
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Nullable
    public String getAdvertisingName() {
        return receiverOptions != null ? receiverOptions.advertisingName : null;
    }

    public static class ReceiverOptions {

        private String advertisingName;

        public ReceiverOptions(@NonNull String advertisingName) {
            this.advertisingName = advertisingName;
        }
    }
}
