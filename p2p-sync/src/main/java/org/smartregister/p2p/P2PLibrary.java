package org.smartregister.p2p;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import java.util.UUID;
import android.support.annotation.Nullable;

import org.smartregister.p2p.tasks.GenericAsyncTask;
import org.smartregister.p2p.util.Device;
import org.smartregister.p2p.util.Settings;
import java.util.concurrent.Callable;
import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public final class P2PLibrary {

    private static P2PLibrary instance;
    private Options options;
    private String hashKey;
    private String deviceUniqueIdentifier;

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

        hashKey = getHashKey();
    }

    @NonNull
    public String getHashKey() {
        if (hashKey == null) {
            Settings settings = new Settings(getContext());
            hashKey = settings.getHashKey();

            if (hashKey == null) {
                hashKey = generateHashKey();
                settings.saveHashKey(hashKey);
            }
        }

        return hashKey;
    }

    private String generateHashKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * This retrieves the device Wifi MAC Address. This might take up-to 5 seconds because it has to turn on wifi
     * if it is not on so that is can access the WLAN interface
     *
     * @param context
     * @param onFinishedCallback
     */
    public void getDeviceMacAddress(@NonNull final Context context, @NonNull GenericAsyncTask.OnFinishedCallback onFinishedCallback) {
        GenericAsyncTask<String> genericAsyncTask = new GenericAsyncTask<>(new Callable<String>() {
            @Override
            public String call() {
                return Device.generateUniqueDeviceId(context);
            }
        });

        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);
        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setDeviceUniqueIdentifier(@NonNull String deviceUniqueIdentifier) {
        this.deviceUniqueIdentifier = deviceUniqueIdentifier;
    }

    @Nullable
    public String getDeviceUniqueIdentifier() {
        return deviceUniqueIdentifier;
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
