package org.smartregister.p2p.model.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Dao
public interface P2pReceivedHistoryDao {

    @Query("DELETE FROM p2p_received_history WHERE sending_device_id = :sendingDeviceId")
    int clearDeviceRecords(int sendingDeviceId);
}
