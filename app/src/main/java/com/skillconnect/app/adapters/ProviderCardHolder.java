package com.skillconnect.app.adapters;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.UiUtils;

/**
 * Holds and binds the views of item_provider_card.xml. Shared by the RecyclerView
 * adapter (ProviderAdapter) and the ListView adapter (ProviderListAdapter).
 */
public class ProviderCardHolder {

    public interface Listener {
        void onOpenProvider(Provider provider);

        void onToggleFavorite(Provider provider);
    }

    final View root;
    final ImageView avatar;
    final View availability;
    final TextView name;
    final ImageView verified;
    final TextView skill;
    final TextView rating;
    final TextView reviews;
    final ImageButton favorite;
    final TextView price;
    final TextView distance;
    final MaterialButton viewProfile;

    public ProviderCardHolder(View v) {
        root = v;
        avatar = v.findViewById(R.id.ivAvatar);
        availability = v.findViewById(R.id.viewAvailability);
        name = v.findViewById(R.id.tvName);
        verified = v.findViewById(R.id.ivVerified);
        skill = v.findViewById(R.id.tvSkill);
        rating = v.findViewById(R.id.tvRating);
        reviews = v.findViewById(R.id.tvReviews);
        favorite = v.findViewById(R.id.btnFavorite);
        price = v.findViewById(R.id.tvPrice);
        distance = v.findViewById(R.id.tvDistance);
        viewProfile = v.findViewById(R.id.btnViewProfile);
    }

    public void bind(Provider p, boolean isFavorite, Listener listener) {
        ImageUtils.loadAvatar(avatar, p.getProfileImage(), p.getName());
        availability.setBackgroundResource(p.isAvailable() ? R.drawable.bg_dot_available : R.drawable.bg_dot_unavailable);
        availability.setContentDescription(p.isAvailable() ? "Available" : "Busy");
        name.setText(p.getName());
        verified.setVisibility(p.isVerified() ? View.VISIBLE : View.GONE);
        skill.setText(p.getTitle());
        if (p.getReviewCount() > 0) {
            rating.setText(UiUtils.formatRating(p.getAverageRating()));
            reviews.setText("(" + p.getReviewCount() + ")");
        } else {
            rating.setText("New");
            reviews.setText("");
        }
        price.setText(UiUtils.startingPrice(p.getStartingPrice()) + " · " + p.getPriceUnit());
        if (p.getDistanceKm() >= 0) {
            distance.setVisibility(View.VISIBLE);
            distance.setText(LocationUtils.formatDistance(p.getDistanceKm()));
        } else {
            distance.setVisibility(p.getAddress() != null ? View.VISIBLE : View.GONE);
            distance.setText(p.getAddress());
        }
        favorite.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        favorite.setColorFilter(ContextCompat.getColor(root.getContext(), isFavorite ? R.color.error : R.color.text_hint));

        favorite.setOnClickListener(v -> listener.onToggleFavorite(p));
        viewProfile.setOnClickListener(v -> listener.onOpenProvider(p));
    }
}
