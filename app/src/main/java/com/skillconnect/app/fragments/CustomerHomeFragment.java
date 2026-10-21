package com.skillconnect.app.fragments;

import android.content.Intent;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.skillconnect.app.adapters.PromoAdapter;
import com.skillconnect.app.utils.AnimUtils;
import com.skillconnect.app.utils.DateTimeUtils;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.activities.BaseHomeActivity;
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.CategoriesActivity;
import com.skillconnect.app.activities.ChatListActivity;
import com.skillconnect.app.activities.CustomerHomeActivity;
import com.skillconnect.app.activities.LocationPickerActivity;
import com.skillconnect.app.activities.MapActivity;
import com.skillconnect.app.activities.ProviderDetailsActivity;
import com.skillconnect.app.activities.ProviderListActivity;
import com.skillconnect.app.adapters.CategoryCardAdapter;
import com.skillconnect.app.adapters.ProviderAdapter;
import com.skillconnect.app.adapters.ProviderCardHolder;
import com.skillconnect.app.adapters.ProviderCompactAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.FilterOptions;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.FavoriteHelper;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.ProviderSearch;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Customer home: greeting, search, categories, nearby providers and recommendations. */
public class CustomerHomeFragment extends Fragment {

    private static final int NEARBY_LIMIT = 5;
    private static final int RECOMMENDED_LIMIT = 8;

    private AppRepository repo;
    private DatabaseHelper db;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvGreeting;
    private TextView tvLocation;
    private TextView tvLocationHint;
    private ImageView ivAvatar;
    private StateView nearbyState;
    private RecyclerView rvNearby;
    private TextView tvRecommendedTitle;
    private RecyclerView rvRecommended;

