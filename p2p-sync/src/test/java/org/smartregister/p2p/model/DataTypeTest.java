package org.smartregister.p2p.model;

import org.junit.Test;

import java.util.Iterator;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 29/03/2019
 */

public class DataTypeTest {
    
    @Test
    public void dataTypesShouldBeOrderedInAscendingOrderBasedOnTheirPositionProperty() {
        TreeSet<DataType> dataSyncOrder;

        DataType event = new DataType("event", DataType.Type.NON_MEDIA, 1);
        DataType client = new DataType("client", DataType.Type.NON_MEDIA, 2);
        DataType profilePic = new DataType("profile-pic", DataType.Type.MEDIA, 3);

        dataSyncOrder = new TreeSet<>();
        dataSyncOrder.add(profilePic);
        dataSyncOrder.add(client);
        dataSyncOrder.add(event);

        Iterator<DataType> iterator = dataSyncOrder.iterator();

        assertEquals(1, iterator.next().getPosition());
        assertEquals(2, iterator.next().getPosition());
        assertEquals(3, iterator.next().getPosition());
    }
}