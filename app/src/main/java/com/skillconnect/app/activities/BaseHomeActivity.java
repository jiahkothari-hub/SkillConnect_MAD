package com.skillconnect.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skillconnect.app.R;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.data.Subscription;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.User;
import com.skillconnect.app.notifications.NotificationHelper;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared logic for the customer and provider home screens:
 * bottom navigation with fragments, one real-time bookings listener, and
 * local notifications when a booking changes.
 */
public abstract class BaseHomeActivity extends AppCompatActivity implements BookingsHost {

    private static final String TAB_PREFIX = "tab_";

    protected BottomNavigationView bottomNav;
    protected User user;

    private Subscription subscription;
    private List<Booking> latestBookings;
    private final List<BookingsListener> listeners = new ArrayList<>();
    private final Map<String, String> knownStatuses = new HashMap<>();
    private boolean firstSnapshot = true;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    protected abstract int getMenuRes();

    protected abstract Fragment createFragment(int menuItemId);

    /** Called for every booking whose status changed (oldStatus == null for new bookings). */
    protected abstract void onBookingChanged(Booking booking, String oldStatus);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        user = RepositoryProvider.get().getCachedUser();
        // If Android recreated this screen after killing the process, reload via the splash screen.
        super.onCreate(user == null ? null : savedInstanceState);
        if (user == null) {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_home);
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.inflateMenu(getMenuRes());
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) showTab(R.id.nav_home);

        subscribeToBookings();
        askNotificationPermission();
    }

    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        String tag = TAB_PREFIX + itemId;
        Fragment target = fm.findFragmentByTag(tag);
        FragmentTransaction tx = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f.getTag() != null && f.getTag().startsWith(TAB_PREFIX) && f != target) tx.hide(f);
        }
        if (target == null) {
            tx.add(R.id.fragmentContainer, createFragment(itemId), tag);
        } else {
            tx.show(target);
        }
        tx.setReorderingAllowed(true);
        tx.commitNow();
    }

    protected Fragment findTab(int itemId) {
        return getSupportFragmentManager().findFragmentByTag(TAB_PREFIX + itemId);
    }

    @Override
    public void selectTab(int menuItemId) {
        bottomNav.setSelectedItemId(menuItemId);
    }

    // ------------------------------------------------------------ bookings

    private void subscribeToBookings() {
        subscription = RepositoryProvider.get().listenToMyBookings(new Callback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                handleBookings(bookings);
            }

            @Override
            public void onError(String message) {
                for (BookingsListener l : new ArrayList<>(listeners)) l.onBookingsError(message);
            }
        });
    }

    private void handleBookings(List<Booking> bookings) {
        if (!firstSnapshot) {
            for (Booking b : bookings) {
                String old = knownStatuses.get(b.getBookingId());
                if (old == null || !old.equals(b.getStatus())) onBookingChanged(b, old);
            }
        }
        knownStatuses.clear();
        for (Booking b : bookings) knownStatuses.put(b.getBookingId(), b.getStatus());
        firstSnapshot = false;
        remindUpcoming(bookings);

        List<Booking> sorted = new ArrayList<>(bookings);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getScheduledAt(), b.getScheduledAt()));
        latestBookings = sorted;
        for (BookingsListener l : new ArrayList<>(listeners)) l.onBookingsChanged(sorted);
    }

    /** "Your service appointment is tomorrow" – shown once per booking. */
    private void remindUpcoming(List<Booking> bookings) {
        for (Booking b : bookings) {
            if (Booking.STATUS_ACCEPTED.equals(b.getStatus()) && DateTimeUtils.isTomorrow(b.getScheduledAt())
                    && PrefsManager.get(this).markReminded(b.getBookingId())) {
                String other = user.isProvider() ? b.getCustomerName() : b.getProviderName();
                NotificationHelper.show(this, "Your service appointment is tomorrow",
                        b.getService() + " with " + other + " at " + DateTimeUtils.to12Hour(b.getTime()),
                        b.getBookingId());
            }
        }
    }

    @Override
    public List<Booking> getLatestBookings() {
        return latestBookings;
    }

    @Override
    public void addBookingsListener(BookingsListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void removeBookingsListener(BookingsListener listener) {
        listeners.remove(listener);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onDestroy() {
        if (subscription != null) subscription.remove();
        super.onDestroy();
    }
}
