package org.smartregister.p2p.util;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */

public abstract class DialogUtils {

    public static void dismissAllDialogs(@NonNull FragmentManager fragmentManager) {
        List<Fragment> fragments = fragmentManager.getFragments();

        for (Fragment fragment: fragments) {
            if (fragment instanceof DialogFragment) {
                ((DialogFragment) fragment).dismissAllowingStateLoss();
            }

            FragmentManager childFragmentManager = fragment.getChildFragmentManager();
            dismissAllDialogs(childFragmentManager);
        }
    }
}
