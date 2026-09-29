package com.skillconnect.app.fragments;

import com.skillconnect.app.R;
import com.skillconnect.app.adapters.BookingAdapter;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.utils.StateView;

/** Customer "My Bookings": Upcoming / Completed / Cancelled. */
public class CustomerBookingsFragment extends BaseBookingsFragment {

    @Override
    protected String getTitle() {
        return getString(R.string.my_bookings);
    }

    @Override
    protected String getSubtitle() {
        return "Track your service requests in real time";
    }

    @Override
    protected String[] getTabs() {
        return new String[]{getString(R.string.upcoming), getString(R.string.completed), getString(R.string.cancelled)};
    }

    @Override
    protected boolean matchesTab(Booking b, int tab) {
        switch (tab) {
            case 1:
                return Booking.STATUS_COMPLETED.equals(b.getStatus());
            case 2:
                return b.isClosed();
            default:
                return b.isOpen();
        }
    }

    @Override
    protected int getAdapterMode() {
        return BookingAdapter.MODE_CUSTOMER;
    }

    @Override
    protected void showEmptyState(StateView state, int tab) {
        if (tab == 1) {
            state.showEmpty(R.drawable.ic_check_circle, "No completed bookings yet.",
                    "Completed services will appear here so you can review them.");
        } else if (tab == 2) {
            state.showEmpty(R.drawable.ic_block, "No cancelled bookings.", "Cancelled or declined requests appear here.");
        } else {
            state.showEmpty(R.drawable.ic_calendar, getString(R.string.no_bookings_title), getString(R.string.no_bookings_msg),
                    getString(R.string.find_provider), () -> host.selectTab(R.id.nav_search));
        }
    }

    @Override
    protected String getExportFileName() {
        return "booking_history.csv";
    }
}
