package org.smartregister.p2p.activity;

import android.Manifest;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.authorizer.P2PAuthorizationService;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.ReceiverTransferDao;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.shadows.ShadowAppDatabase;

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
@Config(application = TestApplication.class, shadows = {ShadowAppDatabase.class})
public class P2pModeSelectActivityTest {

    private P2pModeSelectActivity activity;
    
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ReceiverTransferDao receiverTransferDao;

    @Before
    public void setUp() throws Exception {
        P2PLibrary.init(new P2PLibrary.Options(RuntimeEnvironment.application
                ,"password","username", Mockito.mock(P2PAuthorizationService.class)
                , receiverTransferDao, Mockito.mock(SenderTransferDao.class)));

        activity = Robolectric.buildActivity(P2pModeSelectActivity.class)
                .create()
                .start()
                .resume()
                .get();
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

        for (String permission: permissions) {
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

        for (String permission: permissions) {
            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || permissions.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                hasStoragePermissions = true;
            }
        }


        assertFalse(hasStoragePermissions);
    }
}