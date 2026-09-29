package com.skillconnect.app.activities;

import androidx.fragment.app.Fragment;

import com.skillconnect.app.R;
import com.skillconnect.app.fragments.ProfileFragment;
import com.skillconnect.app.fragments.ProviderDashboardFragment;
import com.skillconnect.app.fragments.ProviderJobsFragment;
import com.skillconnect.app.fragments.ProviderRequestsFragment;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.notifications.NotificationHelper;
import com.skillconnect.app.utils.DateTimeUtils;

/** Provider home with bottom navigation: Home, Requests, Jobs, Profile. */
public class ProviderHomeActivity extends BaseHomeActivity {

    @Override
    protected int getMenuRes() {
        return R.menu.menu_provider_nav;
    }

    @Override
    protected Fragment createFragment(int menuItemId) {
        if (menuItemId == R.id.nav_requests) return new ProviderRequestsFragment();
        if (menuItemId == R.id.nav_jobs) return new ProviderJobsFragment();
        if (menuItemId == R.id.nav_profile) return new ProfileFragment();
        return new ProviderDashboardFragment();
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
