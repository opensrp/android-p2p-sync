package org.smartregister.p2p.util;

import com.google.android.gms.nearby.connection.Strategy;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface Constants {

    Strategy STRATEGY = Strategy.P2P_STAR;
    int DEFAULT_SHARE_BATCH_SIZE = 20;
    int DEFAULT_MIN_DEVICE_CONNECTION_RETRY_DURATION = 2 * 60 * 60;

    interface Dialog {
        String START_SEND_MODE_PROGRESS = "dialog_start_send_mode_progress";
        String START_RECEIVE_MODE_PROGRESS = "dialog_start_receive_mode_progress";
        String QR_CODE_SCANNING = "qr_code_scanner";
        String AUTHENTICATION_QR_CODE_GENERATOR = "authentication_qr_code_generator";
    }

    interface Connection {
        String SYNC_COMPLETE = "SYNC-COMPLETE";
    }

    interface RqCode {
        int PERMISSIONS = 2;
        int LOCATION_SETTINGS = 3;
        int BARCODE_SCANNER_GOOGLE_PLAY_FIX = 4;
    }

    interface Prefs {
        String NAME = "android-p2p-sync";
        String KEY_HASH = "hash-key";
    }

    interface BasicDeviceDetails {

        String KEY_DEVICE_ID = "device-id";
        String KEY_APP_LIFETIME_KEY = "app-lifetime-key";
    }
}
