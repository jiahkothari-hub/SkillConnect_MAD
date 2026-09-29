package com.skillconnect.app.activities;

import androidx.fragment.app.Fragment;

import com.skillconnect.app.R;
import com.skillconnect.app.fragments.CustomerBookingsFragment;
import com.skillconnect.app.fragments.CustomerHomeFragment;
import com.skillconnect.app.fragments.ProfileFragment;
import com.skillconnect.app.fragments.SavedFragment;
import com.skillconnect.app.fragments.SearchFragment;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.notifications.NotificationHelper;
import com.skillconnect.app.utils.DateTimeUtils;

/** Customer home with bottom navigation: Home, Search, Bookings, Saved, Profile. */
public class CustomerHomeActivity extends BaseHomeActivity {

    @Override
    protected int getMenuRes() {
        return R.menu.menu_customer_nav;
    }

    @Override
    protected Fragment createFragment(int menuItemId) {
        if (menuItemId == R.id.nav_search) return new SearchFragment();
        if (menuItemId == R.id.nav_bookings) return new CustomerBookingsFragment();
        if (menuItemId == R.id.nav_saved) return new SavedFragment();
        if (menuItemId == R.id.nav_profile) return new ProfileFragment();
        return new CustomerHomeFragment();
    }

    /** Opens the Search tab, optionally with a query already typed. */
    public void openSearch(String query) {
        selectTab(R.id.nav_search);
        Fragment f = findTab(R.id.nav_search);
        if (f instanceof SearchFragment) ((SearchFragment) f).setQuery(query);
    }

    @Override
    protected void onBookingChanged(Booking b, String oldStatus) {
        if (oldStatus == null) return;
        String when = b.getDate() + " at " + DateTimeUtils.to12Hour(b.getTime());
        switch (b.getStatus()) {
            case Booking.STATUS_ACCEPTED:
                NotificationHelper.show(this, "Your booking request was accepted",
                        b.getProviderName() + " accepted your " + b.getService() + " request for " + when + ".", b.getBookingId());
                break;
            case Booking.STATUS_REJECTED:
                NotificationHelper.show(this, "Booking request declined",
                        b.getProviderName() + " can't take your " + b.getService() + " request. Try another provider.", b.getBookingId());
                break;
            case Booking.STATUS_COMPLETED:
                NotificationHelper.show(this, "How was your experience?",
                        "Rate " + b.getProviderName() + " for " + b.getService() + ".", b.getBookingId());
                break;
            default:
                break;
        }
    }
}
