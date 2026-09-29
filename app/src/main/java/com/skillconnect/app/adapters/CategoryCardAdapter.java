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

import com.skillconnect.app.R;
import com.skillconnect.app.models.Category;

import java.util.ArrayList;
import java.util.List;

/** Horizontal category tiles on the customer home screen. */
public class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardAdapter.ViewHolder> {

    public interface Listener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final Listener listener;

    public CategoryCardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Category> list) {
        categories.clear();
        categories.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Category c = categories.get(position);
        h.icon.setImageResource(c.getIconRes());
        h.icon.setColorFilter(ContextCompat.getColor(h.itemView.getContext(), c.getColorRes()));
        h.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), c.getBackgroundRes())));
        h.name.setText(c.getName());
        h.count.setText(c.getProviderCount() == 1 ? "1 provider" : c.getProviderCount() + " providers");
        h.itemView.setOnClickListener(v -> listener.onCategoryClick(c));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name, count;

        ViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.ivCategoryIcon);
            name = v.findViewById(R.id.tvCategoryName);
            count = v.findViewById(R.id.tvCategoryCount);
        }
    }
}
