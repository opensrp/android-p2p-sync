package org.smartregister.p2p.contract;

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface P2pModeSelectContract {

    interface View extends BaseView {

        void enableSendReceiveButtons(boolean enable);

        void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback);

        interface DialogCancelCallback {

            void onCancelClicked(DialogInterface dialogInterface);
        }
    }

    interface Presenter {

        void onSendButtonClicked();

        void onReceiveButtonClicked();
    }

    interface Interactor {

        void startAdvertising();
    }
}
