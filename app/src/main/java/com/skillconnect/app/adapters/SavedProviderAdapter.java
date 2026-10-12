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
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Saved providers read from the local SQLite table favorite_providers. */
public class SavedProviderAdapter extends RecyclerView.Adapter<SavedProviderAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(FavoriteProvider item);

        void onRemove(FavoriteProvider item);
    }

    private final List<FavoriteProvider> items = new ArrayList<>();
    private final Listener listener;

    public SavedProviderAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<FavoriteProvider> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FavoriteProvider f = items.get(position);
        ImageUtils.loadAvatar(h.avatar, f.getImageUrl(), f.getName());
        h.name.setText(f.getName());
        h.skill.setText(f.getSkill());
        String rating = f.getRating() > 0 ? UiUtils.formatRating(f.getRating()) : "New";
        h.meta.setText(rating + "  ·  " + UiUtils.startingPrice(f.getPrice()) + " " + (f.getPriceUnit() != null ? f.getPriceUnit() : ""));
        h.card.setOnClickListener(v -> listener.onOpen(f));
        h.remove.setOnClickListener(v -> listener.onRemove(f));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView avatar;
        final TextView name, skill, meta;
        final View remove;

        ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.cardSaved);
            avatar = v.findViewById(R.id.ivAvatar);
            name = v.findViewById(R.id.tvName);
            skill = v.findViewById(R.id.tvSkill);
            meta = v.findViewById(R.id.tvMeta);
            remove = v.findViewById(R.id.btnRemove);
        }
    }
}
