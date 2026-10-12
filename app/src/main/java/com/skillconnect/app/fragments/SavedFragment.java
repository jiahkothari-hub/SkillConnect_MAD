package com.skillconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.ProviderDetailsActivity;
import com.skillconnect.app.adapters.ProviderCompactAdapter;
import com.skillconnect.app.adapters.SavedProviderAdapter;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.FavoriteProvider;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/** Saved providers and recently viewed providers – both read from SQLite. */
public class SavedFragment extends Fragment {

    private DatabaseHelper db;
    private String userId;
    private SavedProviderAdapter savedAdapter;
    private ProviderCompactAdapter recentAdapter;
    private StateView state;
    private RecyclerView rvSaved;
    private RecyclerView rvRecent;
    private View recentHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = DatabaseHelper.getInstance(requireContext());
        userId = RepositoryProvider.get().getCurrentUserId();
        state = new StateView(view.findViewById(R.id.savedState));
        rvSaved = view.findViewById(R.id.rvSaved);
        rvRecent = view.findViewById(R.id.rvRecent);
        recentHeader = view.findViewById(R.id.layoutRecentHeader);

        savedAdapter = new SavedProviderAdapter(new SavedProviderAdapter.Listener() {
            @Override
            public void onOpen(FavoriteProvider item) {
                openProvider(item.getProviderId());
            }

            @Override
            public void onRemove(FavoriteProvider item) {
                db.removeFavorite(userId, item.getProviderId());           // SQLite DELETE
                RepositoryProvider.get().syncFavorite(item.getProviderId(), false);
                UiUtils.toast(requireContext(), item.getName() + " removed from saved");
                refresh();
            }
        });
        rvSaved.setAdapter(savedAdapter);

        recentAdapter = new ProviderCompactAdapter(this::openProvider);
        rvRecent.setAdapter(recentAdapter);

        view.findViewById(R.id.btnClearRecent).setOnClickListener(v -> {
            db.clearRecentlyViewed(userId);
            refresh();
        });
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) refresh();
    }

    /** SQLite SELECT for both lists. */
    private void refresh() {
        List<FavoriteProvider> saved = db.getFavorites(userId);
        savedAdapter.submit(saved);
        if (saved.isEmpty()) {
            rvSaved.setVisibility(View.GONE);
            state.showEmpty(R.drawable.ic_favorite_border, getString(R.string.no_saved_title), getString(R.string.no_saved_msg),
                    getString(R.string.find_provider), () -> {
                        if (getActivity() instanceof BookingsHost) ((BookingsHost) getActivity()).selectTab(R.id.nav_search);
                    });
        } else {
            state.hide();
            rvSaved.setVisibility(View.VISIBLE);
        }
        List<FavoriteProvider> recent = db.getRecentlyViewed(userId);
        recentAdapter.submitSaved(recent);
        int visibility = recent.isEmpty() ? View.GONE : View.VISIBLE;
        recentHeader.setVisibility(visibility);
        rvRecent.setVisibility(visibility);
    }

    private void openProvider(String providerId) {
        Intent i = new Intent(requireContext(), ProviderDetailsActivity.class);
        i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, providerId);
        startActivity(i);
    }
}
