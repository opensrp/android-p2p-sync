package org.smartregister.p2p.dialog;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class StartReceiveModeProgressDialogTest {

    private StartReceiveModeProgressDialog startReceiveModeProgressDialog;

    @Before
    public void setUp() throws Exception {
        FragmentActivity activity = Robolectric
                .buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        startReceiveModeProgressDialog = new StartReceiveModeProgressDialog();
        startReceiveModeProgressDialog.show(fragmentManager, "dialog_receive_progress");
    }

    @Test
    public void onCreateDialogShouldMakeFragmentDialogNotCancelable() {
        Assert.assertFalse(startReceiveModeProgressDialog.isCancelable());
    }
}