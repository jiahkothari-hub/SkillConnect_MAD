package com.skillconnect.app.fragments;

import com.skillconnect.app.R;
import com.skillconnect.app.adapters.BookingAdapter;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.utils.StateView;

/** Provider "Requests": pending requests with Accept / Decline. */
public class ProviderRequestsFragment extends BaseBookingsFragment {

    @Override
    protected String getTitle() {
        return getString(R.string.nav_requests);
    }

    @Override
    protected String getSubtitle() {
        return "Respond quickly to win more jobs";
    }

    @Override
    protected String[] getTabs() {
        return null;
    }

    @Override
    protected boolean matchesTab(Booking b, int tab) {
        return Booking.STATUS_PENDING.equals(b.getStatus());
    }

    @Override
    protected int getAdapterMode() {
        return BookingAdapter.MODE_PROVIDER;
    }

    @Override
    protected void showEmptyState(StateView state, int tab) {
        state.showEmpty(R.drawable.ic_inbox, getString(R.string.no_requests_title), getString(R.string.no_requests_msg));
    }

    @Override
    protected String getExportFileName() {
        return null;
    }
}
