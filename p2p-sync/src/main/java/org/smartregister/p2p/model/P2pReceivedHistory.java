package org.smartregister.p2p.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Entity(tableName = "p2p_received_history")
public class P2pReceivedHistory {

    @PrimaryKey
    private int id;

    @ColumnInfo(name = "sending_device_id")
    private int sendingDeviceId;

    @ColumnInfo(name = "entity_name")
    private String entityName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSendingDeviceId() {
        return sendingDeviceId;
    }

    public void setSendingDeviceId(int sendingDeviceId) {
        this.sendingDeviceId = sendingDeviceId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}
