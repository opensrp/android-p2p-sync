package org.smartregister.p2p.sample.dao;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.smartregister.p2p.model.DataType;
import org.smartregister.p2p.model.dao.SenderTransferDao;
import org.smartregister.p2p.sync.JsonData;
import org.smartregister.p2p.sync.MultiMediaData;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/03/2019
 */

public class SampleSenderDao implements SenderTransferDao {

    private List<String> records = new ArrayList<>();

    public SampleSenderDao() {
        records.add("John Doe");
        records.add("Jane Doe");
        records.add("Sarah Platlin");
        records.add("Rose Wambui");
        records.add("Leo Atieno");
        records.add("Chris Wamaitha");
    }

    @Nullable
    @Override
    public TreeSet<DataType> getDataTypes() {
        TreeSet<DataType> dataTypes = new TreeSet<>();
        dataTypes.add(new DataType("names", DataType.Type.NON_MEDIA, 0));

        return dataTypes;
    }

    @Nullable
    @Override
    public JsonData getJsonData(@NonNull DataType dataType, long lastRecordId, int batchSize) {
        if (lastRecordId != 0) {
            return null;
        } else {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(records.get(0));
            jsonArray.put(records.get(1));
            jsonArray.put(records.get(2));
            jsonArray.put(records.get(3));
            jsonArray.put(records.get(4));
            jsonArray.put(records.get(5));

            return new JsonData(jsonArray, 4);
        }
    }

    @Nullable
    @Override
    public MultiMediaData getMultiMediaData(@NonNull DataType dataType, long lastRecordId) {
        return null;
    }
}
