package com.skillconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.skillconnect.app.R;
import com.skillconnect.app.activities.BookingDetailsActivity;
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.EditProviderProfileActivity;
import com.skillconnect.app.adapters.BookingAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.BookingActions;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Provider home: welcome, availability switch, statistics and the newest requests. */
public class ProviderDashboardFragment extends Fragment implements BookingsHost.BookingsListener {

    private static final int REQUEST_PREVIEW_LIMIT = 3;

    private AppRepository repo;
    private BookingsHost host;
    private Provider provider;

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvWelcome, tvProfession, tvAvailabilityHint;
    private TextView tvStatPending, tvStatConfirmed, tvStatCompleted, tvStatRating;
    private MaterialSwitch switchAvailable;
    private View availabilityDot;
    private View cardCompleteProfile;
    private RecyclerView rvRequests;
    private StateView requestsState;
    private BookingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = RepositoryProvider.get();
        host = (BookingsHost) requireActivity();

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvProfession = view.findViewById(R.id.tvProfession);
        tvAvailabilityHint = view.findViewById(R.id.tvAvailabilityHint);
        tvStatPending = view.findViewById(R.id.tvStatPending);
        tvStatConfirmed = view.findViewById(R.id.tvStatConfirmed);
        tvStatCompleted = view.findViewById(R.id.tvStatCompleted);
        tvStatRating = view.findViewById(R.id.tvStatRating);
        switchAvailable = view.findViewById(R.id.switchAvailable);
        availabilityDot = view.findViewById(R.id.viewAvailabilityDot);
        cardCompleteProfile = view.findViewById(R.id.cardCompleteProfile);
        rvRequests = view.findViewById(R.id.rvRequests);
        requestsState = new StateView(view.findViewById(R.id.requestsState));

        User user = repo.getCachedUser();
        tvWelcome.setText("Welcome back, " + (user != null ? user.getFirstName() : "") + " 👋");
        ImageUtils.loadAvatar((ImageView) view.findViewById(R.id.ivAvatar),
                user != null ? user.getProfileImage() : null, user != null ? user.getName() : "?");
        view.findViewById(R.id.ivAvatar).setOnClickListener(v -> host.selectTab(R.id.nav_profile));

        adapter = new BookingAdapter(BookingAdapter.MODE_PROVIDER, new BookingAdapter.Listener() {
            @Override
            public void onOpen(Booking booking) {
                Intent i = new Intent(requireContext(), BookingDetailsActivity.class);
                i.putExtra(BookingDetailsActivity.EXTRA_BOOKING_ID, booking.getBookingId());
                startActivity(i);
            }

            @Override
            public void onPrimaryAction(Booking booking) {
                BookingActions.changeStatus(requireContext(), booking, Booking.STATUS_ACCEPTED, null);
            }

            @Override
            public void onSecondaryAction(Booking booking) {
                BookingActions.changeStatus(requireContext(), booking, Booking.STATUS_REJECTED, null);
            }
        });
        rvRequests.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadProvider);
        view.findViewById(R.id.btnViewAllRequests).setOnClickListener(v -> host.selectTab(R.id.nav_requests));
        view.findViewById(R.id.btnCompleteProfile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProviderProfileActivity.class)));

        host.addBookingsListener(this);
        renderBookings();
        loadProvider();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (provider != null) loadProvider();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) loadProvider();
    }

    @Override
    public void onDestroyView() {
        host.removeBookingsListener(this);
        super.onDestroyView();
    }

    private void loadProvider() {
        repo.getProvider(repo.getCurrentUserId(), new Callback<Provider>() {
            @Override
            public void onSuccess(Provider result) {
                if (getView() == null) return;
                swipeRefresh.setRefreshing(false);
                provider = result;
                bindProvider();
            }

            @Override
            public void onError(String message) {
                if (getView() == null) return;
                swipeRefresh.setRefreshing(false);
                UiUtils.toast(requireContext(), message);
            }
        });
    }

    private void bindProvider() {
        tvProfession.setText(provider.getTitle() != null && !provider.getTitle().isEmpty()
                ? provider.getTitle() + (provider.isVerified() ? " · Verified" : " · Verification pending")
                : "Set up your profile to get discovered");
        tvStatRating.setText(provider.getReviewCount() > 0 ? UiUtils.formatRating(provider.getAverageRating()) : "New");
        cardCompleteProfile.setVisibility(provider.isProfileComplete() ? View.GONE : View.VISIBLE);

        switchAvailable.setOnCheckedChangeListener(null);
        switchAvailable.setChecked(provider.isAvailable());
        updateAvailabilityHint(provider.isAvailable());
        switchAvailable.setOnCheckedChangeListener((button, checked) -> {
            provider.setAvailable(checked);
            updateAvailabilityHint(checked);
            repo.saveProviderProfile(provider, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (isAdded()) UiUtils.toast(requireContext(), checked ? "You're now available for new jobs" : "You're marked as busy");
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) UiUtils.toast(requireContext(), message);
                }
            });
        });
    }

    private void updateAvailabilityHint(boolean available) {
        tvAvailabilityHint.setText(available ? "Customers see you as available" : "Customers see you as busy");
        availabilityDot.setBackgroundResource(available ? R.drawable.bg_dot_available : R.drawable.bg_dot_unavailable);
    }

    @Override
    public void onBookingsChanged(List<Booking> bookings) {
        renderBookings();
    }

    @Override
    public void onBookingsError(String message) {
        if (getView() != null && host.getLatestBookings() == null) requestsState.showError(message, null);
    }

    private void renderBookings() {
        if (getView() == null) return;
        List<Booking> all = host.getLatestBookings();
        if (all == null) {
            requestsState.showLoading(getString(R.string.loading));
            rvRequests.setVisibility(View.GONE);
            return;
        }
        int pending = 0, confirmed = 0, completed = 0;
        List<Booking> newest = new ArrayList<>();
        for (Booking b : all) {
            switch (b.getStatus()) {
                case Booking.STATUS_PENDING:
                    pending++;
                    if (newest.size() < REQUEST_PREVIEW_LIMIT) newest.add(b);
                    break;
                case Booking.STATUS_ACCEPTED:
                    confirmed++;
                    break;
                case Booking.STATUS_COMPLETED:
                    completed++;
                    break;
                default:
                    break;
            }
        }
        tvStatPending.setText(String.valueOf(pending));
        tvStatConfirmed.setText(String.valueOf(confirmed));
        tvStatCompleted.setText(String.valueOf(completed));

        adapter.submit(newest);
        if (newest.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            requestsState.showEmpty(R.drawable.ic_inbox, getString(R.string.no_requests_title), getString(R.string.no_requests_msg));
        } else {
            requestsState.hide();
            rvRequests.setVisibility(View.VISIBLE);
        }
    }
}
