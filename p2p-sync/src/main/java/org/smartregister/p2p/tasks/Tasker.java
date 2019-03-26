package org.smartregister.p2p.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

public class Tasker {

    public static <T> void run(@NonNull Callable<T> callable
            , @NonNull GenericAsyncTask.OnFinishedCallback<T> onFinishedCallback) {
        GenericAsyncTask<T> genericAsyncTask = new GenericAsyncTask<T>(callable);
        genericAsyncTask.setOnFinishedCallback(onFinishedCallback);
        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
