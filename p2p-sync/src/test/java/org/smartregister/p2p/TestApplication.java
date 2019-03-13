package org.smartregister.p2p;

import android.app.Application;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 13/03/2019
 */

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        setTheme(R.style.Theme_AppCompat);
    }
}
