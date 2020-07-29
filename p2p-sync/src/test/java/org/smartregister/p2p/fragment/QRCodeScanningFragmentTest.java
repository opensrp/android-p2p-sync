package org.smartregister.p2p.fragment;

import android.util.SparseArray;

import com.google.android.gms.vision.barcode.Barcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 24-07-2020.
 */
public class QRCodeScanningFragmentTest {

    private QRCodeScanningFragment qrCodeScanningFragment;

    @Before
    public void setUp() throws Exception {
        qrCodeScanningFragment = new QRCodeScanningFragment();
    }

    @Test
    public void areCodesDuplicateShouldReturnFalse() {
        SparseArray<Barcode> scanResults = new SparseArray<>();

        Barcode barcode = new Barcode();
        barcode.rawValue = "98238";
        scanResults.append(0 , barcode);

        Assert.assertFalse(qrCodeScanningFragment.areCodesDuplicate(scanResults));
    }

    @Test
    public void areCodesDuplicateShouldReturnTrueWhenCodeIsDuplicateWithin5Seconds() {
        String barcodeResult = "98238";
        HashMap<String, Long> alreadyReadCodes = ReflectionHelpers.getField(qrCodeScanningFragment, "alreadyReadCodes");
        alreadyReadCodes.put(barcodeResult, System.currentTimeMillis());

        SparseArray<Barcode> scanResults = new SparseArray<>();

        Barcode barcode = new Barcode();
        barcode.rawValue = barcodeResult;
        scanResults.append(0 , barcode);

        Assert.assertTrue(qrCodeScanningFragment.areCodesDuplicate(scanResults));
    }

    @Test
    public void areCodesDuplicateShouldReturnFalseWhenCodeIsDuplicate10SecondsAgo() {
        String barcodeResult = "98238";
        HashMap<String, Long> alreadyReadCodes = ReflectionHelpers.getField(qrCodeScanningFragment, "alreadyReadCodes");
        alreadyReadCodes.put(barcodeResult, System.currentTimeMillis() - (10 * 1000));

        SparseArray<Barcode> scanResults = new SparseArray<>();

        Barcode barcode = new Barcode();
        barcode.rawValue = barcodeResult;
        scanResults.append(0 , barcode);

        Assert.assertFalse(qrCodeScanningFragment.areCodesDuplicate(scanResults));
    }
}