package org.smartregister.p2p.shadows;


import com.google.android.gms.common.GoogleApiAvailability;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(GoogleApiAvailability.class)
public class GoogleApiAvailabilityShadow {

    private static GoogleApiAvailability instance = Mockito.mock(GoogleApiAvailability.class);

    @Implementation
    public static GoogleApiAvailability getInstance() {
        return instance;
    }

    public static void setInstance(GoogleApiAvailability instance) {
        GoogleApiAvailabilityShadow.instance = instance;
    }
}
