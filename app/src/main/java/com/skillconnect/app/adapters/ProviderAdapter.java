package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.Provider;

import java.util.ArrayList;
import java.util.List;

/** RecyclerView adapter for provider cards (home screen "Nearby Skilled People"). */
public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {

    private final List<Provider> providers = new ArrayList<>();
    private final DatabaseHelper db;
    private final String userId;
    private final ProviderCardHolder.Listener listener;

    public ProviderAdapter(DatabaseHelper db, String userId, ProviderCardHolder.Listener listener) {
        this.db = db;
        this.userId = userId;
        this.listener = listener;
    }

    public void submit(List<Provider> list) {
        providers.clear();
        providers.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Provider p = providers.get(position);
        holder.card.bind(p, db.isFavorite(userId, p.getProviderId()), listener);
        holder.itemView.findViewById(R.id.cardProvider).setOnClickListener(v -> listener.onOpenProvider(p));
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ProviderCardHolder card;

        ViewHolder(View v) {
            super(v);
            card = new ProviderCardHolder(v);
        }
    }
}
