package org.smartregister.p2p.sample.util;

import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.RecalledIdentifier;

public class FailSafeRecallableID implements RecalledIdentifier {
    @NonNull
    @Override
    public String getUniqueID() {
        return null;
    }
}
