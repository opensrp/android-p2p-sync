package org.smartregister.p2p.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.vision.barcode.Barcode;

import org.smartregister.p2p.R;
import org.smartregister.p2p.view.QRCodeScannerView;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class QRCodeScanningFragment extends Fragment {

    private QRCodeScanDialogCallback qrCodeScanDialogCallback;
    private QRCodeScannerView qrCodeScannerView;

    public QRCodeScanningFragment() {
    }

    public void setOnQRRecognisedListener(@NonNull QRCodeScanDialogCallback qrCodeScanDialogCallback) {
        this.qrCodeScanDialogCallback = qrCodeScanDialogCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_qr_code_scanning, container, false);

        Button skipBtn = view.findViewById(R.id.btn_qrCodeScanningDialog_skipBtn);
        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                if (qrCodeScanDialogCallback != null) {
                    qrCodeScanDialogCallback.onSkipClicked();
                }
            }
        });

        qrCodeScannerView = view.findViewById(R.id.bsv_qrCodeScanningDialog_barcodeScanner);
        qrCodeScannerView.addOnBarcodeRecognisedListener(new QRCodeScannerView.OnQRRecognisedListener() {
            @Override
            public void onBarcodeRecognised(SparseArray<Barcode> recognisedItems) {
                closeFragment();
                if (qrCodeScanDialogCallback != null) {
                    qrCodeScanDialogCallback.qrCodeScanned(recognisedItems);
                }
            }
        });

        return view;
    }

    private void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    @Override
    public void onPause() {
        if (qrCodeScannerView != null) {
            qrCodeScannerView.onPause();
            qrCodeScannerView.onDestroy();
        }
        super.onPause();
    }

    public interface QRCodeScanDialogCallback {

        void onSkipClicked();

        void qrCodeScanned(@NonNull SparseArray<Barcode> qrCodeResult);

        void onErrorOccurred(@NonNull Exception e);
    }
}
