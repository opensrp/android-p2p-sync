package org.smartregister.p2p.contract;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.smartregister.p2p.handlers.ActivityResultHandler;
import org.smartregister.p2p.handlers.ActivityResumeHandler;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface P2pModeSelectContract {

    interface View extends BaseView, ActivityResultHandler, ActivityResumeHandler {

        void enableSendReceiveButtons(boolean enable);

        void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        void requestPermissions(@NonNull List<String> unauthorisedPermissions);

        @NonNull
        List<String> getUnauthorisedPermissions();

        boolean isLocationEnabled();

        void requestEnableLocation(@NonNull OnLocationEnabled onLocationEnabled);

        interface DialogCancelCallback {
            void onCancelClicked(DialogInterface dialogInterface);
        }

        interface OnLocationEnabled {
            void locationEnabled();
        }
    }

    interface Presenter {

        void onSendButtonClicked();

        void onReceiveButtonClicked();

        void prepareForAdvertising(boolean returningFromRequestingPermissions);

        void startAdvertisingMode();
    }

    interface Interactor {

        void startAdvertising();

        void stopAdvertising();

        boolean isAdvertising();

        @NonNull
        String getAppPackageName();

        @NonNull
        String getAdvertisingUsername();

        @NonNull
        Context getContext();
    }
}
