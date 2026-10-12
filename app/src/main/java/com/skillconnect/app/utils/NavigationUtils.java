package com.skillconnect.app.utils;

import android.app.Activity;
import android.content.Intent;

import com.skillconnect.app.R;
import com.skillconnect.app.activities.AdminActivity;
import com.skillconnect.app.activities.CustomerHomeActivity;
import com.skillconnect.app.activities.LoginActivity;
import com.skillconnect.app.activities.ProviderHomeActivity;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.User;

/** Explicit-intent navigation used after login / logout. */
public final class NavigationUtils {

    private NavigationUtils() {
    }

    /** Opens the role-specific home screen and clears the back stack. */
    public static void goToHome(Activity activity, User user) {
        Class<?> target;
        if (user.isAdmin()) target = AdminActivity.class;
        else if (user.isProvider()) target = ProviderHomeActivity.class;
        else target = CustomerHomeActivity.class;
        Intent intent = new Intent(activity, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void goToLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void confirmLogout(Activity activity) {
        UiUtils.confirm(activity, activity.getString(R.string.logout), activity.getString(R.string.logout_confirm),
                activity.getString(R.string.logout), () -> {
                    RepositoryProvider.get().logout();
                    RepositoryProvider.setDemoSession(false);
                    UiUtils.toast(activity, "Logged out");
                    goToLogin(activity);
                });
    }
}
