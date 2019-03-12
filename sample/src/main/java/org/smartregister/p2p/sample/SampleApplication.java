package org.smartregister.p2p.sample;

import android.app.Application;

import org.smartregister.p2p.P2PLibrary;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        P2PLibrary.init(new P2PLibrary.ReceiverOptions("John Doe"));
    }
}
