package org.smartregister.p2p.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.smartregister.p2p.R;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class QRCodeGeneratorDialog extends DialogFragment {

    private DialogInterface dialogInterface;
    private String authenticationCode;
    private QRCodeAuthenticationCallback qrCodeAuthenticationCallback;
    private String deviceName;

    public QRCodeGeneratorDialog() {
        dialogInterface = new DialogInterface() {
            @Override
            public void cancel() {
                QRCodeGeneratorDialog.this.dismiss();
            }

            @Override
            public void dismiss() {
                QRCodeGeneratorDialog.this.dismiss();
            }
        };
    }

    public void setAuthenticationCode(@NonNull String authenticationCode) {
        this.authenticationCode = authenticationCode;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setQrCodeAuthenticationCallback(QRCodeAuthenticationCallback qrCodeAuthenticationCallback) {
        this.qrCodeAuthenticationCallback = qrCodeAuthenticationCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_qr_code_generation, null))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (qrCodeAuthenticationCallback != null) {
                            qrCodeAuthenticationCallback.onAccepted(dialogInterface);
                        }
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (qrCodeAuthenticationCallback != null) {
                            qrCodeAuthenticationCallback.onRejected(dialogInterface);
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        setCancelable(false);

        return dialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        // Generate the QR Code
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = null;
        try {
            bitmap = barcodeEncoder.encodeBitmap(authenticationCode, BarcodeFormat.QR_CODE, 800, 800);

            ImageView imageViewQrCode = getDialog().findViewById(R.id.iv_qrCodeGenDialog_qrCode);
            imageViewQrCode.setImageBitmap(bitmap);

            ((TextView) getDialog().findViewById(R.id.tv_qrCodeGenDialog_authCode))
                    .setText(String.format("Accept connection to %s with code %s", deviceName, authenticationCode));
        } catch (WriterException e) {
            Timber.e(e);

            // TODO: Show error toast
            if (qrCodeAuthenticationCallback != null) {
                qrCodeAuthenticationCallback.onRejected(dialogInterface);
            }
            dismiss();
        }

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public interface QRCodeAuthenticationCallback {

        void onAccepted(@NonNull DialogInterface dialogInterface);

        void onRejected(@NonNull DialogInterface dialogInterface);
    }
}
