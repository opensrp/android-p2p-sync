package org.smartregister.p2p.shadows;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.p2p.model.AppDatabase;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019
 */

@Implements(AppDatabase.class)
public class ShadowAppDatabase {

    private static AppDatabase instance;

    @Implementation
    public static AppDatabase getInstance(@NonNull Context context, @NonNull String passphrase) {
        if (instance == null) {
            instance = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                    .build();
        }

        return instance;
    }
}
