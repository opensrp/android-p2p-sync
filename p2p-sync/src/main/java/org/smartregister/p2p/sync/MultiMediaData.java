package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;

import java.io.File;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class MultiMediaData {

    private File file;
    private long fileRecordId;

    public MultiMediaData(@NonNull File file, long fileRecordId) {
        this.file = file;
        this.fileRecordId = fileRecordId;
    }

    @NonNull
    public File getFile() {
        return file;
    }

    public long getRecordId() {
        return fileRecordId;
    }
}
