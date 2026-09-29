package com.skillconnect.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Small key-value settings stored with SharedPreferences. */
public final class PrefsManager {

    private static final String FILE = "skillconnect_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_DEMO_SESSION = "demo_session";
    private static final String KEY_DEMO_USER = "demo_user_id";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LNG = "last_lng";
    private static final String KEY_LOCALITY = "last_locality";
    private static final String KEY_LOCATION_ASKED = "location_asked";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_REMINDED = "reminded_bookings";

    private static PrefsManager instance;
    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static synchronized PrefsManager get(Context context) {
        if (instance == null) instance = new PrefsManager(context);
        return instance;
    }

    public boolean isOnboardingDone() { return prefs.getBoolean(KEY_ONBOARDING_DONE, false); }
    public void setOnboardingDone(boolean done) { prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply(); }

    public boolean isDemoSession() { return prefs.getBoolean(KEY_DEMO_SESSION, false); }
    public void setDemoSession(boolean demo) { prefs.edit().putBoolean(KEY_DEMO_SESSION, demo).apply(); }

    public String getDemoUserId() { return prefs.getString(KEY_DEMO_USER, null); }
    public void setDemoUserId(String id) { prefs.edit().putString(KEY_DEMO_USER, id).apply(); }

    public boolean hasSavedLocation() { return prefs.contains(KEY_LAT); }
    public double getLatitude(double fallback) { return Double.longBitsToDouble(prefs.getLong(KEY_LAT, Double.doubleToLongBits(fallback))); }
    public double getLongitude(double fallback) { return Double.longBitsToDouble(prefs.getLong(KEY_LNG, Double.doubleToLongBits(fallback))); }
    public String getLocality() { return prefs.getString(KEY_LOCALITY, null); }

    public void saveLocation(double lat, double lng, String locality) {
        SharedPreferences.Editor e = prefs.edit()
                .putLong(KEY_LAT, Double.doubleToLongBits(lat))
                .putLong(KEY_LNG, Double.doubleToLongBits(lng));
        if (locality != null) e.putString(KEY_LOCALITY, locality);
        e.apply();
    }

    public boolean wasLocationAsked() { return prefs.getBoolean(KEY_LOCATION_ASKED, false); }
    public void setLocationAsked() { prefs.edit().putBoolean(KEY_LOCATION_ASKED, true).apply(); }

    public boolean areNotificationsEnabled() { return prefs.getBoolean(KEY_NOTIFICATIONS, true); }
    public void setNotificationsEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply(); }

    /** Returns true the first time it is called for a booking id (used for one-off reminders). */
    public boolean markReminded(String bookingId) {
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_REMINDED, new HashSet<>()));
        if (set.contains(bookingId)) return false;
        set.add(bookingId);
        prefs.edit().putStringSet(KEY_REMINDED, set).apply();
        return true;
    }
}