    private CategoryCardAdapter categoryAdapter;
    private ProviderAdapter nearbyAdapter;
    private ProviderCompactAdapter recommendedAdapter;
    private ViewPager2 vpPromos;
    private LinearLayout promoDots;
    private View skeletonNearby;
    private ObjectAnimator skeletonAnimator;
    private final Handler autoScroll = new Handler(Looper.getMainLooper());
    private static final long PROMO_INTERVAL_MS = 4500;
    private final Runnable nextPromo = new Runnable() {
        @Override
        public void run() {
            if (vpPromos != null && vpPromos.getAdapter() != null && vpPromos.getAdapter().getItemCount() > 1) {
                int next = (vpPromos.getCurrentItem() + 1) % vpPromos.getAdapter().getItemCount();
                vpPromos.setCurrentItem(next, true);
            }
            autoScroll.postDelayed(this, PROMO_INTERVAL_MS);
        }
    };
    private final List<Provider> allProviders = new ArrayList<>();
    private boolean providersLoaded;
    private TextView tvChatBadge;
    private int unreadChats;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!isAdded()) return;
                if (LocationUtils.hasPermission(requireContext())) {
                    fetchLocation(true);
                } else {
                    UiUtils.toast(requireContext(), "Showing distances from Mumbai. You can enable location any time.");
                }
            });

    /** Result of "Choose on map" – the user picked an area themselves (e.g. Fort). */
    private final ActivityResultLauncher<Intent> locationPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isAdded() || result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                Intent d = result.getData();
                PrefsManager.get(requireContext()).saveManualLocation(
                        d.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0),
                        d.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0),
                        d.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS));
                updateLocationLabel();
                renderProviders();
                UiUtils.toast(requireContext(), "Showing skilled people near " + PrefsManager.get(requireContext()).getLocality());
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = RepositoryProvider.get();
        db = DatabaseHelper.getInstance(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvLocationHint = view.findViewById(R.id.tvLocationHint);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        nearbyState = new StateView(view.findViewById(R.id.nearbyState));
        rvNearby = view.findViewById(R.id.rvNearby);
        tvRecommendedTitle = view.findViewById(R.id.tvRecommendedTitle);
        rvRecommended = view.findViewById(R.id.rvRecommended);
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        tvChatBadge = view.findViewById(R.id.tvChatBadge);
        view.findViewById(R.id.btnMessages).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatListActivity.class)));
        if (getActivity() instanceof BaseHomeActivity) unreadChats = ((BaseHomeActivity) getActivity()).getUnreadChats();
        updateChatBadge();

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadProviders);

        categoryAdapter = new CategoryCardAdapter(this::openCategory);
        rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.submit(CategoryData.getMainCategories());

        nearbyAdapter = new ProviderAdapter(db, repo.getCurrentUserId(), new ProviderCardHolder.Listener() {
            @Override
            public void onOpenProvider(Provider provider) {
                openProvider(provider.getProviderId());
            }

            @Override
            public void onToggleFavorite(Provider provider) {
                FavoriteHelper.toggle(requireContext(), provider);
                nearbyAdapter.notifyDataSetChanged();
            }
        });
        rvNearby.setAdapter(nearbyAdapter);

        recommendedAdapter = new ProviderCompactAdapter(this::openProvider);
        skeletonNearby = view.findViewById(R.id.skeletonNearby);
        setupQuickSkills(view);
        setupPromos(view);
        rvRecommended.setAdapter(recommendedAdapter);

        view.findViewById(R.id.searchBar).setOnClickListener(v -> {
            if (getActivity() instanceof CustomerHomeActivity) ((CustomerHomeActivity) getActivity()).openSearch(null);
        });
        view.findViewById(R.id.btnSeeAllCategories).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoriesActivity.class)));
        view.findViewById(R.id.btnSeeAllNearby).setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), ProviderListActivity.class);
            i.putExtra(ProviderListActivity.EXTRA_TITLE, getString(R.string.nearby_people));
            i.putExtra(ProviderListActivity.EXTRA_SORT, FilterOptions.SORT_DISTANCE);
            startActivity(i);
        });
        view.findViewById(R.id.cardMapBanner).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MapActivity.class)));
        view.findViewById(R.id.layoutLocation).setOnClickListener(v -> showLocationOptions());
        ivAvatar.setOnClickListener(v -> {
            if (getActivity() instanceof BookingsHost) ((BookingsHost) getActivity()).selectTab(R.id.nav_profile);
        });

        bindUser();
        updateLocationLabel();
        loadProviders();
        PrefsManager prefs = PrefsManager.get(requireContext());
        if (prefs.isManualLocation()) {
            // The user chose an area themselves – don't overwrite it with GPS
        } else if (LocationUtils.hasPermission(requireContext())) {
            fetchLocation(false);
        } else if (!prefs.wasLocationAsked()) {
            requestLocation(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nearbyAdapter != null) nearbyAdapter.notifyDataSetChanged();
        if (!isHidden()) startPromoAutoScroll();
    }

    @Override
    public void onPause() {
        autoScroll.removeCallbacks(nextPromo);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        autoScroll.removeCallbacks(nextPromo);
        if (skeletonAnimator != null) skeletonAnimator.cancel();
        super.onDestroyView();
    }

    private void startPromoAutoScroll() {
        autoScroll.removeCallbacks(nextPromo);
        autoScroll.postDelayed(nextPromo, PROMO_INTERVAL_MS);
    }

    // ------------------------------------------------------------------ quick skills & promos

    /** One-tap chips for the most requested skills. */
    private void setupQuickSkills(View view) {
        ChipGroup group = view.findViewById(R.id.chipsQuickSkills);
        String[][] skills = {
                {"Electrician", CategoryData.HOME}, {"Plumber", CategoryData.HOME}, {"AC Repair", CategoryData.HOME},
                {"Math Tutor", CategoryData.EDUCATION}, {"Makeup Artist", CategoryData.CREATIVE},
                {"Photographer", CategoryData.CREATIVE}, {"Fitness Trainer", CategoryData.LIFESTYLE},
                {"Web Developer", CategoryData.TECH}};
        for (String[] skill : skills) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, group, false);
            chip.setText(skill[0]);
            chip.setChipIconResource(CategoryData.getCategoryIcon(skill[1]));
            chip.setChipIconVisible(true);
            chip.setOnClickListener(v -> openSkill(skill[0], skill[1]));
            group.addView(chip);
        }
    }

    private void setupPromos(View view) {
        vpPromos = view.findViewById(R.id.vpPromos);
        promoDots = view.findViewById(R.id.promoDots);
        List<PromoAdapter.Promo> promos = new ArrayList<>();
        promos.add(new PromoAdapter.Promo("SUMMER READY", "AC servicing at your doorstep",
                "Verified technicians near you, from ₹400", "Book now",
                R.drawable.ic_cat_home, R.drawable.bg_promo_indigo, CategoryData.HOME, "AC Repair"));
        promos.add(new PromoAdapter.Promo("LEARN SOMETHING NEW", "Tutors for every subject",
                "Guitar, coding and maths from ₹400/hour", "Find a tutor",
                R.drawable.ic_cat_education, R.drawable.bg_promo_teal, CategoryData.EDUCATION, null));
        promos.add(new PromoAdapter.Promo("WEDDING SEASON", "Top makeup & mehendi artists",
                "Highly rated artists for every occasion", "Explore",
                R.drawable.ic_cat_creative, R.drawable.bg_promo_rose, CategoryData.CREATIVE, null));
        vpPromos.setAdapter(new PromoAdapter(promos, promo -> {
            if (promo.skill != null) openSkill(promo.skill, promo.categoryId);
            else openCategory(CategoryData.getCategory(promo.categoryId));
        }));
        vpPromos.setOffscreenPageLimit(1);
        vpPromos.setPageTransformer(new MarginPageTransformer(UiUtils.dp(requireContext(), 12)));

        promoDots.removeAllViews();
        for (int i = 0; i < promos.size(); i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int m = UiUtils.dp(requireContext(), 3);
            lp.setMargins(m, 0, m, 0);
            dot.setLayoutParams(lp);
            promoDots.addView(dot);
        }
        updatePromoDots(0);
        vpPromos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePromoDots(position);
                startPromoAutoScroll();   // restart the timer after a manual swipe
            }
        });
    }

    private void updatePromoDots(int selected) {
        for (int i = 0; i < promoDots.getChildCount(); i++) {
            ((ImageView) promoDots.getChildAt(i)).setImageResource(
                    i == selected ? R.drawable.bg_indicator_active : R.drawable.bg_indicator_inactive);
        }
    }

    private void openSkill(String skill, String categoryId) {
        Intent i = new Intent(requireContext(), ProviderListActivity.class);
        i.putExtra(ProviderListActivity.EXTRA_TITLE, skill);
        i.putExtra(ProviderListActivity.EXTRA_CATEGORY_ID, categoryId);
        i.putExtra(ProviderListActivity.EXTRA_SKILL, skill);
        startActivity(i);
    }

    /** Pulsing grey placeholder cards instead of a spinner. */
    private void showSkeleton(boolean show) {
        if (show) {
            skeletonNearby.setVisibility(View.VISIBLE);
            if (skeletonAnimator == null) skeletonAnimator = AnimUtils.pulse(skeletonNearby);
        } else {
            if (skeletonAnimator != null) {
                skeletonAnimator.cancel();
                skeletonAnimator = null;
            }
            skeletonNearby.setVisibility(View.GONE);
            skeletonNearby.setAlpha(1f);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) autoScroll.removeCallbacks(nextPromo);
        else if (getView() != null) startPromoAutoScroll();
        if (!hidden && getView() != null) {
            bindUser();
            updateLocationLabel();
            renderProviders();
        }
    }

    /** Called by CustomerHomeActivity when the number of unread messages changes. */
    public void setUnreadChats(int count) {
        unreadChats = count;
        updateChatBadge();
    }

    private void updateChatBadge() {
        if (tvChatBadge == null) return;
        tvChatBadge.setVisibility(unreadChats > 0 ? View.VISIBLE : View.GONE);
        tvChatBadge.setText(unreadChats > 9 ? "9+" : String.valueOf(unreadChats));
    }

    private void bindUser() {
        User user = repo.getCachedUser();
        String name = user != null ? user.getFirstName() : "there";
        tvGreeting.setText(DateTimeUtils.greeting() + ", " + name + " 👋");
        ImageUtils.loadAvatar(ivAvatar, user != null ? user.getProfileImage() : null, user != null ? user.getName() : "?");
    }

    // ------------------------------------------------------------------ location

    /** Tapping the location chip: switch between GPS and a chosen area. */
    private void showLocationOptions() {
        String[] options = {"Use my current location", "Choose an area on the map"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Show skilled people near…")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        requestLocation(true);
                    } else {
                        double[] current = LocationUtils.getUserLocation(requireContext());
                        Intent i = new Intent(requireContext(), LocationPickerActivity.class);
                        i.putExtra(LocationPickerActivity.EXTRA_LAT, current[0]);
                        i.putExtra(LocationPickerActivity.EXTRA_LNG, current[1]);
                        i.putExtra(LocationPickerActivity.EXTRA_ADDRESS, PrefsManager.get(requireContext()).getLocality());
                        locationPicker.launch(i);
                    }
                })
                .show();
    }

    private void requestLocation(boolean userInitiated) {
        if (LocationUtils.hasPermission(requireContext())) {
            if (userInitiated) UiUtils.toast(requireContext(), "Updating your location…");
            fetchLocation(userInitiated);
            return;
        }
        PrefsManager.get(requireContext()).setLocationAsked();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.location_rationale_title)
                .setMessage(R.string.location_rationale)
                .setNegativeButton(R.string.not_now, null)
                .setPositiveButton(R.string.allow, (d, w) -> locationPermission.launch(LocationUtils.PERMISSIONS))
                .show();
    }

    private void fetchLocation(boolean showErrors) {
        LocationUtils.fetchCurrentLocation(requireContext(), new LocationUtils.LocationListener() {
            @Override
            public void onLocation(double latitude, double longitude) {
                if (!isAdded()) return;
                LocationUtils.reverseGeocode(requireContext(), latitude, longitude, address -> {
                    if (!isAdded()) return;
                    // Only an explicit "use my current location" replaces a manually chosen area
                    PrefsManager prefs = PrefsManager.get(requireContext());
                    if (prefs.isManualLocation() && !showErrors) return;
                    prefs.saveGpsLocation(latitude, longitude, address != null ? address : "Current location");
                    updateLocationLabel();
                    renderProviders();
                });
            }

            @Override
            public void onFailed(String message) {
                if (isAdded() && showErrors) UiUtils.toast(requireContext(), message);
            }
        });
    }

    private void updateLocationLabel() {
        PrefsManager prefs = PrefsManager.get(requireContext());
        boolean usingDefault = LocationUtils.isUsingDefault(requireContext());
        tvLocation.setText(usingDefault || prefs.getLocality() == null
                ? getString(R.string.default_location) : prefs.getLocality());
        tvLocationHint.setVisibility(LocationUtils.hasPermission(requireContext()) ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------ data

    private void loadProviders() {
        if (allProviders.isEmpty()) {
            nearbyState.hide();
            showSkeleton(true);
            rvNearby.setVisibility(View.GONE);
        }
        repo.getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> result) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                allProviders.clear();
                allProviders.addAll(result);
                providersLoaded = true;
                renderProviders();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                showSkeleton(false);
                if (allProviders.isEmpty()) {
                    rvNearby.setVisibility(View.GONE);
                    nearbyState.showError(message, CustomerHomeFragment.this::loadProviders);
                } else {
                    UiUtils.toast(requireContext(), message);
                }
            }
        });
    }

    private void renderProviders() {
        if (!isAdded() || !providersLoaded) return;
        showSkeleton(false);
        LocationUtils.applyDistances(requireContext(), allProviders);

        List<Category> categories = CategoryData.getMainCategories();
        for (Category c : categories) c.setProviderCount(CategoryData.countForCategory(allProviders, c.getId()));
        categoryAdapter.submit(categories);

        List<Provider> nearby = ProviderSearch.apply(allProviders, null, new FilterOptions());
        if (nearby.isEmpty()) {
            rvNearby.setVisibility(View.GONE);
            nearbyState.showEmpty(R.drawable.ic_people, getString(R.string.no_providers_title),
                    "Providers will appear here once they join SkillConnect.");
        } else {
            nearbyState.hide();
            boolean firstShow = rvNearby.getVisibility() != View.VISIBLE;
            rvNearby.setVisibility(View.VISIBLE);
            nearbyAdapter.submit(nearby.subList(0, Math.min(NEARBY_LIMIT, nearby.size())));
            if (firstShow) rvNearby.scheduleLayoutAnimation();
        }
        renderRecommended();
    }

    /** V1 recommendations: categories you viewed/saved recently (SQLite), best rated first. */
    private void renderRecommended() {
        List<String> recentCategories = db.getRecentCategories(repo.getCurrentUserId());
        List<Provider> picks = new ArrayList<>();
        for (Provider p : allProviders) {
            if (recentCategories.isEmpty() || recentCategories.contains(p.getCategory())) picks.add(p);
        }
        Collections.sort(picks, (a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()));
        if (picks.size() > RECOMMENDED_LIMIT) picks = picks.subList(0, RECOMMENDED_LIMIT);
        recommendedAdapter.submitProviders(picks);
        int visibility = picks.isEmpty() ? View.GONE : View.VISIBLE;
        tvRecommendedTitle.setVisibility(visibility);
        rvRecommended.setVisibility(visibility);
    }

    private void openCategory(Category category) {
        Intent i = new Intent(requireContext(), ProviderListActivity.class);
        i.putExtra(ProviderListActivity.EXTRA_TITLE, category.getName());
        i.putExtra(ProviderListActivity.EXTRA_CATEGORY_ID, category.getId());
        startActivity(i);
    }

    private void openProvider(String providerId) {
        Intent i = new Intent(requireContext(), ProviderDetailsActivity.class);
        i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, providerId);
        startActivity(i);
    }
}
