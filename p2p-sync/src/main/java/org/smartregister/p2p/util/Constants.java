package org.smartregister.p2p.util;

import com.google.android.gms.nearby.connection.Strategy;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public interface Constants {

    Strategy STRATEGY = Strategy.P2P_STAR;

    interface RQ_CODE {
        int PERMISSIONS = 2;
        int LOCATION_SETTINGS = 3;
    }
}
