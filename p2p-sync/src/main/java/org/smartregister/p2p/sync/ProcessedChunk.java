package org.smartregister.p2p.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.nearby.connection.Payload;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 29/04/2019
 */

public class ProcessedChunk {

    private int type;
    private String jsonData;
    private Payload fileData;


    public ProcessedChunk(int type, @NonNull String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }

    public ProcessedChunk(int type, @NonNull Payload fileData) {
        this.type = type;
        this.fileData = fileData;
    }

    public int getType() {
        return type;
    }

    @Nullable
    public String getJsonData() {
        return jsonData;
    }

    @Nullable
    public Payload getFileData() {
        return fileData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }
}
