package org.smartregister.p2p.sync.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.smartregister.p2p.model.DataType;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */
public class SyncPackageManifest {

    private long payloadId;
    private String payloadExtension;
    private DataType dataType;
    private HashMap<String, Object> payloadDetails;

    public SyncPackageManifest(long payloadId, @NonNull String payloadExtension, @NonNull DataType dataType) {
        this.payloadId = payloadId;
        this.payloadExtension = payloadExtension;
        this.dataType = dataType;
    }

    public long getPayloadId() {
        return payloadId;
    }

    public String getPayloadExtension() {
        return payloadExtension;
    }

    public DataType getDataType() {
        return dataType;
    }

    @Nullable
    public HashMap<String, Object> getPayloadDetails() {
        return payloadDetails;
    }

    public void setPayloadDetails(@Nullable HashMap<String, Object> payloadDetails) {
        this.payloadDetails = payloadDetails;
    }
}
