package com.skillconnect.app.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.models.ChatThread;
import com.skillconnect.app.utils.AvatarDrawable;
import com.skillconnect.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/** Conversation list: other person, last message, time and unread badge. */
public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(ChatThread chat);
    }

    private final List<ChatThread> chats = new ArrayList<>();
    private final String myUserId;
    private final Listener listener;

    public ChatThreadAdapter(String myUserId, Listener listener) {
        this.myUserId = myUserId;
        this.listener = listener;
    }

    public void submit(List<ChatThread> list) {
        chats.clear();
        chats.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_thread, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ChatThread c = chats.get(position);
        String other = c.otherName(myUserId);
        boolean iAmCustomer = c.isCustomer(myUserId);
        h.avatar.setImageDrawable(new AvatarDrawable(other));
        h.name.setText(other);
        h.subtitle.setText(iAmCustomer ? c.getProviderTitle() : "Customer");
        h.subtitle.setVisibility(h.subtitle.getText() == null || h.subtitle.getText().length() == 0 ? View.GONE : View.VISIBLE);

        String last = c.getLastMessage();
        if (last == null || last.isEmpty()) last = "No messages yet. Say hi!";
        else if (myUserId.equals(c.getLastSenderId())) last = "You: " + last;
        h.lastMessage.setText(last);
        h.time.setText(c.getLastMessage() != null ? DateTimeUtils.chatListTime(c.getUpdatedAt()) : "");

        int unread = c.unreadFor(myUserId);
        h.unread.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        h.unread.setText(unread > 99 ? "99+" : String.valueOf(unread));
        h.lastMessage.setTypeface(null, unread > 0 ? Typeface.BOLD : Typeface.NORMAL);
        h.lastMessage.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                unread > 0 ? R.color.text_primary : R.color.text_secondary));
        h.card.setOnClickListener(v -> listener.onOpen(c));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView avatar;
        final TextView name, subtitle, lastMessage, time, unread;

        ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.cardChat);
            avatar = v.findViewById(R.id.ivAvatar);
            name = v.findViewById(R.id.tvName);
            subtitle = v.findViewById(R.id.tvSubtitle);
            lastMessage = v.findViewById(R.id.tvLastMessage);
            time = v.findViewById(R.id.tvTime);
            unread = v.findViewById(R.id.tvUnread);
        }
    }
}
