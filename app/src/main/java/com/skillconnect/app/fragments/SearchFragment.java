package com.skillconnect.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.app.R;
import com.skillconnect.app.activities.CategoriesActivity;
import com.skillconnect.app.activities.ProviderDetailsActivity;
import com.skillconnect.app.adapters.ProviderCardHolder;
import com.skillconnect.app.adapters.ProviderListAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.FilterOptions;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.FavoriteHelper;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.ProviderSearch;
import com.skillconnect.app.utils.StateView;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search across provider name, skill, category and description.
 * Results are shown in a ListView with a custom adapter. Recent searches are stored in SQLite.
 */
public class SearchFragment extends Fragment {

    private static final String[] POPULAR = {"Electrician", "Guitar", "Photographer", "Web Developer",
            "Video Editor", "Makeup", "Math Tutor", "Yoga"};
    private static final long DEBOUNCE_MS = 250;

    private AppRepository repo;
    private DatabaseHelper db;
    private TextInputEditText etSearch;
    private MaterialButton btnFilters;
    private View layoutSuggestions;
    private View layoutResults;
    private View layoutRecentHeader;
    private ChipGroup chipsRecent;
    private TextView tvResultCount;
    private ListView listResults;
    private StateView state;
    private ProviderListAdapter adapter;

    private final List<Provider> allProviders = new ArrayList<>();
    private boolean loaded;
    private String loadError;
    private FilterOptions filters = new FilterOptions();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::updateResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = RepositoryProvider.get();
        db = DatabaseHelper.getInstance(requireContext());

        etSearch = view.findViewById(R.id.etSearch);
        btnFilters = view.findViewById(R.id.btnFilters);
        layoutSuggestions = view.findViewById(R.id.layoutSuggestions);
        layoutResults = view.findViewById(R.id.layoutResults);
        layoutRecentHeader = view.findViewById(R.id.layoutRecentHeader);
        chipsRecent = view.findViewById(R.id.chipsRecent);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        listResults = view.findViewById(R.id.listResults);
        state = new StateView(view.findViewById(R.id.searchState));

        adapter = new ProviderListAdapter(requireContext(), repo.getCurrentUserId(), new ProviderCardHolder.Listener() {
            @Override
            public void onOpenProvider(Provider provider) {
                openProvider(provider);
            }

            @Override
            public void onToggleFavorite(Provider provider) {
                FavoriteHelper.toggle(requireContext(), provider);
                adapter.notifyDataSetChanged();
            }
        });
        listResults.setAdapter(adapter);
        // Classic ListView item click
        listResults.setOnItemClickListener((parent, v, position, id) -> openProvider(adapter.getItem(position)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                saveRecentSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });

        btnFilters.setOnClickListener(v ->
                FilterBottomSheet.newInstance(filters, false).show(getChildFragmentManager(), "filters"));
        getChildFragmentManager().setFragmentResultListener(FilterBottomSheet.REQUEST_KEY, getViewLifecycleOwner(),
                (key, bundle) -> {
                    FilterOptions f = (FilterOptions) bundle.getSerializable(FilterBottomSheet.RESULT_FILTERS);
                    if (f != null) filters = f;
                    updateFilterButton();
                    updateResults();
                });

        view.findViewById(R.id.btnClearRecent).setOnClickListener(v -> {
            db.clearRecentSearches(repo.getCurrentUserId());
            renderRecentSearches();
        });
        view.findViewById(R.id.btnBrowseCategories).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoriesActivity.class)));

        ChipGroup chipsPopular = view.findViewById(R.id.chipsPopular);
        for (String term : POPULAR) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, chipsPopular, false);
            chip.setText(term);
            chip.setOnClickListener(v -> setQuery(term));
            chipsPopular.addView(chip);
        }

        updateFilterButton();
        renderRecentSearches();
        loadProviders();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) {
            renderRecentSearches();
            if (!loaded) loadProviders();
        } else if (hidden && getView() != null) {
            hideKeyboard();
        }
    }

    /** Called by the home screen search bar and the suggestion chips. */
    public void setQuery(@Nullable String query) {
        if (etSearch == null) return;
        if (query != null) {
            etSearch.setText(query);
            etSearch.setSelection(query.length());
            saveRecentSearch();
            hideKeyboard();
        } else {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private String query() {
        return etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
    }

    private void loadProviders() {
        loadError = null;
        repo.getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> result) {
                if (!isAdded()) return;
                loaded = true;
                allProviders.clear();
                allProviders.addAll(result);
                LocationUtils.applyDistances(requireContext(), allProviders);
                updateResults();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                loadError = message;
                updateResults();
            }
        });
    }

    private void updateResults() {
        if (getView() == null) return;
        String q = query();
        boolean searching = !q.isEmpty() || filters.activeCount() > 0;
        layoutSuggestions.setVisibility(searching ? View.GONE : View.VISIBLE);
        layoutResults.setVisibility(searching ? View.VISIBLE : View.GONE);
        if (!searching) return;

        if (loadError != null) {
            adapter.submit(new ArrayList<>());
            tvResultCount.setText("");
            state.showError(loadError, this::loadProviders);
            return;
        }
        if (!loaded) {
            state.showLoading(getString(R.string.finding_people));
            return;
        }
        List<Provider> results = ProviderSearch.apply(allProviders, q, filters);
        adapter.submit(results);
        tvResultCount.setText(results.size() == 1 ? "1 provider found" : results.size() + " providers found");
        if (results.isEmpty()) {
            state.showEmpty(R.drawable.ic_search, getString(R.string.no_providers_title),
                    getString(R.string.no_providers_msg),
                    filters.activeCount() > 0 ? "Clear filters" : null, () -> {
                        filters = new FilterOptions();
                        updateFilterButton();
                        updateResults();
                    });
        } else {
            state.hide();
        }
    }

    private void updateFilterButton() {
        int count = filters.activeCount();
        btnFilters.setText(count > 0 ? String.valueOf(count) : "");
        btnFilters.setIconPadding(count > 0 ? 8 : 0);
    }

    private void saveRecentSearch() {
        String q = query();
        if (q.length() >= 2) {
            db.addRecentSearch(repo.getCurrentUserId(), q);
            renderRecentSearches();
        }
    }

    private void renderRecentSearches() {
        chipsRecent.removeAllViews();
        List<String> recent = db.getRecentSearches(repo.getCurrentUserId());
        layoutRecentHeader.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
        chipsRecent.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
        for (String term : recent) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, chipsRecent, false);
            chip.setText(term);
            chip.setChipIconResource(R.drawable.ic_history);
            chip.setChipIconVisible(true);
            chip.setCloseIconVisible(true);
            chip.setOnClickListener(v -> setQuery(term));
            chip.setOnCloseIconClickListener(v -> {
                db.deleteRecentSearch(repo.getCurrentUserId(), term);
                renderRecentSearches();
            });
            chipsRecent.addView(chip);
        }
    }

    private void openProvider(Provider provider) {
        saveRecentSearch();
        Intent i = new Intent(requireContext(), ProviderDetailsActivity.class);
        i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, provider.getProviderId());
        startActivity(i);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(searchRunnable);
        super.onDestroyView();
    }
}
