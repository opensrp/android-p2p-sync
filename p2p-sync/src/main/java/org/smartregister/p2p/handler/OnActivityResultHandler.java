package org.smartregister.p2p.handler;

import android.content.Intent;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface OnActivityResultHandler {

    int getRequestCode();

    void handleActivityResult(int resultCode, Intent data);
}
