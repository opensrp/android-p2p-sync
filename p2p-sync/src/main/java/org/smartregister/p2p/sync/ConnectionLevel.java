package org.smartregister.p2p.sync;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 20/03/2019
 */
public enum ConnectionLevel {
    AUTHENTICATED,
    AUTHORIZED,
    RECEIVED_HASH_KEY,
    SENT_HASH_KEY
}
