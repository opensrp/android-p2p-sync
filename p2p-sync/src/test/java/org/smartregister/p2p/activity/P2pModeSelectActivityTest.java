package org.smartregister.p2p.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.google.android.gms.common.GoogleApiAvailability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.R;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.fragment.SyncCompleteTransferFragment;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.GoogleApiAvailabilityShadow;
import org.smartregister.p2p.shadows.ShadowAppDatabase;
import org.smartregister.p2p.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class, shadows = {ShadowAppDatabase.class, GoogleApiAvailabilityShadow.class})
public class P2pModeSelectActivityTest {

    private P2pModeSelectActivity activity;

    private P2pModeSelectContract.SenderPresenter senderPresenter;
    private P2pModeSelectContract.ReceiverPresenter receiverPresenter;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ReceiverTransferDao receiverTransferDao;

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application
                , "password", "username", Mockito.mock(P2PAuthorizationService.class)
                , receiverTransferDao, Mockito.mock(SenderTransferDao.class)));

        activity = Robolectric.buildActivity(P2pModeSelectActivity.class)
                .create()
                .start()
                .postCreate(null)
                .resume()
                .get();

        senderPresenter = Mockito.spy((P2pModeSelectContract.SenderPresenter)
                ReflectionHelpers.getField(activity, "senderBasePresenter"));
        ReflectionHelpers.setField(activity, "senderBasePresenter", senderPresenter);
        receiverPresenter = Mockito.spy((P2pModeSelectContract.ReceiverPresenter)
                ReflectionHelpers.getField(activity, "receiverBasePresenter"));
        ReflectionHelpers.setField(activity, "receiverBasePresenter", receiverPresenter);
    }

    @Test
    public void testCheckForPlayServicesCreatesAPlayServiceRequest() throws Exception {
        GoogleApiAvailability googleApiAvailability = Mockito.mock(GoogleApiAvailability.class);
        GoogleApiAvailabilityShadow.setInstance(googleApiAvailability);
        Whitebox.invokeMethod(activity, "checkForPlayServices");
        Mockito.verify(googleApiAvailability).makeGooglePlayServicesAvailable(Mockito.eq(activity));
    }

    @Test
    public void testGetPlayServicesVersionRequestPlayServiceVersion() throws Exception {
        PackageManager packageManager = Mockito.mock(PackageManager.class);
        PackageInfo packageInfo = Mockito.mock(PackageInfo.class);

        activity = Mockito.spy(activity);
        Mockito.doReturn(packageManager).when(activity).getPackageManager();
        Mockito.doReturn(packageInfo).when(packageManager).getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0);

        Whitebox.invokeMethod(activity, "getPlayServicesVersion");
        Mockito.verify(packageManager).getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0);
    }

    @Test
    public void testUpdatePlayStoreOrDieDisplaysAFatalDialogPositiveButton() throws Exception {
        activity = Mockito.spy(activity);
        AlertDialog alertDialog = Whitebox.invokeMethod(activity, "updatePlayStoreOrDie");
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
        Mockito.verify(activity).finish();
    }

    @Test
    public void testUpdatePlayStoreOrDieDisplaysAFatalDialogNegativeButton() throws Exception {
        activity = Mockito.spy(activity);
        AlertDialog alertDialog = Whitebox.invokeMethod(activity, "updatePlayStoreOrDie");
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).callOnClick();
        Mockito.verify(activity).finish();
    }

    @Test
    public void testOnMacAddressResolutionFailureWithWifiHW() throws Exception {
        PackageManager packageManager = Mockito.mock(PackageManager.class);

        activity = Mockito.spy(activity);
        Mockito.doReturn(packageManager).when(activity).getPackageManager();
        Mockito.doReturn(true).when(packageManager).hasSystemFeature(PackageManager.FEATURE_WIFI);

        Context context = Mockito.mock(Context.class);
        Mockito.doReturn(context).when(activity).getApplicationContext();

        WifiManager wifiManager =  Mockito.mock(WifiManager.class);
        Mockito.doReturn(wifiManager).when(context).getSystemService(Context.WIFI_SERVICE);

        //(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Whitebox.invokeMethod(activity, "onMacAddressResolutionFailure");

        Mockito.verify(activity).showFatalErrorDialog(R.string.an_error_occured, R.string.error_try_turning_wifi_on);
    }

    @Test
    public void addOnActivityRequestPermissionHandlerShouldAddPassedHandlerWithoutDuplicating() {
        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return 345;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                // Do nothing right now
            }
        };

        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler);
        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler);

        ArrayList<OnActivityRequestPermissionHandler> onActivityRequestPermissionHandlers =
                ReflectionHelpers.getField(activity, "onActivityRequestPermissionHandlers");

        assertEquals(1, onActivityRequestPermissionHandlers.size());
        assertEquals(onActivityRequestPermissionHandler, onActivityRequestPermissionHandlers.get(0));
    }


    @Test
    public void addOnActivityRequestPermissionHandlerShouldAddMultipleHandlers() {
        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return 345;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                // Do nothing right now
            }
        };

        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler2 = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return 346;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                // Do nothing for now
            }
        };


        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler);
        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler2);

        ArrayList<OnActivityRequestPermissionHandler> onActivityRequestPermissionHandlers =
                ReflectionHelpers.getField(activity, "onActivityRequestPermissionHandlers");

        assertEquals(2, onActivityRequestPermissionHandlers.size());
        assertEquals(onActivityRequestPermissionHandler2, onActivityRequestPermissionHandlers.get(1));
    }

    @Test
    public void onRequestPermissionResultShouldCallPermissionResultHandlers() {
        final int requestCode = 345;
        final List<Object> results = new ArrayList<>();
        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return requestCode;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                results.add(getRequestCode());
            }
        };

        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler2 = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return 346;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                results.add(getRequestCode());
            }
        };


        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler);
        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler2);

        activity.onRequestPermissionsResult(346, new String[]{}, new int[]{0});
        assertEquals(346, results.get(0));
    }

    @Test
    public void removeOnActivityRequestPermissionHandler() {
        final int requestCode = 345;
        final List<Object> results = new ArrayList<>();
        OnActivityRequestPermissionHandler onActivityRequestPermissionHandler = new OnActivityRequestPermissionHandler() {
            @Override
            public int getRequestCode() {
                return requestCode;
            }

            @Override
            public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                results.add("permission_handled");
            }
        };

        activity.addOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler);
        assertTrue(activity.removeOnActivityRequestPermissionHandler(onActivityRequestPermissionHandler));

        ArrayList<OnActivityRequestPermissionHandler> onActivityRequestPermissionHandlers =
                ReflectionHelpers.getField(activity, "onActivityRequestPermissionHandlers");

        assertEquals(0, onActivityRequestPermissionHandlers.size());
    }

    @Test
    public void getUnAuthorizedPermissionsShouldReturnStoragePermissionsIncludedWhenAvailableDataTypesMedia() {
        TreeSet<DataType> dataTypes = new TreeSet<>();
        dataTypes.add(new DataType("persons", DataType.Type.NON_MEDIA, 4));
        dataTypes.add(new DataType("image", DataType.Type.MEDIA, 5));

        Mockito.doReturn(dataTypes)
                .when(receiverTransferDao)
                .getDataTypes();

        List<String> permissions = activity.getUnauthorisedPermissions();
        boolean hasStoragePermissions = false;

        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                hasStoragePermissions = true;
            }
        }


        assertTrue(hasStoragePermissions);
    }

    @Test
    public void getUnAuthorizedPermissionsShouldReturnStoragePermissionsExclusiveWhenAvailableDataTypesHaveNoMediaType() {
        TreeSet<DataType> dataTypes = new TreeSet<>();
        dataTypes.add(new DataType("persons", DataType.Type.NON_MEDIA, 4));
        dataTypes.add(new DataType("transactions", DataType.Type.NON_MEDIA, 5));

        Mockito.doReturn(dataTypes)
                .when(receiverTransferDao)
                .getDataTypes();

        List<String> permissions = activity.getUnauthorisedPermissions();
        boolean hasStoragePermissions = false;

        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || permissions.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                hasStoragePermissions = true;
            }
        }


        assertFalse(hasStoragePermissions);
    }

    @Test
    public void onStopShouldCallPresenterOnStopMethods() {
        activity.onStop();

        Mockito.verify(senderPresenter, Mockito.times(1))
                .onStop();
        Mockito.verify(receiverPresenter, Mockito.times(1))
                .onStop();
    }

    @Test
    public void testShowSyncCompleteFragmentDisplaysAFragment(){
        activity = Mockito.spy(activity);
        activity.showSyncCompleteFragment(false , null, Mockito.mock(SyncCompleteTransferFragment.OnCloseClickListener.class), "summaryReport", false);
        Fragment fragmentByTag = activity.getSupportFragmentManager().findFragmentByTag(Constants.Fragment.SYNC_COMPLETE);
        Assert.assertNotNull(fragmentByTag);
        Assert.assertTrue(fragmentByTag instanceof SyncCompleteTransferFragment);
    }

}