package org.smartregister.p2p.exceptions;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */
public class AsyncTaskCancelledException extends Exception {

    public AsyncTaskCancelledException() {
        super("AsyncTask was cancelled");
    }

    public AsyncTaskCancelledException(Class asyncTaskClass) {
        super("AsyncTask was cancedlled : " + asyncTaskClass.getName());
    }

    public AsyncTaskCancelledException(Throwable cause) {
        super(cause);
    }

    public AsyncTaskCancelledException(String message) {
        super(message);
    }

}
