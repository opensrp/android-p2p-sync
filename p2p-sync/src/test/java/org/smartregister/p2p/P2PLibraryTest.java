package org.smartregister.p2p;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 06/05/2019
 */

public class P2PLibraryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getInstanceShouldThrowExceptionWhenInstanceIsNull() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Instance does not exist!!! Call P2PLibrary.init method"
                + "in the onCreate method of "
                + "your Application class ");
        P2PLibrary.getInstance();
    }
}