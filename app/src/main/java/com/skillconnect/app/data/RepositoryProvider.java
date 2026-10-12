package com.skillconnect.app.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.skillconnect.app.firebase.FirebaseRepository;
import com.skillconnect.app.firebase.FirestoreHelper;
import com.skillconnect.app.utils.PrefsManager;

/**
 * Decides which backend the app talks to.
 *
 * FIREBASE MODE: google-services.json was added, so FirebaseApp initialised and all
 * data is read from / written to Firebase.
 *
 * DEMO MODE: Firebase is not configured (or the user tapped "Explore with a demo
 * account"). Sample data from {@link DemoData} is used and nothing is sent to a server.
 */
public final class RepositoryProvider {

    private static Context appContext;
    private static boolean firebaseAvailable;
    private static FirebaseRepository firebaseRepository;
    private static DemoRepository demoRepository;

    private RepositoryProvider() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        firebaseAvailable = !FirebaseApp.getApps(appContext).isEmpty();
        if (firebaseAvailable) {
            FirestoreHelper.enableOfflinePersistence();
        }
    }

    /** True when google-services.json is present and Firebase initialised. */
    public static boolean isFirebaseAvailable() {
        return firebaseAvailable;
    }

    public static boolean isDemoMode() {
        return !firebaseAvailable || PrefsManager.get(appContext).isDemoSession();
    }

    public static void setDemoSession(boolean demo) {
        PrefsManager.get(appContext).setDemoSession(demo);
    }

    public static synchronized AppRepository get() {
        if (isDemoMode()) {
            if (demoRepository == null) demoRepository = new DemoRepository(appContext);
            return demoRepository;
        }
        if (firebaseRepository == null) firebaseRepository = new FirebaseRepository();
        return firebaseRepository;
    }
}
