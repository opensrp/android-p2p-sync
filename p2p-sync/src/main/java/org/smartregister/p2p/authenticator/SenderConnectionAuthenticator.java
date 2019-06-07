package org.smartregister.p2p.authenticator;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.vision.barcode.Barcode;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.QRCodeScanningFragment;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class SenderConnectionAuthenticator extends BaseSyncConnectionAuthenticator {

    public SenderConnectionAuthenticator(@NonNull P2pModeSelectContract.SenderPresenter senderPresenter) {
        super(senderPresenter);
    }

    @Override
    public void authenticate(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        if (discoveredDevice.getConnectionInfo() != null
                && !discoveredDevice.getConnectionInfo().isIncomingConnection()) {
            final ConnectionInfo connectionInfo = discoveredDevice.getConnectionInfo();

            getPresenter().getView().showQRCodeScanningFragment(discoveredDevice.getEndpointName(), new QRCodeScanningFragment.QRCodeScanDialogCallback() {
                @Override
                public void qrCodeScanned(final @NonNull SparseArray<Barcode> qrCodeResult) {
                    getPresenter().getView().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String authenticationCode = connectionInfo.getAuthenticationToken();
                            boolean authenticationCodeFound = false;

                            for (int i = 0; i < qrCodeResult.size(); i++) {
                                if (authenticationCode.equals(qrCodeResult.valueAt(i).rawValue)) {
                                    authenticationCodeFound = true;
                                    break;
                                }
                            }

                            if (authenticationCodeFound) {
                                authenticationCallback.onAuthenticationSuccessful();
                            } else {
                                authenticationCallback.onAuthenticationFailed(new Exception("Authentication tokens do not match"));
                            }

                            //getPresenter().getView().showToast(String.format(message, connectionInfo.getEndpointName()), Toast.LENGTH_LONG);
                        }
                    });
                }

                @Override
                public void onErrorOccurred(@NonNull Exception e) {

                }

                @Override
                public void onSkipClicked() {
                    getPresenter().getView().showConnectionAcceptDialog(discoveredDevice.getEndpointName(), connectionInfo.getAuthenticationToken(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Todo: Test if the dialogs are dismissed automatically
                            //dialog.dismiss();

                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                authenticationCallback.onAuthenticationSuccessful();
                            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                authenticationCallback.onAuthenticationFailed(new Exception("User rejected the connection"));
                            }
                        }
                    });
                }
            });
        } else {
            authenticationCallback.onAuthenticationFailed(new Exception("DiscoveredDevice information passed is invalid"));
        }
    }
}
