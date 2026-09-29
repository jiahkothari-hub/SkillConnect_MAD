package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;

/** Pages of the onboarding ViewPager2. */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    public static class Page {
        final int icon;
        final int accentIcon;
        final int title;
        final int description;

        public Page(int icon, int accentIcon, int title, int description) {
            this.icon = icon;
            this.accentIcon = accentIcon;
            this.title = title;
            this.description = description;
        }
    }

    private final Page[] pages;

    public OnboardingAdapter(Page[] pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder h, int position) {
        Page p = pages[position];
        h.illustration.setImageResource(p.icon);
        h.accent.setImageResource(p.accentIcon);
        h.title.setText(p.title);
        h.description.setText(p.description);
    }

    @Override
    public int getItemCount() {
        return pages.length;
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView illustration;
        final ImageView accent;
        final TextView title;
        final TextView description;

        PageHolder(View v) {
            super(v);
            illustration = v.findViewById(R.id.ivIllustration);
            accent = v.findViewById(R.id.ivAccent);
            title = v.findViewById(R.id.tvTitle);
            description = v.findViewById(R.id.tvDescription);
        }
    }
}
