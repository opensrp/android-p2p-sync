package org.smartregister.p2p.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.smartregister.p2p.exceptions.AsyncTaskCancelledException;

import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

public class GenericAsyncTask<T> extends AsyncTask<Void, Void, T> {

    private Callable<T> toCall;
    private OnFinishedCallback onFinishedCallback;

    private Exception exception;

    public GenericAsyncTask(@NonNull Callable<T> toCall) {
        this.toCall = toCall;
    }

    @Override
    protected T doInBackground(Void... voids) {
        try {
            return toCall.call();
        } catch (Exception e) {
            Timber.e(e);
            exception = e;
            this.cancel(true);

            return null;
        }
    }

    @Override
    protected void onPostExecute(T result) {
        if (onFinishedCallback != null) {
            onFinishedCallback.onSuccess(result);
        }
    }

    @Override
    protected void onCancelled() {
        if (onFinishedCallback != null) {
            Exception cancelException = exception == null ?
                    new AsyncTaskCancelledException() :
                    exception;

            onFinishedCallback.onError(cancelException);
        }
    }

    public void setOnFinishedCallback(@Nullable OnFinishedCallback onFinishedCallback) {
        this.onFinishedCallback = onFinishedCallback;
    }

    public interface OnFinishedCallback {

        void onSuccess(@Nullable Object... objects);

        void onError(Exception e);
    }
}
