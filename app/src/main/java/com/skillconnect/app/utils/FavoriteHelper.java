package com.skillconnect.app.utils;

import android.content.Context;

import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.Provider;

/** Save / unsave a provider (♡ → ❤). Stored in SQLite and backed up to Firestore. */
public final class FavoriteHelper {

    private FavoriteHelper() {
    }

    /** @return true if the provider is now saved. */
    public static boolean toggle(Context context, Provider provider) {
        String uid = RepositoryProvider.get().getCurrentUserId();
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        boolean nowSaved;
        if (db.isFavorite(uid, provider.getProviderId())) {
            db.removeFavorite(uid, provider.getProviderId());
            nowSaved = false;
            UiUtils.toast(context, "Removed from saved providers");
        } else {
            db.addFavorite(uid, provider);
            nowSaved = true;
            UiUtils.toast(context, "Provider added to favorites");
        }
        RepositoryProvider.get().syncFavorite(provider.getProviderId(), nowSaved);
        return nowSaved;
    }
}
