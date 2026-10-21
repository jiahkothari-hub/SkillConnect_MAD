package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;

import java.util.List;

/** Swipeable promo banners on the customer home screen. */
public class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.ViewHolder> {

    /** One banner. Tapping it opens the provider list for {@code categoryId} / {@code skill}. */
    public static class Promo {
        public final String tag, title, subtitle, cta, categoryId, skill;
        public final int iconRes, backgroundRes;

        public Promo(String tag, String title, String subtitle, String cta, int iconRes, int backgroundRes,
                     String categoryId, String skill) {
            this.tag = tag;
            this.title = title;
            this.subtitle = subtitle;
            this.cta = cta;
            this.iconRes = iconRes;
            this.backgroundRes = backgroundRes;
            this.categoryId = categoryId;
            this.skill = skill;
        }
    }

    public interface Listener {
        void onPromoClick(Promo promo);
    }

    private final List<Promo> promos;
    private final Listener listener;

    public PromoAdapter(List<Promo> promos, Listener listener) {
        this.promos = promos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promo_banner, parent, false);
        // ViewPager2 pages must fill the pager
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Promo p = promos.get(position);
        h.itemView.setBackgroundResource(p.backgroundRes);
        h.tag.setText(p.tag);
        h.title.setText(p.title);
        h.subtitle.setText(p.subtitle);
        h.cta.setText(p.cta + "  →");
        h.icon.setImageResource(p.iconRes);
        h.itemView.setOnClickListener(v -> listener.onPromoClick(p));
    }

    @Override
    public int getItemCount() {
        return promos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tag, title, subtitle, cta;
        final ImageView icon;

        ViewHolder(View v) {
            super(v);
            tag = v.findViewById(R.id.tvPromoTag);
            title = v.findViewById(R.id.tvPromoTitle);
            subtitle = v.findViewById(R.id.tvPromoSubtitle);
            cta = v.findViewById(R.id.tvPromoCta);
            icon = v.findViewById(R.id.ivPromoIcon);
        }
    }
}
