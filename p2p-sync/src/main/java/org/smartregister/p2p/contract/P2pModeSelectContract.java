package org.smartregister.p2p.contract;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.PayloadCallback;

import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface P2pModeSelectContract {

    interface View extends BaseView {

        void enableSendReceiveButtons(boolean enable);

        void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        void showDiscoveringProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        boolean removeDiscoveringProgressDialog();

        void showQRCodeScanningDialog(@NonNull QRCodeScanningDialog.QRCodeScanDialogCallback qrCodeScanDialogCallback);

        void showConnectionAcceptDialog(@NonNull String receiverDeviceId, @NonNull String authenticationCode
                , @NonNull DialogInterface.OnClickListener onClickListener);

        void requestPermissions(@NonNull List<String> unauthorisedPermissions);

        @NonNull
        List<String> getUnauthorisedPermissions();

        boolean isLocationEnabled();

        void dismissAllDialogs();

        void requestEnableLocation(@NonNull OnLocationEnabled onLocationEnabled);

        void showToast(@NonNull String text, int length);

        void displayMessage(@NonNull String text);

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

        void prepareForDiscovering(boolean returningFromRequestingPermissions);

        void startDiscoveringMode();

        void onStop();
    }

    interface Interactor extends BaseInteractor {

        void startAdvertising();

        void stopAdvertising();

        boolean isAdvertising();

        void startDiscovering(@NonNull EndpointDiscoveryCallback endpointDiscoveryCallback
                , @NonNull OnResultCallback onStartDiscoveringResult);

        void stopDiscovering();

        boolean isDiscovering();

        void closeAllEndpoints();

        void sendMessage(@NonNull String message);

        @NonNull
        String getAppPackageName();

        @NonNull
        String getUserNickName();

        @NonNull
        Context getContext();

        void requestConnection(@NonNull String endpointId
                , @NonNull OnResultCallback onRequestConnectionResult
                , @NonNull ConnectionLifecycleCallback connectionLifecycleCallback);

        void acceptConnection(String endpointId, PayloadCallback payloadCallback);
    }
}
