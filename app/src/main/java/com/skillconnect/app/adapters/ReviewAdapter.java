package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.utils.AvatarDrawable;
import com.skillconnect.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/** Customer reviews on the provider profile. */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviews = new ArrayList<>();

    public void submit(List<Review> list) {
        reviews.clear();
        reviews.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Review r = reviews.get(position);
        h.avatar.setImageDrawable(new AvatarDrawable(r.getCustomerName()));
        h.name.setText(r.getCustomerName());
        h.time.setText(DateTimeUtils.relativeTime(r.getCreatedAt()));
        h.rating.setRating((float) r.getRating());
        boolean hasComment = r.getComment() != null && !r.getComment().trim().isEmpty();
        h.comment.setVisibility(hasComment ? View.VISIBLE : View.GONE);
        h.comment.setText(r.getComment());
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name, time, comment;
        final RatingBar rating;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.ivAvatar);
            name = v.findViewById(R.id.tvName);
            time = v.findViewById(R.id.tvTime);
            comment = v.findViewById(R.id.tvComment);
            rating = v.findViewById(R.id.ratingBar);
        }
    }
}
