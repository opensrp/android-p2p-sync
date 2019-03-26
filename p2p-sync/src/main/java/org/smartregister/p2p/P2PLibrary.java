package org.smartregister.p2p;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.util.Constants;
import java.util.UUID;
import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public final class P2PLibrary {

    private static P2PLibrary instance;
    private Options options;
    private String hashKey;

    @NonNull
    public static P2PLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call P2PLibrary.init method"
                    + "in the onCreate method of "
                    + "your Application class ");
        }

        return instance;
    }

    public static void init(@NonNull Options options) {
        instance = new P2PLibrary(options);
    }

    private P2PLibrary(@NonNull Options options) {
        this.options = options;

        // We should not override the host applications Timber trees
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }

        checkHashKeyPresent();
    }

    private void checkHashKeyPresent() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE);
        hashKey = sharedPreferences.getString(Constants.Prefs.KEY_HASH, null);
        if (hashKey == null) {
            hashKey = UUID.randomUUID().toString();
            sharedPreferences.edit()
                    .putString(Constants.Prefs.KEY_HASH, hashKey)
                    .apply();
        }
    }

    @NonNull
    public String getUsername() {
        return options.getUsername();
    }

    @NonNull
    public P2PAuthorizationService getP2PAuthorizationService() {
        return options.getP2PAuthorizationService();
    }

    @NonNull
    public Context getContext() {
        return options.getContext();
    }

    public static class Options {

        private Context context;
        private String username;
        private P2PAuthorizationService p2PAuthorizationService;

        public Options(@NonNull Context context, @NonNull String username, @NonNull P2PAuthorizationService p2PAuthorizationService) {
            this.context = context;
            this.username = username;
            this.p2PAuthorizationService = p2PAuthorizationService;
        }

        @NonNull
        public String getUsername() {
            return this.username;
        }

        @NonNull
        public P2PAuthorizationService getP2PAuthorizationService() {
            return p2PAuthorizationService;
        }

        public Context getContext() {
            return context;
        }
    }
}
