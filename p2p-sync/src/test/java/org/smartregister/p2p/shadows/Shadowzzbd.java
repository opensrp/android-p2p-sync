package org.smartregister.p2p.shadows;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.internal.nearby.zzbd;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.mockito.Mock;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

@Implements(zzbd.class)
public class Shadowzzbd extends ShadowConnectionsClient {

    public boolean stopAdvertisingCalled;
    public boolean startAdvertisingCalled;
    public static ArrayList<Shadowzzbd> instances = new ArrayList<>();

    @Implementation
    public void __constructor__(Context context) {
    }

    @Implementation
    public void stopAdvertising() {
        stopAdvertisingCalled = true;

        if (!instances.contains(this)) {
            instances.add(this);
        }
    }

    @Implementation
    public Task<Void> startAdvertising(@NonNull String var1, @NonNull String var2, @NonNull ConnectionLifecycleCallback var3, @NonNull AdvertisingOptions var4) {
        startAdvertisingCalled = true;
        return new DummyTask() {
            @NonNull
            @Override
            public Task addOnSuccessListener(@NonNull OnSuccessListener onSuccessListener) {
                onSuccessListener.onSuccess(null);
                return super.addOnSuccessListener(onSuccessListener);
            }
        };
    }

    public static class DummyTask extends Task {

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Nullable
        @Override
        public Object getResult() {
            return null;
        }

        @Nullable
        @Override
        public Exception getException() {
            return null;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener onSuccessListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @NonNull
        @Override
        public Task addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
            return DummyTask.this;
        }

        @Nullable
        @Override
        public Object getResult(@NonNull Class aClass) throws Throwable {
            return null;
        }
    }

}
