package com.skillconnect.app.fragments;

import android.content.Intent;
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
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.CategoriesActivity;
import com.skillconnect.app.activities.CustomerHomeActivity;
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
    private final List<Provider> allProviders = new ArrayList<>();

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!isAdded()) return;
                if (LocationUtils.hasPermission(requireContext())) {
                    fetchLocation(true);
                } else {
                    UiUtils.toast(requireContext(), "Showing distances from Mumbai. You can enable location any time.");
                }
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
        view.findViewById(R.id.layoutLocation).setOnClickListener(v -> requestLocation(true));
        ivAvatar.setOnClickListener(v -> {
            if (getActivity() instanceof BookingsHost) ((BookingsHost) getActivity()).selectTab(R.id.nav_profile);
        });

        bindUser();
        updateLocationLabel();
        loadProviders();
        if (LocationUtils.hasPermission(requireContext())) fetchLocation(false);
        else if (!PrefsManager.get(requireContext()).wasLocationAsked()) requestLocation(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nearbyAdapter != null) nearbyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) {
            bindUser();
            nearbyAdapter.notifyDataSetChanged();
            renderRecommended();
        }
    }

    private void bindUser() {
        User user = repo.getCachedUser();
        String name = user != null ? user.getFirstName() : "there";
        tvGreeting.setText("Hello, " + name + " 👋");
        ImageUtils.loadAvatar(ivAvatar, user != null ? user.getProfileImage() : null, user != null ? user.getName() : "?");
    }

    // ------------------------------------------------------------------ location

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
                    PrefsManager.get(requireContext()).saveLocation(latitude, longitude,
                            address != null ? address : "Current location");
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
            nearbyState.showLoading(getString(R.string.finding_people));
            rvNearby.setVisibility(View.GONE);
        }
        repo.getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> result) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                allProviders.clear();
                allProviders.addAll(result);
                renderProviders();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
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
        if (!isAdded()) return;
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
            rvNearby.setVisibility(View.VISIBLE);
            nearbyAdapter.submit(nearby.subList(0, Math.min(NEARBY_LIMIT, nearby.size())));
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
