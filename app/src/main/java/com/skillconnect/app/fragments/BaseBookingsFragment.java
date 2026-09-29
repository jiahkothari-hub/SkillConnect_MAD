package com.skillconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.activities.BookingDetailsActivity;
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.ReviewActivity;
import com.skillconnect.app.adapters.BookingAdapter;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.utils.BookingActions;
import com.skillconnect.app.utils.FileUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared screen for booking lists (customer bookings, provider requests, provider jobs).
 * Data comes from the host activity's single real-time listener (see BookingsHost).
 */
public abstract class BaseBookingsFragment extends Fragment implements BookingsHost.BookingsListener {

    protected BookingsHost host;
    private BookingAdapter adapter;
    private StateView state;
    private RecyclerView recyclerView;
    private TabLayout tabs;
    private int selectedTab;

    protected abstract String getTitle();

    protected abstract String getSubtitle();

    /** Tab titles, or null for no tabs. */
    protected abstract String[] getTabs();

    protected abstract boolean matchesTab(Booking booking, int tab);

    protected abstract int getAdapterMode();

    protected abstract void showEmptyState(StateView state, int tab);

    /** File name for the CSV export, or null to hide the export button. */
    protected abstract String getExportFileName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        host = (BookingsHost) requireActivity();
        ((TextView) view.findViewById(R.id.tvTitle)).setText(getTitle());
        ((TextView) view.findViewById(R.id.tvSubtitle)).setText(getSubtitle());
        state = new StateView(view.findViewById(R.id.bookingsState));
        recyclerView = view.findViewById(R.id.rvBookings);
        tabs = view.findViewById(R.id.tabs);

        adapter = new BookingAdapter(getAdapterMode(), new BookingAdapter.Listener() {
            @Override
            public void onOpen(Booking booking) {
                Intent i = new Intent(requireContext(), BookingDetailsActivity.class);
                i.putExtra(BookingDetailsActivity.EXTRA_BOOKING_ID, booking.getBookingId());
                startActivity(i);
            }

            @Override
            public void onPrimaryAction(Booking booking) {
                if (getAdapterMode() == BookingAdapter.MODE_CUSTOMER) {
                    Intent i = new Intent(requireContext(), ReviewActivity.class);
                    i.putExtra(ReviewActivity.EXTRA_BOOKING_ID, booking.getBookingId());
                    startActivity(i);
                } else if (Booking.STATUS_PENDING.equals(booking.getStatus())) {
                    BookingActions.changeStatus(requireContext(), booking, Booking.STATUS_ACCEPTED, null);
                } else if (Booking.STATUS_ACCEPTED.equals(booking.getStatus())) {
                    BookingActions.changeStatus(requireContext(), booking, Booking.STATUS_COMPLETED, null);
                }
            }

            @Override
            public void onSecondaryAction(Booking booking) {
                BookingActions.changeStatus(requireContext(), booking, Booking.STATUS_REJECTED, null);
            }
        });
        recyclerView.setAdapter(adapter);

        String[] tabTitles = getTabs();
        if (tabTitles == null) {
            tabs.setVisibility(View.GONE);
        } else {
            for (String t : tabTitles) tabs.addTab(tabs.newTab().setText(t));
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    selectedTab = tab.getPosition();
                    render();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        View btnExport = view.findViewById(R.id.btnExport);
        btnExport.setVisibility(getExportFileName() != null ? View.VISIBLE : View.GONE);
        btnExport.setOnClickListener(v -> exportCsv());

        host.addBookingsListener(this);
        render();
    }

    @Override
    public void onDestroyView() {
        if (host != null) host.removeBookingsListener(this);
        super.onDestroyView();
    }

    @Override
    public void onBookingsChanged(List<Booking> bookings) {
        render();
    }

    @Override
    public void onBookingsError(String message) {
        if (getView() == null) return;
        if (host.getLatestBookings() == null) {
            recyclerView.setVisibility(View.GONE);
            state.showError(message, null);
        } else {
            UiUtils.toast(requireContext(), message);
        }
    }

    private void render() {
        if (getView() == null) return;
        List<Booking> all = host.getLatestBookings();
        if (all == null) {
            recyclerView.setVisibility(View.GONE);
            state.showLoading(getString(R.string.loading));
            return;
        }
        List<Booking> filtered = new ArrayList<>();
        for (Booking b : all) if (matchesTab(b, selectedTab)) filtered.add(b);
        // Upcoming lists: soonest first. History lists: most recent first.
        boolean upcoming = selectedTab == 0;
        Collections.sort(filtered, (a, b) -> upcoming
                ? Long.compare(a.getScheduledAt(), b.getScheduledAt())
                : Long.compare(b.getScheduledAt(), a.getScheduledAt()));
        adapter.submit(filtered);
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            showEmptyState(state, selectedTab);
        } else {
            state.hide();
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /** FILE HANDLING: exports all bookings to a CSV file and offers to share it. */
    private void exportCsv() {
        List<Booking> all = host.getLatestBookings();
        if (all == null || all.isEmpty()) {
            UiUtils.toast(requireContext(), "No bookings to export yet");
            return;
        }
        FileUtils.exportAndPreview(requireActivity(), getExportFileName(), FileUtils.MIME_CSV, FileUtils.buildBookingsCsv(all));
    }
}
