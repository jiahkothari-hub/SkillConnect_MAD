package com.skillconnect.app.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.AvatarDrawable;
import com.skillconnect.app.utils.BookingActions;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

/** One booking with its status timeline and the actions allowed for the viewer's role. */
public class BookingDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private String bookingId;
    private Booking booking;
    private StateView state;
    private View content;
    private MaterialButton btnPrimary, btnSecondary, btnTertiary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);
        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        state = new StateView(findViewById(R.id.detailsState));
        content = findViewById(R.id.scrollContent);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnSecondary = findViewById(R.id.btnSecondary);
        btnTertiary = findViewById(R.id.btnTertiary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        User me = RepositoryProvider.get().getCachedUser();
        if (me == null || bookingId == null) {
            // Opened from a notification after the app was closed – start normally.
            Intent i = new Intent(this, SplashActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        if (booking == null) state.showLoading(getString(R.string.loading));
        RepositoryProvider.get().getBooking(bookingId, new Callback<Booking>() {
            @Override
            public void onSuccess(Booking result) {
                booking = result;
                state.hide();
                content.setVisibility(View.VISIBLE);
                bind();
            }

            @Override
            public void onError(String message) {
                content.setVisibility(View.GONE);
                state.showError(message, BookingDetailsActivity.this::load);
            }
        });
    }

    private void bind() {
        User me = RepositoryProvider.get().getCachedUser();
        boolean iAmProvider = me != null && me.getUserId().equals(booking.getProviderId());
        boolean iAmCustomer = me != null && me.getUserId().equals(booking.getCustomerId());
        String status = booking.getStatus();
        String providerFirst = firstName(booking.getProviderName());
        String customerFirst = firstName(booking.getCustomerName());
        String when = DateTimeUtils.displayDate(booking.getScheduledAt()) + " at " + DateTimeUtils.to12Hour(booking.getTime());

        UiUtils.styleStatusPill(findViewById(R.id.tvStatus), status);
        String message;
        switch (status) {
            case Booking.STATUS_PENDING:
                message = iAmProvider ? customerFirst + " is waiting for your response."
                        : "Waiting for " + providerFirst + " to respond to your request.";
                break;
            case Booking.STATUS_ACCEPTED:
                message = iAmProvider ? "You accepted this job. See you on " + when + "."
                        : providerFirst + " accepted! See you on " + when + ".";
                break;
            case Booking.STATUS_COMPLETED:
                message = booking.isReviewed() ? "Service completed and reviewed. Thank you!" : "This service was completed.";
                break;
            case Booking.STATUS_REJECTED:
                message = iAmProvider ? "You declined this request."
                        : providerFirst + " couldn't take this request. Try another provider.";
                break;
            default:
                message = "This booking was cancelled.";
                break;
        }
        ((TextView) findViewById(R.id.tvStatusMessage)).setText(message);
        bindSteps(status);

        // The other person
        String otherName = iAmProvider ? booking.getCustomerName() : booking.getProviderName();
        String otherPhone = iAmProvider ? booking.getCustomerPhone() : booking.getProviderPhone();
        ((ImageView) findViewById(R.id.ivPerson)).setImageDrawable(new AvatarDrawable(otherName));
        ((TextView) findViewById(R.id.tvPersonLabel)).setText(iAmProvider ? "Customer" : "Provider");
        ((TextView) findViewById(R.id.tvPersonName)).setText(otherName);
        View btnCall = findViewById(R.id.btnCall);
        btnCall.setVisibility(otherPhone != null && !otherPhone.isEmpty() && !booking.isClosed() ? View.VISIBLE : View.GONE);
        btnCall.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + otherPhone)));
            } catch (ActivityNotFoundException e) {
                UiUtils.toast(this, "No phone app found");
            }
        });

        row(R.id.rowService, R.drawable.ic_work, "Service", booking.getService());
        row(R.id.rowDate, R.drawable.ic_calendar, "Date", DateTimeUtils.displayDate(booking.getScheduledAt()));
        row(R.id.rowTime, R.drawable.ic_time, "Time", DateTimeUtils.to12Hour(booking.getTime()));
        row(R.id.rowPrice, R.drawable.ic_rupee, "Price", "From " + UiUtils.formatPrice(booking.getPrice()));
        row(R.id.rowLocation, R.drawable.ic_location, "Location", booking.getLocation());
        row(R.id.rowDescription, R.drawable.ic_chat, "Details", booking.getDescription());
        ((TextView) findViewById(R.id.tvBookingId)).setText("Booking ID: " + booking.getBookingId()
                + " · Requested " + DateTimeUtils.relativeTime(booking.getCreatedAt()));

        btnPrimary.setVisibility(View.GONE);
        btnSecondary.setVisibility(View.GONE);
        btnTertiary.setVisibility(View.GONE);

        if (iAmCustomer) {
            if (booking.isOpen()) {
                showButton(btnSecondary, "Cancel Booking", v ->
                        BookingActions.changeStatus(this, booking, Booking.STATUS_CANCELLED, this::load));
            }
            if (booking.canBeReviewed()) {
                showButton(btnPrimary, getString(R.string.leave_review), v -> {
                    Intent i = new Intent(this, ReviewActivity.class);
                    i.putExtra(ReviewActivity.EXTRA_BOOKING_ID, booking.getBookingId());
                    startActivity(i);
                });
            }
            showButton(btnTertiary, "View " + providerFirst + "'s Profile", v -> {
                Intent i = new Intent(this, ProviderDetailsActivity.class);
                i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, booking.getProviderId());
                startActivity(i);
            });
        } else if (iAmProvider) {
            if (Booking.STATUS_PENDING.equals(status)) {
                showButton(btnPrimary, getString(R.string.accept), v ->
                        BookingActions.changeStatus(this, booking, Booking.STATUS_ACCEPTED, this::load));
                showButton(btnSecondary, getString(R.string.decline), v ->
                        BookingActions.changeStatus(this, booking, Booking.STATUS_REJECTED, this::load));
            } else if (Booking.STATUS_ACCEPTED.equals(status)) {
                showButton(btnPrimary, getString(R.string.mark_completed), v ->
                        BookingActions.changeStatus(this, booking, Booking.STATUS_COMPLETED, this::load));
                showButton(btnTertiary, "Get Directions", v ->
                        LocationUtils.openAddressInMaps(this, booking.getLocation()));
            }
        }
    }

    private void bindSteps(String status) {
        boolean closed = Booking.STATUS_CANCELLED.equals(status) || Booking.STATUS_REJECTED.equals(status);
        findViewById(R.id.layoutSteps).setVisibility(closed ? View.GONE : View.VISIBLE);
        findViewById(R.id.layoutStepLabels).setVisibility(closed ? View.GONE : View.VISIBLE);
        int progress = Booking.STATUS_COMPLETED.equals(status) ? 3 : Booking.STATUS_ACCEPTED.equals(status) ? 2 : 1;
        int[] steps = {R.id.step1, R.id.step2, R.id.step3};
        int[] lines = {R.id.line1, R.id.line2};
        for (int i = 0; i < steps.length; i++) {
            findViewById(steps[i]).setBackgroundResource(i < progress ? R.drawable.bg_step_done : R.drawable.bg_step_todo);
        }
        for (int i = 0; i < lines.length; i++) {
            findViewById(lines[i]).setBackgroundColor(ContextCompat.getColor(this,
                    i < progress - 1 ? R.color.primary : R.color.stroke));
        }
    }

    private void row(int rowId, int icon, String label, String value) {
        View row = findViewById(rowId);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value != null && !value.isEmpty() ? value : "-");
    }

    private void showButton(MaterialButton button, String text, View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setOnClickListener(listener);
    }

    private static String firstName(String name) {
        return name == null || name.isEmpty() ? "" : name.split(" ")[0];
    }
}
