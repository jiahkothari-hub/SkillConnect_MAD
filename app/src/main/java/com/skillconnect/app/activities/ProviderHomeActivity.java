package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;

import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.fragments.ChatListFragment;
import com.skillconnect.app.fragments.ProfileFragment;
import com.skillconnect.app.fragments.ProviderDashboardFragment;
import com.skillconnect.app.fragments.ProviderJobsFragment;
import com.skillconnect.app.fragments.ProviderRequestsFragment;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.notifications.NotificationHelper;
import com.skillconnect.app.utils.DateTimeUtils;

/** Provider home with bottom navigation: Home, Requests, Jobs, Profile. */
public class ProviderHomeActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isFinishing()) checkServiceProfile();
    }

    /**
     * Customers can only find providers whose service profile (skill, price, location, description)
     * is complete, so a provider who hasn't finished it is taken to the setup screen first.
     */
    private void checkServiceProfile() {
        AppRepository repo = RepositoryProvider.get();
        String uid = repo.getCurrentUserId();
        if (uid == null) return;
        repo.getProvider(uid, new Callback<Provider>() {
            @Override
            public void onSuccess(Provider provider) {
                if (!provider.isProfileComplete()) openSetup();
            }

            @Override
            public void onError(String message) {
                if (AppRepository.PROVIDER_NOT_FOUND.equals(message)) openSetup();
            }
        });
    }

    private void openSetup() {
        if (isFinishing() || isDestroyed()) return;
        Intent intent = new Intent(this, EditProviderProfileActivity.class);
        intent.putExtra(EditProviderProfileActivity.EXTRA_FIRST_SETUP, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected int getMenuRes() {
        return R.menu.menu_provider_nav;
    }

    @Override
    protected Fragment createFragment(int menuItemId) {
        if (menuItemId == R.id.nav_requests) return new ProviderRequestsFragment();
        if (menuItemId == R.id.nav_jobs) return new ProviderJobsFragment();
        if (menuItemId == R.id.nav_messages) return ChatListFragment.newInstance(true);
        if (menuItemId == R.id.nav_profile) return new ProfileFragment();
        return new ProviderDashboardFragment();
    }

    @Override
    protected void onUnreadChatsChanged(int count) {
        if (count > 0) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_messages);
            badge.setNumber(count);
            badge.setVisible(true);
        } else {
            bottomNav.removeBadge(R.id.nav_messages);
        }
    }

    @Override
    protected void onBookingChanged(Booking b, String oldStatus) {
        if (oldStatus == null && Booking.STATUS_PENDING.equals(b.getStatus())) {
            NotificationHelper.show(this, "You have a new service request",
                    b.getCustomerName() + " requested " + b.getService() + " on " + b.getDate()
                            + " at " + DateTimeUtils.to12Hour(b.getTime()) + ".", b.getBookingId());
        } else if (oldStatus != null && Booking.STATUS_CANCELLED.equals(b.getStatus())) {
            NotificationHelper.show(this, "Booking cancelled",
                    b.getCustomerName() + " cancelled " + b.getService() + " on " + b.getDate() + ".", b.getBookingId());
        }
    }
}
