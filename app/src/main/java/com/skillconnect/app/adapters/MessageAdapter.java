package com.skillconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.models.Message;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat bubbles with two view types: messages I sent (right, primary colour) and
 * messages I received (left, white). A day label is shown when the date changes.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    public interface Listener {
        void onImageClick(Message message);
    }

    private final List<Message> messages = new ArrayList<>();
    private final String myUserId;
    private final Listener listener;

    public MessageAdapter(String myUserId, Listener listener) {
        this.myUserId = myUserId;
        this.listener = listener;
    }

    public void submit(List<Message> list) {
        messages.clear();
        messages.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(myUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Message m = messages.get(position);
        boolean newDay = position == 0
                || !DateTimeUtils.sameDay(messages.get(position - 1).getCreatedAt(), m.getCreatedAt());
        h.day.setVisibility(newDay ? View.VISIBLE : View.GONE);
        h.day.setText(DateTimeUtils.chatDayLabel(m.getCreatedAt()));

        boolean hasText = m.getText() != null && !m.getText().trim().isEmpty();
        h.text.setVisibility(hasText ? View.VISIBLE : View.GONE);
        h.text.setText(m.getText());
        if (m.hasImage()) {
            h.image.setVisibility(View.VISIBLE);
            ImageUtils.loadImage(h.image, m.getImageUrl());
            h.image.setOnClickListener(v -> listener.onImageClick(m));
        } else {
            h.image.setVisibility(View.GONE);
            h.image.setOnClickListener(null);
        }
        h.time.setText(DateTimeUtils.timeOfDay(m.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView day, text, time;
        final ImageView image;

        ViewHolder(View v) {
            super(v);
            day = v.findViewById(R.id.tvDay);
            text = v.findViewById(R.id.tvText);
            time = v.findViewById(R.id.tvTime);
            image = v.findViewById(R.id.ivImage);
        }
    }
}
