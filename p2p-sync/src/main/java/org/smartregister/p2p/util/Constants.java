package org.smartregister.p2p.util;

import com.google.android.gms.nearby.connection.Strategy;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 11/03/2019
 */

public abstract class Constants {

    public static final Strategy STRATEGY = Strategy.P2P_STAR;

    public abstract static class RQ_CODE {
        public static final int PERMISSIONS = 2;
        public static final int LOCATION_SETTINGS = 3;
    }
}
