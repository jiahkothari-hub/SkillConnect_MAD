package com.skillconnect.app.fragments;

import com.skillconnect.app.R;
import com.skillconnect.app.adapters.BookingAdapter;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.utils.StateView;

/** Provider "Jobs": Accepted (mark completed) / Completed / Closed. */
public class ProviderJobsFragment extends BaseBookingsFragment {

    @Override
    protected String getTitle() {
        return getString(R.string.nav_jobs);
    }

    @Override
    protected String getSubtitle() {
        return "Your confirmed and past work";
    }

    @Override
    protected String[] getTabs() {
        return new String[]{"Accepted", getString(R.string.completed), "Closed"};
    }

    @Override
    protected boolean matchesTab(Booking b, int tab) {
        switch (tab) {
            case 1:
                return Booking.STATUS_COMPLETED.equals(b.getStatus());
            case 2:
                return b.isClosed();
            default:
                return Booking.STATUS_ACCEPTED.equals(b.getStatus());
        }
    }

    @Override
    protected int getAdapterMode() {
        return BookingAdapter.MODE_PROVIDER;
    }

    @Override
    protected void showEmptyState(StateView state, int tab) {
        state.showEmpty(R.drawable.ic_work, getString(R.string.no_jobs_title), getString(R.string.no_jobs_msg));
    }

    @Override
    protected String getExportFileName() {
        return "job_history.csv";
    }
}
