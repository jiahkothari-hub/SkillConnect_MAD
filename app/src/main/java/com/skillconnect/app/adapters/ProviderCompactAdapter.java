package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.models.FavoriteProvider;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Small horizontal cards ("Recommended for You", "Recently viewed").
 * Works with cloud Providers or local SQLite FavoriteProvider rows.
 */
public class ProviderCompactAdapter extends RecyclerView.Adapter<ProviderCompactAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(String providerId);
    }

    private static class Row {
        String id, name, skill, image;
        double rating, price;
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public ProviderCompactAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitProviders(List<Provider> list) {
        rows.clear();
        for (Provider p : list) {
            Row r = new Row();
            r.id = p.getProviderId();
            r.name = p.getName();
            r.skill = p.getTitle();
            r.image = p.getProfileImage();
            r.rating = p.getAverageRating();
            r.price = p.getStartingPrice();
            rows.add(r);
        }
        notifyDataSetChanged();
    }

    public void submitSaved(List<FavoriteProvider> list) {
        rows.clear();
        for (FavoriteProvider f : list) {
            Row r = new Row();
            r.id = f.getProviderId();
            r.name = f.getName();
            r.skill = f.getSkill();
            r.image = f.getImageUrl();
            r.rating = f.getRating();
            r.price = f.getPrice();
            rows.add(r);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider_compact, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row r = rows.get(position);
        ImageUtils.loadAvatar(h.avatar, r.image, r.name);
        h.name.setText(r.name);
        h.skill.setText(r.skill);
        h.rating.setText(r.rating > 0 ? UiUtils.formatRating(r.rating) : "New");
        h.price.setText(UiUtils.formatPrice(r.price));
        h.itemView.findViewById(R.id.cardCompact).setOnClickListener(v -> listener.onOpen(r.id));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name, skill, rating, price;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.ivAvatar);
            name = v.findViewById(R.id.tvName);
            skill = v.findViewById(R.id.tvSkill);
            rating = v.findViewById(R.id.tvRating);
            price = v.findViewById(R.id.tvPrice);
        }
    }
}
