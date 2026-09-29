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

import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/** Generic row adapter for the admin panel (providers, users, bookings, categories). */
public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    /** One row in the admin list. */
    public static class Row {
        public String title;
        public String subtitle;
        public String meta;
        public int metaTextColor = R.color.text_secondary;
        public int metaBgColor = R.color.surface_variant;
        public String avatarName;
        public String avatarUrl;
        public String action;
        public String action2;
        public Object payload;
    }

    public interface Listener {
        void onRowClick(Row row);

        void onAction(Row row);

        void onAction2(Row row);
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public AdminAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Row> list) {
        rows.clear();
        rows.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row r = rows.get(position);
        ImageUtils.loadAvatar(h.avatar, r.avatarUrl, r.avatarName != null ? r.avatarName : r.title);
        h.title.setText(r.title);
        h.subtitle.setText(r.subtitle);
        h.meta.setVisibility(r.meta != null ? View.VISIBLE : View.GONE);
        h.meta.setText(r.meta);
        h.meta.setTextColor(ContextCompat.getColor(h.itemView.getContext(), r.metaTextColor));
        h.meta.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), r.metaBgColor)));
        h.actions.setVisibility(r.action != null || r.action2 != null ? View.VISIBLE : View.GONE);
        h.action.setVisibility(r.action != null ? View.VISIBLE : View.GONE);
        h.action.setText(r.action);
        h.action2.setVisibility(r.action2 != null ? View.VISIBLE : View.GONE);
        h.action2.setText(r.action2);
        h.card.setOnClickListener(v -> listener.onRowClick(r));
        h.action.setOnClickListener(v -> listener.onAction(r));
        h.action2.setOnClickListener(v -> listener.onAction2(r));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card, actions;
        final ImageView avatar;
        final TextView title, subtitle, meta;
        final MaterialButton action, action2;

        ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.cardAdmin);
            actions = v.findViewById(R.id.layoutActions);
            avatar = v.findViewById(R.id.ivAvatar);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            meta = v.findViewById(R.id.tvMeta);
            action = v.findViewById(R.id.btnAction);
            action2 = v.findViewById(R.id.btnAction2);
        }
    }
}
