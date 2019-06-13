package org.smartregister.p2p.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-06-12
 */

@RunWith(RobolectricTestRunner.class)
public class SyncDataConverterUtilTest {

    @Test
    public void generateSummaryReportShouldReturnNumberFormattedStringWithCorrectVerb() {
        HashMap<String, Integer> transferItems = new HashMap<>();

        transferItems.put("mangoes", 56);
        transferItems.put("oranges", 1023);

        String actual = SyncDataConverterUtil.generateSummaryReport(RuntimeEnvironment.application, false, transferItems);
        Assert.assertEquals("1,079 records received", actual);
    }
}