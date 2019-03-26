package org.smartregister.p2p.callback;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

public interface ConnectionLevelChangeCallback {

    void onAuthenticated();

    void onAuthorized();

    void onHashKeyReceived();
}
