package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import org.smartregister.p2p.model.DataType;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */
public class SyncPackageManifest {

    private long payloadId;
    private String payloadExtension;
    private DataType dataType;

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
}
