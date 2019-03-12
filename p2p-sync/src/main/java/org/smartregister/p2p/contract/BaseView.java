package org.smartregister.p2p.contract;

import android.content.Context;
import android.support.annotation.NonNull;

import org.smartregister.p2p.handler.ActivityRequestPermissionResultHandler;
import org.smartregister.p2p.handler.ActivityResultHandler;
import org.smartregister.p2p.handler.ActivityResumeHandler;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface BaseView extends ActivityResultHandler, ActivityResumeHandler, ActivityRequestPermissionResultHandler {

    void initializePresenter();

    @NonNull
    Context getContext();
}
