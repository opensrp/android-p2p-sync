package org.smartregister.p2p.contract;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public interface BaseView {

    void initializePresenter();

    @NonNull
    Context getContext();
}
