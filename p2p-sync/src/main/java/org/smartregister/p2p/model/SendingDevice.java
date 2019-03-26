package org.smartregister.p2p.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */
@Entity(tableName = "sending_devices")
public class SendingDevice {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "device_unique_id", index = true)
    private String deviceUniqueId;

    @ColumnInfo(name = "app_lifetime_key")
    private String appLifetimeKey;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceUniqueId() {
        return deviceUniqueId;
    }

    public void setDeviceUniqueId(String deviceUniqueId) {
        this.deviceUniqueId = deviceUniqueId;
    }

    public String getAppLifetimeKey() {
        return appLifetimeKey;
    }

    public void setAppLifetimeKey(String appLifetimeKey) {
        this.appLifetimeKey = appLifetimeKey;
    }
}
