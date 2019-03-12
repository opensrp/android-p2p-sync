package org.smartregister.p2p;

import android.support.annotation.NonNull;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public final class P2PLibrary {

    private static P2PLibrary instance;
    private BasicOptions basicOptions;

    @NonNull
    public static P2PLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call P2PLibrary.init method"
                    + "in the onCreate method of "
                    + "your Application class ");
        }

        return instance;
    }

    public static void init(@NonNull BasicOptions basicOptions) {
        instance = new P2PLibrary(basicOptions);
    }

    private P2PLibrary(@NonNull BasicOptions basicOptions) {
        this.basicOptions = basicOptions;

        // We should not override the host applications Timber trees
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @NonNull
    public String getUsername() {
        return basicOptions.getUsername();
    }

    public static class ReceiverOptions extends BasicOptions {

        public ReceiverOptions(@NonNull String advertisingName) {
            super(advertisingName);
        }

    }

    public static abstract class BasicOptions {

        private String username;

        public BasicOptions(@NonNull String username) {
            this.username = username;
        }

        @NonNull
        public String getUsername() {
            return this.username;
        }
    }
}
