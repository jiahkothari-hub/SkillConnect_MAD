package com.skillconnect.app.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.skillconnect.app.R;
import com.skillconnect.app.models.PortfolioItem;

import java.util.ArrayList;
import java.util.List;

/** Portfolio image grid. Demo placeholders are drawn as tinted tiles. */
public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(PortfolioItem item);

        void onItemLongClick(PortfolioItem item);
    }

    private static final int[][] PLACEHOLDER_COLORS = {
            {R.color.primary_light, R.color.primary},
            {R.color.secondary_light, R.color.secondary},
            {R.color.cat_creative_bg, R.color.cat_creative},
            {R.color.cat_home_bg, R.color.cat_home},
            {R.color.cat_tech_bg, R.color.cat_tech},
    };

    private final List<PortfolioItem> items = new ArrayList<>();
    private final Listener listener;
    private final boolean showCaptions;
    private final int placeholderIcon;

    public PortfolioAdapter(boolean showCaptions, int placeholderIcon, Listener listener) {
        this.showCaptions = showCaptions;
        this.placeholderIcon = placeholderIcon;
        this.listener = listener;
    }

    public void submit(List<PortfolioItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_portfolio, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PortfolioItem item = items.get(position);
        if (item.isDemoPlaceholder()) {
            int[] colors = PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length];
            Glide.with(h.image).clear(h.image);
            h.image.setImageDrawable(null);
            h.image.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), colors[0])));
            h.placeholder.setVisibility(View.VISIBLE);
            h.placeholder.setImageResource(placeholderIcon);
            h.placeholder.setColorFilter(ContextCompat.getColor(h.itemView.getContext(), colors[1]));
        } else {
            h.placeholder.setVisibility(View.GONE);
            h.image.setBackgroundTintList(null);
            Glide.with(h.image).load(item.getImageUrl()).centerCrop().into(h.image);
        }
        boolean hasCaption = showCaptions && item.getDescription() != null && !item.getDescription().isEmpty();
        h.caption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
        h.caption.setText(item.getDescription());
        h.image.setOnClickListener(v -> listener.onItemClick(item));
        h.image.setOnLongClickListener(v -> {
            listener.onItemLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final ImageView placeholder;
        final TextView caption;

        ViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            placeholder = v.findViewById(R.id.ivPlaceholder);
            caption = v.findViewById(R.id.tvCaption);
        }
    }
}
