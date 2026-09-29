package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.utils.AvatarDrawable;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Booking cards. In CUSTOMER mode the provider is shown (with "Leave a Review" when allowed);
 * in PROVIDER mode the customer and job description are shown with Accept/Decline/Complete.
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    public static final int MODE_CUSTOMER = 0;
    public static final int MODE_PROVIDER = 1;

    public interface Listener {
        void onOpen(Booking booking);

        /** Accept / Mark completed / Leave review */
        void onPrimaryAction(Booking booking);

        /** Decline */
        void onSecondaryAction(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final int mode;
    private final Listener listener;

    public BookingAdapter(int mode, Listener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    public void submit(List<Booking> list) {
        bookings.clear();
        bookings.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Booking b = bookings.get(position);
        boolean providerMode = mode == MODE_PROVIDER;
        String otherName = providerMode ? b.getCustomerName() : b.getProviderName();
        h.avatar.setImageDrawable(new AvatarDrawable(otherName));
        h.name.setText(otherName);
        h.service.setText(b.getService());
        UiUtils.styleStatusPill(h.status, b.getStatus());
        h.date.setText(DateTimeUtils.displayDate(b.getScheduledAt()));
        h.time.setText(DateTimeUtils.to12Hour(b.getTime()));
        h.description.setVisibility(providerMode ? View.VISIBLE : View.GONE);
        h.description.setText(b.getDescription());

        String primary = null;
        String secondary = null;
        if (providerMode) {
            if (Booking.STATUS_PENDING.equals(b.getStatus())) {
                primary = h.itemView.getContext().getString(R.string.accept);
                secondary = h.itemView.getContext().getString(R.string.decline);
            } else if (Booking.STATUS_ACCEPTED.equals(b.getStatus())) {
                primary = h.itemView.getContext().getString(R.string.mark_completed);
            }
        } else if (b.canBeReviewed()) {
            primary = h.itemView.getContext().getString(R.string.leave_review);
        }
        h.actions.setVisibility(primary != null ? View.VISIBLE : View.GONE);
        h.primary.setVisibility(primary != null ? View.VISIBLE : View.GONE);
        h.primary.setText(primary);
        h.secondary.setVisibility(secondary != null ? View.VISIBLE : View.GONE);
        h.secondary.setText(secondary);

        h.primary.setOnClickListener(v -> listener.onPrimaryAction(b));
        h.secondary.setOnClickListener(v -> listener.onSecondaryAction(b));
        h.card.setOnClickListener(v -> listener.onOpen(b));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView avatar;
        final TextView name, service, status, date, time, description;
        final View actions;
        final MaterialButton primary, secondary;

        ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.cardBooking);
            avatar = v.findViewById(R.id.ivAvatar);
            name = v.findViewById(R.id.tvName);
            service = v.findViewById(R.id.tvService);
            status = v.findViewById(R.id.tvStatus);
            date = v.findViewById(R.id.tvDate);
            time = v.findViewById(R.id.tvTime);
            description = v.findViewById(R.id.tvDescription);
            actions = v.findViewById(R.id.layoutActions);
            primary = v.findViewById(R.id.btnPrimaryAction);
            secondary = v.findViewById(R.id.btnSecondaryAction);
        }
    }
}
