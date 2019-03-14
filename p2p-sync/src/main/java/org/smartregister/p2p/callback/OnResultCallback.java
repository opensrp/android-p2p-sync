package org.smartregister.p2p.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public interface OnResultCallback {

    void onSuccess(@Nullable Object object);

    void onFailure(@NonNull Exception e);
}
