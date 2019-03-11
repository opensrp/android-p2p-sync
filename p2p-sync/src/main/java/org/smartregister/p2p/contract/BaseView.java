package org.smartregister.p2p.contract;

import android.content.Context;
import android.support.annotation.NonNull;

import org.smartregister.p2p.handlers.ActivityResultHandler;
import org.smartregister.p2p.handlers.ActivityResumeHandler;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface BaseView extends ActivityResultHandler, ActivityResumeHandler {

    void initializePresenter();

    @NonNull
    Context getContext();
}
