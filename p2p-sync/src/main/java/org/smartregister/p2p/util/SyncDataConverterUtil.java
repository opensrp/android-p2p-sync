package org.smartregister.p2p.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.smartregister.p2p.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/04/2019
 */

public class SyncDataConverterUtil {

    @NonNull
    public static String readInputStreamAsString(InputStream in)
            throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while(result != -1) {
            byte b = (byte)result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    @NonNull
    public static String generateSummaryReport(@NonNull Context context, @Nullable HashMap<String, Integer> transferItems) {
        String transferSummary = context.getString(R.string.transfer_summary_content);

        if (transferItems != null) {
            StringBuilder stringBuilder = new StringBuilder();
            int total = 0;
            for (String key: transferItems.keySet()) {
                total += transferItems.get(key);

            }

            return String.format(transferSummary, String.format(Locale.US, "\n%,d records", total));
        }

        return String.format(transferSummary, "\n0 records transferred");
    }
}
