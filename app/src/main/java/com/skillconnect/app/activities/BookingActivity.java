package com.skillconnect.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Book a service: choose the service, a date (DatePickerDialog), a time (TimePickerDialog),
 * the address and describe the job. A confirmation dialog is shown before sending.
 */
public class BookingActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "provider_id";
    private static final int MAX_DAYS_AHEAD = 90;

    private AppRepository repo;
    private Provider provider;
    private StateView state;

    private TextInputLayout tilService, tilDate, tilTime, tilAddress, tilDescription;
    private MaterialAutoCompleteTextView actService;
    private TextInputEditText etDate, etTime, etAddress, etDescription;
    private MaterialButton btnSend;
    private View progress;

    private Calendar selectedDay;        // null until a date is picked
    private int selectedHour = -1;
    private int selectedMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        repo = RepositoryProvider.get();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        state = new StateView(findViewById(R.id.bookingState));
        tilService = findViewById(R.id.tilService);
        tilDate = findViewById(R.id.tilDate);
        tilTime = findViewById(R.id.tilTime);
        tilAddress = findViewById(R.id.tilAddress);
        tilDescription = findViewById(R.id.tilDescription);
        actService = findViewById(R.id.actService);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);
        btnSend = findViewById(R.id.btnSend);
        progress = findViewById(R.id.progress);

        etDate.setOnClickListener(v -> showDatePicker());
        tilDate.setStartIconOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        tilTime.setStartIconOnClickListener(v -> showTimePicker());
        btnSend.setOnClickListener(v -> validateAndConfirm());

        loadProvider();
    }

    private void loadProvider() {
        state.showLoading(getString(R.string.loading));
        repo.getProvider(getIntent().getStringExtra(EXTRA_PROVIDER_ID), new Callback<Provider>() {
            @Override
            public void onSuccess(Provider result) {
                provider = result;
                state.hide();
                findViewById(R.id.scrollContent).setVisibility(View.VISIBLE);
                bindProvider();
            }

            @Override
            public void onError(String message) {
                state.showError(message, BookingActivity.this::loadProvider);
            }
        });
    }

    private void bindProvider() {
        ImageUtils.loadAvatar(findViewById(R.id.ivAvatar), provider.getProfileImage(), provider.getName());
        ((TextView) findViewById(R.id.tvProviderName)).setText(provider.getName());
        ((TextView) findViewById(R.id.tvProviderTitle)).setText(provider.getTitle());
        ((TextView) findViewById(R.id.tvProviderLocation)).setText(provider.getAddress());
        ((TextView) findViewById(R.id.tvProviderPrice)).setText(UiUtils.formatPrice(provider.getStartingPrice()));
        ((TextView) findViewById(R.id.tvPriceSummary)).setText("Starting at " + UiUtils.formatPrice(provider.getStartingPrice())
                + " " + provider.getPriceUnit());
        ((TextView) findViewById(R.id.tvWorkingHours)).setText(provider.getName().split(" ")[0] + " usually works "
                + DateTimeUtils.summarizeDays(provider.getAvailableDays()) + ", "
                + DateTimeUtils.hoursRange(provider.getAvailableFrom(), provider.getAvailableTo()));

        // Service dropdown: primary skill + listed skills (free text is also allowed)
        List<String> services = new ArrayList<>();
        services.add(provider.getTitle());
        for (String s : provider.getSkills()) if (!services.contains(s)) services.add(s);
        actService.setSimpleItems(services.toArray(new String[0]));
        actService.setText(provider.getTitle(), false);

        User me = repo.getCachedUser();
        String address = me != null && me.getAddress() != null ? me.getAddress() : PrefsManager.get(this).getLocality();
        if (address != null) etAddress.setText(address);
    }

    // ---------------------------------------------------------------- pickers

    private void showDatePicker() {
        Calendar initial = selectedDay != null ? selectedDay : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            selectedDay = c;
            etDate.setText(DateTimeUtils.displayDate(c.getTimeInMillis()));
            tilDate.setError(null);
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        Calendar max = Calendar.getInstance();
        max.add(Calendar.DAY_OF_YEAR, MAX_DAYS_AHEAD);
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.getDatePicker().setMaxDate(max.getTimeInMillis());
        dialog.show();
    }

    private void showTimePicker() {
        int[] start = DateTimeUtils.parseTime(provider != null ? provider.getAvailableFrom() : "09:00");
        int hour = selectedHour >= 0 ? selectedHour : (start != null ? start[0] : 9);
        int minute = selectedMinute >= 0 ? selectedMinute : 0;
        new TimePickerDialog(this, (view, h, m) -> {
            selectedHour = h;
            selectedMinute = m;
            etTime.setText(DateTimeUtils.to12Hour(DateTimeUtils.formatTime24(h, m)));
            tilTime.setError(null);
        }, hour, minute, false).show();
    }

    // ---------------------------------------------------------------- validation

    private void validateAndConfirm() {
        for (TextInputLayout t : new TextInputLayout[]{tilService, tilDate, tilTime, tilAddress, tilDescription}) {
            t.setError(null);
        }
        String service = actService.getText() != null ? actService.getText().toString().trim() : "";
        String address = text(etAddress);
        String description = text(etDescription);
        boolean valid = true;

        if (service.length() < 2) {
            tilService.setError("Choose or type a service");
            valid = false;
        }
        if (selectedDay == null) {
            tilDate.setError("Pick a date");
            valid = false;
        }
        if (selectedHour < 0) {
            tilTime.setError("Pick a time");
            valid = false;
        }
        long scheduledAt = 0;
        if (selectedDay != null && selectedHour >= 0) {
            scheduledAt = DateTimeUtils.combine(selectedDay.getTimeInMillis(),
                    DateTimeUtils.formatTime24(selectedHour, selectedMinute));
            if (scheduledAt < System.currentTimeMillis() + 30 * 60 * 1000L) {
                tilTime.setError("Choose a time at least 30 min from now");
                valid = false;
            }
        }
        if (address.length() < 3) {
            tilAddress.setError("Where should the service happen?");
            valid = false;
        }
        if (description.length() < 10) {
            tilDescription.setError("Please describe what you need (at least 10 characters)");
            valid = false;
        }
        if (!valid) return;

        String dayName = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date(scheduledAt));
        String warning = provider.getAvailableDays().isEmpty() || provider.getAvailableDays().contains(dayName) ? ""
                : "\n\nNote: " + provider.getName().split(" ")[0] + " doesn't usually work on " + dayName
                + ". They may suggest another day.";
        String message = service + "\n" + DateTimeUtils.displayDate(scheduledAt) + " at "
                + DateTimeUtils.to12Hour(DateTimeUtils.formatTime24(selectedHour, selectedMinute))
                + "\n" + address + warning;
        long finalScheduledAt = scheduledAt;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.send_request_confirm)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> sendRequest(service, address, description, finalScheduledAt))
                .show();
    }

    private void sendRequest(String service, String address, String description, long scheduledAt) {
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        User me = repo.getCachedUser();
        if (me == null) {
            UiUtils.toast(this, "Please log in again");
            return;
        }
        Booking b = new Booking();
        b.setCustomerId(me.getUserId());
        b.setCustomerName(me.getName());
        b.setCustomerPhone(me.getPhone());
        b.setProviderId(provider.getProviderId());
        b.setProviderName(provider.getName());
        b.setProviderPhone(provider.getPhone());
        b.setService(service);
        b.setDescription(description);
        b.setDate(DateTimeUtils.formatDate(scheduledAt));
        b.setTime(DateTimeUtils.formatTime24(selectedHour, selectedMinute));
        b.setScheduledAt(scheduledAt);
        b.setPrice(provider.getStartingPrice());
        b.setLocation(address);

        setLoading(true);
        repo.createBooking(b, new Callback<Booking>() {
            @Override
            public void onSuccess(Booking created) {
                setLoading(false);
                UiUtils.toast(BookingActivity.this, getString(R.string.booking_sent));
                Intent i = new Intent(BookingActivity.this, BookingDetailsActivity.class);
                i.putExtra(BookingDetailsActivity.EXTRA_BOOKING_ID, created.getBookingId());
                startActivity(i);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                UiUtils.toast(BookingActivity.this, message);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
