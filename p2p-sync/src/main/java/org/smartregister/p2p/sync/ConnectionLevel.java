package org.smartregister.p2p.sync;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 20/03/2019
 */
public enum ConnectionLevel {
    CONNECT_BEFORE_AUTHENTICATE,
    AUTHENTICATED,
    AUTHORIZED,
    SENT_HASH_KEY,
    RECEIVED_HASH_KEY,
    SENT_RECEIVED_HISTORY,
    RECEIPT_OF_RECEIVED_HISTORY
}
