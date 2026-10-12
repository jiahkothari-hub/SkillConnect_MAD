package com.skillconnect.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

/** 5-star rating + text review for a COMPLETED booking. Updates the provider's average rating. */
public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    private static final String[] LABELS = {"Tap a star to rate", "Terrible", "Bad", "Okay", "Good", "Excellent"};

    private AppRepository repo;
    private Booking booking;
    private StateView state;
    private RatingBar ratingBar;
    private TextView tvRatingLabel;
    private TextInputEditText etComment;
    private MaterialButton btnSubmit;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        repo = RepositoryProvider.get();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        state = new StateView(findViewById(R.id.reviewState));
        ratingBar = findViewById(R.id.ratingBar);
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        progress = findViewById(R.id.progress);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) ->
                tvRatingLabel.setText(LABELS[Math.max(0, Math.min(5, Math.round(rating)))]));
        btnSubmit.setOnClickListener(v -> submit());
        load();
    }

    private void load() {
        state.showLoading(getString(R.string.loading));
        repo.getBooking(getIntent().getStringExtra(EXTRA_BOOKING_ID), new Callback<Booking>() {
            @Override
            public void onSuccess(Booking result) {
                booking = result;
                if (!booking.canBeReviewed()) {
                    UiUtils.toast(ReviewActivity.this, booking.isReviewed()
                            ? "You have already reviewed this booking." : "You can review only after the job is completed.");
                    finish();
                    return;
                }
                state.hide();
                findViewById(R.id.scrollContent).setVisibility(View.VISIBLE);
                String first = booking.getProviderName().split(" ")[0];
                ((TextView) findViewById(R.id.tvQuestion)).setText("How was your experience with " + first + "?");
                ((TextView) findViewById(R.id.tvService)).setText(booking.getService() + " · " + booking.getDate());
                ImageUtils.loadAvatar(findViewById(R.id.ivAvatar), null, booking.getProviderName());
                repo.getProvider(booking.getProviderId(), new Callback<Provider>() {
                    @Override
                    public void onSuccess(Provider p) {
                        ImageUtils.loadAvatar(findViewById(R.id.ivAvatar), p.getProfileImage(), p.getName());
                    }

                    @Override
                    public void onError(String message) {
                        // keep the initials avatar
                    }
                });
            }

            @Override
            public void onError(String message) {
                state.showError(message, ReviewActivity.this::load);
            }
        });
    }

    private void submit() {
        int rating = Math.round(ratingBar.getRating());
        if (rating < 1) {
            UiUtils.toast(this, "Please choose a star rating");
            return;
        }
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        User me = repo.getCachedUser();
        Review review = new Review();
        review.setBookingId(booking.getBookingId());
        review.setProviderId(booking.getProviderId());
        review.setCustomerId(booking.getCustomerId());
        review.setCustomerName(me != null ? me.getName() : booking.getCustomerName());
        review.setRating(rating);
        review.setComment(etComment.getText() != null ? etComment.getText().toString().trim() : "");

        progress.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        repo.submitReview(review, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                UiUtils.toast(ReviewActivity.this, "Thanks for your review!");
                finish();
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                UiUtils.toast(ReviewActivity.this, message);
            }
        });
    }
}
