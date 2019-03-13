package org.smartregister.p2p.activity;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.p2p.TestApplication;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class P2pModeSelectActivityTest {

    private P2pModeSelectActivity activity;

    @Before
    public void setUp() throws Exception {
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
}