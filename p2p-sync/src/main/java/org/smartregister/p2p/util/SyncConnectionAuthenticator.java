package org.smartregister.p2p.util;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.vision.barcode.Barcode;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;
import org.smartregister.p2p.sync.DiscoveredDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class SyncConnectionAuthenticator extends BaseSyncConnectionAuthenticator {

    public SyncConnectionAuthenticator(@NonNull P2pModeSelectContract.View view
            , @NonNull P2pModeSelectContract.Interactor interactor
            , @NonNull P2pModeSelectContract.Presenter presenter) {
        super(view, interactor, presenter);
    }

    @Override
    public void authenticate(@NonNull final DiscoveredDevice discoveredDevice, @NonNull final AuthenticationCallback authenticationCallback) {
        if (discoveredDevice.getConnectionInfo() != null
                && !discoveredDevice.getConnectionInfo().isIncomingConnection()
                && discoveredDevice.getDiscoveredEndpointInfo() != null) {

            final ConnectionInfo connectionInfo = discoveredDevice.getConnectionInfo();
            final DiscoveredEndpointInfo discoveredEndpointInfo = discoveredDevice.getDiscoveredEndpointInfo();

            view.showQRCodeScanningDialog(new QRCodeScanningDialog.QRCodeScanDialogCallback() {
                @Override
                public void qrCodeScanned(@NonNull SparseArray<Barcode> qrCodeResult, @NonNull DialogInterface dialogInterface) {
                    dialogInterface.dismiss();

                    String authenticationCode = connectionInfo.getAuthenticationToken();
                    boolean authenticationCodeFound = false;

                    for (int i = 0; i < qrCodeResult.size(); i++) {
                        if (authenticationCode.equals(qrCodeResult.get(i).rawValue)) {
                            authenticationCodeFound = true;
                            break;
                        }
                    }

                    String message = "Device %s authentication failed";
                    if (authenticationCodeFound) {
                        message = "Device %s authenticated successfully";
                        authenticationCallback.onAuthenticationSuccessful();
                    } else {
                        authenticationCallback.onAuthenticationFailed(new Exception("Authentication tokens do not match"));
                    }

                    view.showToast(String.format(message, connectionInfo.getEndpointName()), Toast.LENGTH_LONG);
                }

                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    dialogInterface.dismiss();
                    view.showConnectionAcceptDialog(discoveredEndpointInfo.getEndpointName(), connectionInfo.getAuthenticationToken(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Todo: Test if the dialogs are dismissed automatically
                            //dialog.dismiss();

                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                authenticationCallback.onAuthenticationSuccessful();
                            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                authenticationCallback.onAuthenticationFailed(new Exception("User cancelled the connection"));
                            }
                        }
                    });
                }
            });
        } else {

        }
    }
}
