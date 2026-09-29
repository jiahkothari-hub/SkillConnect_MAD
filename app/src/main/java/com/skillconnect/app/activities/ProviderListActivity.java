package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.ProviderCardHolder;
import com.skillconnect.app.adapters.ProviderListAdapter;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.fragments.FilterBottomSheet;
import com.skillconnect.app.models.FilterOptions;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.FavoriteHelper;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.ProviderSearch;
import com.skillconnect.app.utils.StateView;

import java.util.ArrayList;
import java.util.List;

/** Provider listing (ListView + custom adapter) for a category, skill or "nearby". */
public class ProviderListActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_SKILL = "skill";
    public static final String EXTRA_SORT = "sort";

    private ProviderListAdapter adapter;
    private StateView state;
    private TextView tvResultCount;
    private MaterialButton btnFilters;
    private FilterOptions filters = new FilterOptions();
    private boolean categoryLocked;
    private final List<Provider> allProviders = new ArrayList<>();
    private boolean loaded;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_list);

        Intent intent = getIntent();
        filters.categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID);
        filters.skill = intent.getStringExtra(EXTRA_SKILL);
        filters.sortBy = intent.getIntExtra(EXTRA_SORT, FilterOptions.SORT_RELEVANCE);
        categoryLocked = filters.categoryId != null;
        String title = intent.getStringExtra(EXTRA_TITLE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title != null ? title : getString(R.string.nearby_people));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_provider_list);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_map) {
                Intent map = new Intent(this, MapActivity.class);
                map.putExtra(MapActivity.EXTRA_CATEGORY_ID, filters.categoryId);
                startActivity(map);
                return true;
            }
            return false;
        });

        tvResultCount = findViewById(R.id.tvResultCount);
        btnFilters = findViewById(R.id.btnFilters);
        state = new StateView(findViewById(R.id.listState));
        ListView listView = findViewById(R.id.listProviders);

        adapter = new ProviderListAdapter(this, RepositoryProvider.get().getCurrentUserId(), new ProviderCardHolder.Listener() {
            @Override
            public void onOpenProvider(Provider provider) {
                openProvider(provider);
            }

            @Override
            public void onToggleFavorite(Provider provider) {
                FavoriteHelper.toggle(ProviderListActivity.this, provider);
                adapter.notifyDataSetChanged();
            }
        });
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openProvider(adapter.getItem(position)));

        btnFilters.setOnClickListener(v ->
                FilterBottomSheet.newInstance(filters, categoryLocked).show(getSupportFragmentManager(), "filters"));
        getSupportFragmentManager().setFragmentResultListener(FilterBottomSheet.REQUEST_KEY, this, (key, bundle) -> {
            FilterOptions f = (FilterOptions) bundle.getSerializable(FilterBottomSheet.RESULT_FILTERS);
            if (f != null) filters = f;
            render();
        });

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void load() {
        state.showLoading(getString(R.string.finding_people));
        RepositoryProvider.get().getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> result) {
                loaded = true;
                allProviders.clear();
                allProviders.addAll(result);
                LocationUtils.applyDistances(ProviderListActivity.this, allProviders);
                render();
            }

            @Override
            public void onError(String message) {
                tvResultCount.setText("");
                state.showError(message, ProviderListActivity.this::load);
            }
        });
    }

    private void render() {
        int extra = filters.activeCount() - (categoryLocked ? 1 : 0);
        btnFilters.setText(extra > 0 ? getString(R.string.filters) + " (" + extra + ")" : getString(R.string.filters));
        if (!loaded) return;
        List<Provider> results = ProviderSearch.apply(allProviders, null, filters);
        adapter.submit(results);
        tvResultCount.setText(results.size() == 1 ? "1 provider found" : results.size() + " providers found");
        if (results.isEmpty()) {
            state.showEmpty(R.drawable.ic_search, getString(R.string.no_providers_title), getString(R.string.no_providers_msg),
                    extra > 0 ? "Clear filters" : null, () -> {
                        FilterOptions reset = new FilterOptions();
                        reset.categoryId = categoryLocked ? filters.categoryId : null;
                        reset.skill = filters.skill;
                        filters = reset;
                        render();
                    });
        } else {
            state.hide();
        }
    }

    private void openProvider(Provider provider) {
        Intent i = new Intent(this, ProviderDetailsActivity.class);
        i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, provider.getProviderId());
        startActivity(i);
    }
}
