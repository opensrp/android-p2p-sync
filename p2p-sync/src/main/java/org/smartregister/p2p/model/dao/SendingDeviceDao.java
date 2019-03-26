package org.smartregister.p2p.model.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;

import org.smartregister.p2p.model.SendingDevice;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Dao
public interface SendingDeviceDao {

    @Insert
    void insert(@NonNull SendingDevice sendingDevice);

    @Update
    void update(@NonNull SendingDevice sendingDevice);

    @Query("SELECT * FROM sending_devices WHERE device_unique_id = :deviceUniqueId")
    SendingDevice getSendingDevice(@NonNull String deviceUniqueId);
}
