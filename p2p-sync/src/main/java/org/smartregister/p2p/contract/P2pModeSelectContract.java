package org.smartregister.p2p.contract;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.PayloadCallback;

import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.callback.OnResultCallback;
import org.smartregister.p2p.dialog.QRCodeGeneratorDialog;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;
import org.smartregister.p2p.sync.ConnectionLevel;
import org.smartregister.p2p.sync.DiscoveredDevice;
import org.smartregister.p2p.sync.IReceiverSyncLifecycleCallback;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface P2pModeSelectContract {

    interface View extends BaseView {

        void enableSendReceiveButtons(boolean enable);

        void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        boolean removeReceiveProgressDialog();

        void showDiscoveringProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        boolean removeDiscoveringProgressDialog();

        void showQRCodeScanningDialog(@NonNull QRCodeScanningDialog.QRCodeScanDialogCallback qrCodeScanDialogCallback);

        void showQRCodeGeneratorDialog(@NonNull String authenticationCode, @NonNull String deviceName
                , @NonNull QRCodeGeneratorDialog.QRCodeAuthenticationCallback qrCodeAuthenticationCallback);

        void showConnectionAcceptDialog(@NonNull String receiverDeviceName, @NonNull String authenticationCode
                , @NonNull DialogInterface.OnClickListener onClickListener);

        void requestPermissions(@NonNull List<String> unauthorisedPermissions);

        @NonNull
        List<String> getUnauthorisedPermissions();

        boolean isLocationEnabled();

        void dismissAllDialogs();

        void requestEnableLocation(@NonNull OnLocationEnabled onLocationEnabled);

        void showToast(@NonNull String text, int length);

        void displayMessage(@NonNull String text);

        @NonNull
        String getString(@StringRes int resId);

        interface DialogCancelCallback {
            void onCancelClicked(DialogInterface dialogInterface);
        }

        interface OnLocationEnabled {
            void locationEnabled();
        }
    }

    interface BasePresenter {

        void onStop();

        /**
         * Sends a payload to the other device and returns the payloadId which can be used to track the
         * transfer progress of the payload/message
         *
         * @param message the string message
         * @return the payloadId
         */
        long sendTextMessage(@NonNull String message);

        @NonNull
        View getView();

        @Nullable
        DiscoveredDevice getCurrentPeerDevice();
    }

    interface ReceiverPresenter extends BasePresenter {

        void onReceiveButtonClicked();

        void prepareForAdvertising(boolean returningFromRequestingPermissions);

        void startAdvertisingMode();

    }

    interface SenderPresenter extends BasePresenter {

        void onSendButtonClicked();

        void prepareForDiscovering(boolean returningFromRequestingPermissions);

        void startDiscoveringMode();
    }

    interface Interactor extends BaseInteractor {

        void startAdvertising(@NonNull final IReceiverSyncLifecycleCallback iReceiverSyncLifecycleCallback);

        void stopAdvertising();

        boolean isAdvertising();

        void startDiscovering(@NonNull final ISenderSyncLifecycleCallback iSenderSyncLifecycleCallback);

        void stopDiscovering();

        boolean isDiscovering();

        void closeAllEndpoints();

        void cleanOngoingConnectionResources();

        /**
         * Sends a payload to the other device and returns the payloadId which can be used to track the
         * transfer progress of the payload/message
         *
         * @param message the string message
         * @return the payloadId
         */
        long sendMessage(@NonNull String message);

        void connectedTo(@Nullable String endpointId);

        @NonNull
        String getAppPackageName();

        @NonNull
        String getUserNickName();

        @NonNull
        Context getContext();

        void requestConnection(@NonNull String endpointId
                , @NonNull OnResultCallback onRequestConnectionResult
                , @NonNull ConnectionLifecycleCallback connectionLifecycleCallback);

        void acceptConnection(@NonNull String endpointId, PayloadCallback payloadCallback);

        void rejectConnection(@NonNull String endpointId);
    }
}
