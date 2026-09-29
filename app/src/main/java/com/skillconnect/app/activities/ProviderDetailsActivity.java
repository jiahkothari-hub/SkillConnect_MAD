package com.skillconnect.app.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.PortfolioAdapter;
import com.skillconnect.app.adapters.ReviewAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.FavoriteHelper;
import com.skillconnect.app.utils.ImagePreviewDialog;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/** Premium provider profile: details, availability, skills, portfolio, reviews and actions. */
public class ProviderDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "provider_id";

    private AppRepository repo;
    private DatabaseHelper db;
    private String providerId;
    private Provider provider;

    private View scrollContent;
    private View bottomBar;
    private StateView state;
    private ImageButton btnFavorite;
    private PortfolioAdapter portfolioAdapter;
    private ReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_details);
        repo = RepositoryProvider.get();
        db = DatabaseHelper.getInstance(this);
        providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);

        scrollContent = findViewById(R.id.scrollContent);
        bottomBar = findViewById(R.id.bottomBar);
        state = new StateView(findViewById(R.id.detailsState));
        btnFavorite = findViewById(R.id.btnFavorite);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> {
            if (provider == null) return;
            FavoriteHelper.toggle(this, provider);
            updateFavoriteIcon();
        });
        findViewById(R.id.btnContact).setOnClickListener(v -> showContactOptions());
        findViewById(R.id.btnMap).setOnClickListener(v -> openMap());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareProfile());

        RecyclerView rvPortfolio = findViewById(R.id.rvPortfolio);
        rvPortfolio.setLayoutManager(new GridLayoutManager(this, 3));
        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        reviewAdapter = new ReviewAdapter();
        rvReviews.setAdapter(reviewAdapter);

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (provider != null) updateFavoriteIcon();
    }

    private void load() {
        if (providerId == null) {
            state.showError("Provider not found", null);
            return;
        }
        state.showLoading(getString(R.string.loading));
        repo.getProvider(providerId, new Callback<Provider>() {
            @Override
            public void onSuccess(Provider result) {
                provider = result;
                state.hide();
                scrollContent.setVisibility(View.VISIBLE);
                bind();
                loadPortfolio();
                loadReviews();
                rememberView();
            }

            @Override
            public void onError(String message) {
                state.showError(message, ProviderDetailsActivity.this::load);
            }
        });
    }

    private void bind() {
        Provider p = provider;
        User viewer = repo.getCachedUser();
        boolean isOwnProfile = viewer != null && p.getProviderId().equals(viewer.getUserId());
        boolean isCustomer = viewer != null && viewer.isCustomer();
        boolean isAdmin = viewer != null && viewer.isAdmin();

        ImageUtils.loadAvatar(findViewById(R.id.ivAvatar), p.getProfileImage(), p.getName());
        ((TextView) findViewById(R.id.tvName)).setText(p.getName());
        ((TextView) findViewById(R.id.tvTitle)).setText(p.getTitle() + " · " + CategoryData.getCategoryName(p.getCategory()));
        findViewById(R.id.ivVerified).setVisibility(p.isVerified() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvVerifiedLabel).setVisibility(p.isVerified() ? View.VISIBLE : View.GONE);

        ((RatingBar) findViewById(R.id.ratingBar)).setRating((float) p.getAverageRating());
        ((TextView) findViewById(R.id.tvRatingSummary)).setText(p.getReviewCount() > 0
                ? UiUtils.formatRating(p.getAverageRating()) + " · " + UiUtils.reviewCount(p.getReviewCount())
                : "New on SkillConnect");

        String address = p.getAddress() != null ? p.getAddress() : "Location not set";
        if (p.hasLocation()) {
            double[] me = LocationUtils.getUserLocation(this);
            double km = LocationUtils.distanceKm(me[0], me[1], p.getLatitude(), p.getLongitude());
            address += "  ·  " + LocationUtils.formatDistance(km);
        }
        ((TextView) findViewById(R.id.tvAddress)).setText(address);

        ((TextView) findViewById(R.id.tvStatExperience)).setText(p.getExperienceYears() + (p.getExperienceYears() == 1 ? " yr" : " yrs"));
        ((TextView) findViewById(R.id.tvStatPrice)).setText(UiUtils.formatPrice(p.getStartingPrice()));
        ((TextView) findViewById(R.id.tvStatPriceUnit)).setText(p.getPriceUnit());
        ((TextView) findViewById(R.id.tvStatRating)).setText(p.getReviewCount() > 0 ? UiUtils.formatRating(p.getAverageRating()) : "–");

        TextView availableNow = findViewById(R.id.tvAvailableNow);
        availableNow.setText(p.isAvailable() ? getString(R.string.available_for_work) : getString(R.string.currently_busy));
        availableNow.setTextColor(ContextCompat.getColor(this, p.isAvailable() ? R.color.success : R.color.text_secondary));
        availableNow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                p.isAvailable() ? R.color.success_light : R.color.surface_variant)));
        ((TextView) findViewById(R.id.tvDays)).setText(DateTimeUtils.summarizeDays(p.getAvailableDays()));
        ((TextView) findViewById(R.id.tvHours)).setText(DateTimeUtils.hoursRange(p.getAvailableFrom(), p.getAvailableTo()));

        String about = p.getDescription();
        ((TextView) findViewById(R.id.tvAbout)).setText(about != null && !about.isEmpty() ? about
                : p.getName() + " hasn't added a description yet.");

        ChipGroup chips = findViewById(R.id.chipsSkills);
        chips.removeAllViews();
        for (String skill : p.getSkills()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, chips, false);
            chip.setText(skill);
            chip.setChipIconResource(R.drawable.ic_check);
            chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
            chip.setChipIconVisible(true);
            chip.setClickable(false);
            chips.addView(chip);
        }

        portfolioAdapter = new PortfolioAdapter(false, CategoryData.getCategoryIcon(p.getCategory()), new PortfolioAdapter.Listener() {
            @Override
            public void onItemClick(PortfolioItem item) {
                ImagePreviewDialog.show(ProviderDetailsActivity.this, item, CategoryData.getCategoryIcon(p.getCategory()));
            }

            @Override
            public void onItemLongClick(PortfolioItem item) {
                onItemClick(item);
            }
        });
        ((RecyclerView) findViewById(R.id.rvPortfolio)).setAdapter(portfolioAdapter);

        // Booking bar: customers book, the owner edits, admins/other providers only view.
        ((TextView) findViewById(R.id.tvBottomPrice)).setText(UiUtils.startingPrice(p.getStartingPrice()));
        ((TextView) findViewById(R.id.tvBottomUnit)).setText(p.getPriceUnit());
        MaterialButton btnBook = findViewById(R.id.btnBook);
        boolean listingActive = p.isActive();
        findViewById(R.id.tvUnavailable).setVisibility(listingActive ? View.GONE : View.VISIBLE);
        if (isOwnProfile) {
            bottomBar.setVisibility(View.VISIBLE);
            btnBook.setText(R.string.edit_profile);
            btnBook.setOnClickListener(v -> startActivity(new Intent(this, EditProviderProfileActivity.class)));
        } else if (isCustomer && listingActive) {
            bottomBar.setVisibility(View.VISIBLE);
            btnBook.setText(R.string.book_service);
            btnBook.setOnClickListener(v -> {
                Intent i = new Intent(this, BookingActivity.class);
                i.putExtra(BookingActivity.EXTRA_PROVIDER_ID, p.getProviderId());
                startActivity(i);
            });
        } else {
            bottomBar.setVisibility(View.GONE);
        }
        btnFavorite.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        if (!isCustomer && !isAdmin) findViewById(R.id.btnContact).setEnabled(!isOwnProfile);
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        boolean fav = db.isFavorite(repo.getCurrentUserId(), providerId);
        btnFavorite.setImageResource(fav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        btnFavorite.setColorFilter(ContextCompat.getColor(this, fav ? R.color.error : R.color.text_primary));
    }

    /** SQLite: recently viewed (INSERT) + refresh the saved snapshot (UPDATE). */
    private void rememberView() {
        User viewer = repo.getCachedUser();
        if (viewer == null || !viewer.isCustomer()) return;
        db.addRecentlyViewed(viewer.getUserId(), provider);
        db.updateFavoriteSnapshot(viewer.getUserId(), provider);
    }

    private void loadPortfolio() {
        repo.getPortfolio(providerId, new Callback<List<PortfolioItem>>() {
            @Override
            public void onSuccess(List<PortfolioItem> items) {
                portfolioAdapter.submit(items);
                findViewById(R.id.tvNoPortfolio).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                ((TextView) findViewById(R.id.tvPortfolioCount)).setText(items.isEmpty() ? "" : items.size() + " photos");
            }

            @Override
            public void onError(String message) {
                TextView empty = findViewById(R.id.tvNoPortfolio);
                empty.setVisibility(View.VISIBLE);
                empty.setText(message);
            }
        });
    }

    private void loadReviews() {
        repo.getReviews(providerId, new Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                reviewAdapter.submit(reviews);
                findViewById(R.id.tvNoReviews).setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                ((TextView) findViewById(R.id.tvReviewsSummary)).setText(provider.getReviewCount() > 0
                        ? UiUtils.formatRating(provider.getAverageRating()) + " (" + provider.getReviewCount() + ")" : "");
            }

            @Override
            public void onError(String message) {
                TextView empty = findViewById(R.id.tvNoReviews);
                empty.setVisibility(View.VISIBLE);
                empty.setText(message);
            }
        });
    }

    // ------------------------------------------------------------ implicit intents

    private void showContactOptions() {
        if (provider == null) return;
        String phone = provider.getPhone();
        if (phone == null || phone.isEmpty()) {
            UiUtils.toast(this, "This provider hasn't added a phone number");
            return;
        }
        String[] options = {"Call " + provider.getName().split(" ")[0], "Send SMS", "WhatsApp"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contact)
                .setItems(options, (d, which) -> {
                    String message = "Hi " + provider.getName().split(" ")[0]
                            + ", I found you on SkillConnect and would like to know more about your "
                            + provider.getTitle() + " service.";
                    Intent intent;
                    if (which == 0) {
                        intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    } else if (which == 1) {
                        intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
                        intent.putExtra("sms_body", message);
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/91" + phone + "?text=" + Uri.encode(message)));
                    }
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        UiUtils.toast(this, "No app found for this action");
                    }
                })
                .show();
    }

    private void openMap() {
        if (provider == null) return;
        if (!provider.hasLocation()) {
            UiUtils.toast(this, "This provider hasn't set a service location yet");
            return;
        }
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra(MapActivity.EXTRA_FOCUS_PROVIDER_ID, provider.getProviderId());
        startActivity(i);
    }

    private void shareProfile() {
        if (provider == null) return;
        String text = "Check out " + provider.getName() + " (" + provider.getTitle() + ") on SkillConnect!\n"
                + "★ " + UiUtils.formatRating(provider.getAverageRating()) + " · "
                + UiUtils.startingPrice(provider.getStartingPrice()) + " " + provider.getPriceUnit() + "\n"
                + "📍 " + (provider.getAddress() != null ? provider.getAddress() : "") + "\n\n"
                + "Find the right skill. Connect with the right person.";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share profile"));
    }
}
