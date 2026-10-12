package com.skillconnect.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.skillconnect.app.models.FavoriteProvider;
import com.skillconnect.app.models.Provider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * LOCAL SQLite database (offline data). Firebase remains the main cloud database;
 * SQLite stores data that belongs to this device:
 * <ul>
 *   <li>favorite_providers – providers the user saved (♡)</li>
 *   <li>recently_viewed – providers the user opened recently</li>
 *   <li>recent_searches – search history</li>
 * </ul>
 * Demonstrates full CRUD: INSERT, SELECT, UPDATE and DELETE.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "skillconnect_local.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_FAVORITES = "favorite_providers";
    public static final String TABLE_RECENT = "recently_viewed";
    public static final String TABLE_SEARCHES = "recent_searches";

    private static final String COL_ID = "id";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_PROVIDER_ID = "provider_id";
    private static final String COL_NAME = "name";
    private static final String COL_SKILL = "skill";
    private static final String COL_CATEGORY = "category";
    private static final String COL_IMAGE_URL = "image_url";
    private static final String COL_RATING = "rating";
    private static final String COL_PRICE = "price";
    private static final String COL_PRICE_UNIT = "price_unit";
    private static final String COL_TIME = "saved_at";
    private static final String COL_QUERY = "query";

    private static final int MAX_RECENT_VIEWS = 10;
    private static final int MAX_RECENT_SEARCHES = 8;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String providerColumns = COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_ID + " TEXT NOT NULL, "
                + COL_PROVIDER_ID + " TEXT NOT NULL, "
                + COL_NAME + " TEXT, "
                + COL_SKILL + " TEXT, "
                + COL_CATEGORY + " TEXT, "
                + COL_IMAGE_URL + " TEXT, "
                + COL_RATING + " REAL, "
                + COL_PRICE + " REAL, "
                + COL_PRICE_UNIT + " TEXT, "
                + COL_TIME + " INTEGER, "
                + "UNIQUE(" + COL_USER_ID + ", " + COL_PROVIDER_ID + ")";
        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " (" + providerColumns + ")");
        db.execSQL("CREATE TABLE " + TABLE_RECENT + " (" + providerColumns + ")");
        db.execSQL("CREATE TABLE " + TABLE_SEARCHES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_ID + " TEXT NOT NULL, "
                + COL_QUERY + " TEXT NOT NULL, "
                + COL_TIME + " INTEGER, "
                + "UNIQUE(" + COL_USER_ID + ", " + COL_QUERY + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCHES);
        onCreate(db);
    }

    private static String uid(String userId) {
        return userId != null ? userId : "guest";
    }

    private ContentValues providerValues(String userId, FavoriteProvider f) {
        ContentValues v = new ContentValues();
        v.put(COL_USER_ID, uid(userId));
        v.put(COL_PROVIDER_ID, f.getProviderId());
        v.put(COL_NAME, f.getName());
        v.put(COL_SKILL, f.getSkill());
        v.put(COL_CATEGORY, f.getCategory());
        v.put(COL_IMAGE_URL, f.getImageUrl());
        v.put(COL_RATING, f.getRating());
        v.put(COL_PRICE, f.getPrice());
        v.put(COL_PRICE_UNIT, f.getPriceUnit());
        v.put(COL_TIME, System.currentTimeMillis());
        return v;
    }

    private List<FavoriteProvider> readProviders(Cursor c) {
        List<FavoriteProvider> list = new ArrayList<>();
        while (c.moveToNext()) {
            FavoriteProvider f = new FavoriteProvider();
            f.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
            f.setProviderId(c.getString(c.getColumnIndexOrThrow(COL_PROVIDER_ID)));
            f.setName(c.getString(c.getColumnIndexOrThrow(COL_NAME)));
            f.setSkill(c.getString(c.getColumnIndexOrThrow(COL_SKILL)));
            f.setCategory(c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)));
            f.setImageUrl(c.getString(c.getColumnIndexOrThrow(COL_IMAGE_URL)));
            f.setRating(c.getDouble(c.getColumnIndexOrThrow(COL_RATING)));
            f.setPrice(c.getDouble(c.getColumnIndexOrThrow(COL_PRICE)));
            f.setPriceUnit(c.getString(c.getColumnIndexOrThrow(COL_PRICE_UNIT)));
            f.setSavedAt(c.getLong(c.getColumnIndexOrThrow(COL_TIME)));
            list.add(f);
        }
        c.close();
        return list;
    }

    // ================================================================ FAVORITES

    /** INSERT */
    public boolean addFavorite(String userId, Provider provider) {
        long id = getWritableDatabase().insertWithOnConflict(TABLE_FAVORITES, null,
                providerValues(userId, FavoriteProvider.fromProvider(provider)), SQLiteDatabase.CONFLICT_REPLACE);
        return id != -1;
    }

    /** SELECT (newest first) */
    public List<FavoriteProvider> getFavorites(String userId) {
        Cursor c = getReadableDatabase().query(TABLE_FAVORITES, null, COL_USER_ID + "=?",
                new String[]{uid(userId)}, null, null, COL_TIME + " DESC");
        return readProviders(c);
    }

    /** SELECT one */
    public boolean isFavorite(String userId, String providerId) {
        Cursor c = getReadableDatabase().query(TABLE_FAVORITES, new String[]{COL_ID},
                COL_USER_ID + "=? AND " + COL_PROVIDER_ID + "=?", new String[]{uid(userId), providerId},
                null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** UPDATE – refreshes the stored snapshot (name, rating, price…) when a profile is viewed. */
    public int updateFavoriteSnapshot(String userId, Provider provider) {
        ContentValues v = new ContentValues();
        v.put(COL_NAME, provider.getName());
        v.put(COL_SKILL, provider.getTitle());
        v.put(COL_CATEGORY, provider.getCategory());
        v.put(COL_IMAGE_URL, provider.getProfileImage());
        v.put(COL_RATING, provider.getAverageRating());
        v.put(COL_PRICE, provider.getStartingPrice());
        v.put(COL_PRICE_UNIT, provider.getPriceUnit());
        return getWritableDatabase().update(TABLE_FAVORITES, v,
                COL_USER_ID + "=? AND " + COL_PROVIDER_ID + "=?", new String[]{uid(userId), provider.getProviderId()});
    }

    /** DELETE */
    public int removeFavorite(String userId, String providerId) {
        return getWritableDatabase().delete(TABLE_FAVORITES,
                COL_USER_ID + "=? AND " + COL_PROVIDER_ID + "=?", new String[]{uid(userId), providerId});
    }

    // =========================================================== RECENTLY VIEWED

    public void addRecentlyViewed(String userId, Provider provider) {
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict(TABLE_RECENT, null, providerValues(userId, FavoriteProvider.fromProvider(provider)),
                SQLiteDatabase.CONFLICT_REPLACE);
        // Keep only the latest MAX_RECENT_VIEWS rows
        db.execSQL("DELETE FROM " + TABLE_RECENT + " WHERE " + COL_USER_ID + "=? AND " + COL_ID + " NOT IN (SELECT "
                + COL_ID + " FROM " + TABLE_RECENT + " WHERE " + COL_USER_ID + "=? ORDER BY " + COL_TIME
                + " DESC LIMIT " + MAX_RECENT_VIEWS + ")", new Object[]{uid(userId), uid(userId)});
    }

    public List<FavoriteProvider> getRecentlyViewed(String userId) {
        Cursor c = getReadableDatabase().query(TABLE_RECENT, null, COL_USER_ID + "=?",
                new String[]{uid(userId)}, null, null, COL_TIME + " DESC");
        return readProviders(c);
    }

    /** Categories of recently viewed providers – used for "Recommended for You". */
    public List<String> getRecentCategories(String userId) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (FavoriteProvider f : getRecentlyViewed(userId)) {
            if (f.getCategory() != null) set.add(f.getCategory());
        }
        for (FavoriteProvider f : getFavorites(userId)) {
            if (f.getCategory() != null) set.add(f.getCategory());
        }
        return new ArrayList<>(set);
    }

    public void clearRecentlyViewed(String userId) {
        getWritableDatabase().delete(TABLE_RECENT, COL_USER_ID + "=?", new String[]{uid(userId)});
    }

    // =========================================================== RECENT SEARCHES

    /** UPDATE the timestamp if the search exists, otherwise INSERT it. */
    public void addRecentSearch(String userId, String query) {
        if (query == null || query.trim().isEmpty()) return;
        String q = query.trim();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TIME, System.currentTimeMillis());
        int updated = db.update(TABLE_SEARCHES, v, COL_USER_ID + "=? AND " + COL_QUERY + "=? COLLATE NOCASE",
                new String[]{uid(userId), q});
        if (updated == 0) {
            v.put(COL_USER_ID, uid(userId));
            v.put(COL_QUERY, q);
            db.insert(TABLE_SEARCHES, null, v);
        }
        db.execSQL("DELETE FROM " + TABLE_SEARCHES + " WHERE " + COL_USER_ID + "=? AND " + COL_ID + " NOT IN (SELECT "
                + COL_ID + " FROM " + TABLE_SEARCHES + " WHERE " + COL_USER_ID + "=? ORDER BY " + COL_TIME
                + " DESC LIMIT " + MAX_RECENT_SEARCHES + ")", new Object[]{uid(userId), uid(userId)});
    }

    public List<String> getRecentSearches(String userId) {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_SEARCHES, new String[]{COL_QUERY}, COL_USER_ID + "=?",
                new String[]{uid(userId)}, null, null, COL_TIME + " DESC");
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    public void deleteRecentSearch(String userId, String query) {
        getWritableDatabase().delete(TABLE_SEARCHES, COL_USER_ID + "=? AND " + COL_QUERY + "=?",
                new String[]{uid(userId), query});
    }

    public void clearRecentSearches(String userId) {
        getWritableDatabase().delete(TABLE_SEARCHES, COL_USER_ID + "=?", new String[]{uid(userId)});
    }
}
