package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.skillconnect.app.BuildConfig;
import com.skillconnect.app.R;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.UiUtils;

/** App settings: notifications, local data (SQLite) and about. */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        PrefsManager prefs = PrefsManager.get(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        String uid = RepositoryProvider.get().getCurrentUserId();

        MaterialSwitch switchNotifications = findViewById(R.id.switchNotifications);
        switchNotifications.setChecked(prefs.areNotificationsEnabled());
        switchNotifications.setOnCheckedChangeListener((b, checked) -> prefs.setNotificationsEnabled(checked));

        menu(R.id.menuClearSearches, R.drawable.ic_search, getString(R.string.clear_recent_searches), () -> {
            db.clearRecentSearches(uid);
            UiUtils.toast(this, "Recent searches cleared");
        });
        menu(R.id.menuClearRecent, R.drawable.ic_history, getString(R.string.clear_recently_viewed), () -> {
            db.clearRecentlyViewed(uid);
            UiUtils.toast(this, "Recently viewed cleared");
        });
        menu(R.id.menuReplayOnboarding, R.drawable.ic_history, getString(R.string.replay_onboarding), () ->
                startActivity(new Intent(this, OnboardingActivity.class).putExtra(OnboardingActivity.EXTRA_REPLAY, true)));
        menu(R.id.menuAbout, R.drawable.ic_info, getString(R.string.about_app), () ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.tagline) + "\n\nVersion " + BuildConfig.VERSION_NAME
                                + " (MVP)\n\nA location-based marketplace connecting people who need a skill "
                                + "with people who provide it. Built with Java, XML, Firebase, Google Maps and SQLite.")
                        .setPositiveButton(R.string.close, null)
                        .show());
        menu(R.id.menuLogout, R.drawable.ic_logout, getString(R.string.logout), () -> NavigationUtils.confirmLogout(this));

        ((TextView) findViewById(R.id.tvDataSource)).setText(RepositoryProvider.isDemoMode()
                ? "Demo mode – using sample data stored in memory on this device. Add google-services.json to use Firebase."
                : "Firebase – Authentication, Cloud Firestore and Storage. Previously loaded data is cached for offline use.");
    }

    private void menu(int id, int icon, String title, Runnable action) {
        View row = findViewById(id);
        ((ImageView) row.findViewById(R.id.menuIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.menuTitle)).setText(title);
        row.setOnClickListener(v -> action.run());
    }
}
