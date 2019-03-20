package org.smartregister.p2p.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 20/03/2019
 */

@RunWith(RobolectricTestRunner.class)
public class PermissionsTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Context context;

    @Test
    public void getUnauthorizedCriticalPermissionsShouldReturnOnlyPermissionsWithoutGrantedFlagWhenGivenAllCriticalPermissions() {
        String[] CRITICAL_PERMISSIONS = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };

        Mockito.doReturn(PackageManager.PERMISSION_DENIED)
                .when(context)
                .checkPermission(ArgumentMatchers.matches(String.format("(%s|%s|%s)"
                        , Manifest.permission.BLUETOOTH
                        , Manifest.permission.ACCESS_WIFI_STATE
                        , Manifest.permission.CHANGE_WIFI_STATE))
                        , Mockito.anyInt()
                        , Mockito.anyInt());



        List<String> unauthorizedPermissions = Permissions.getUnauthorizedCriticalPermissions(context, CRITICAL_PERMISSIONS);

        assertEquals(3, unauthorizedPermissions.size());
        assertEquals(Manifest.permission.BLUETOOTH, unauthorizedPermissions.get(0));
        assertEquals(Manifest.permission.ACCESS_WIFI_STATE, unauthorizedPermissions.get(1));
        assertEquals(Manifest.permission.CHANGE_WIFI_STATE, unauthorizedPermissions.get(2));
    }

    @Test
    public void isPermissionGrantedShouldReturnTrueWhenPermissionHasGrantedFlag() {
        Mockito.doReturn(PackageManager.PERMISSION_GRANTED)
                .when(context)
                .checkPermission(Mockito.anyString()
                        , Mockito.anyInt()
                        , Mockito.anyInt());

        assertTrue(Permissions.isPermissionGranted(context, Manifest.permission.BLUETOOTH));
    }

    @Test
    public void isPermissionGrantedShouldReturnFalseWhenPermissionHasDeniedFlag() {
        Mockito.doReturn(PackageManager.PERMISSION_DENIED)
                .when(context)
                .checkPermission(Mockito.anyString()
                        , Mockito.anyInt()
                        , Mockito.anyInt());

        assertFalse(Permissions.isPermissionGranted(context, Manifest.permission.BLUETOOTH));
    }

}