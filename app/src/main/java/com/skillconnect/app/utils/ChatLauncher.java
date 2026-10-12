package com.skillconnect.app.utils;

import android.app.Activity;
import android.content.Intent;

import com.skillconnect.app.activities.ChatActivity;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.ChatThread;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;

/** Opens (or creates) the right conversation from a provider profile or a booking. */
public final class ChatLauncher {

    private ChatLauncher() {
    }

    /** Customer taps "Message" on a provider's profile. */
    public static void openWithProvider(Activity activity, Provider provider) {
        AppRepository repo = RepositoryProvider.get();
        User me = repo.getCachedUser();
        if (me == null) return;
        repo.openChat(me.getUserId(), me.getName(), provider.getProviderId(), provider.getName(),
                provider.getTitle(), callback(activity));
    }

    /** Either side taps "Message" on a booking. */
    public static void openForBooking(Activity activity, Booking booking) {
        RepositoryProvider.get().openChat(booking.getCustomerId(), booking.getCustomerName(),
                booking.getProviderId(), booking.getProviderName(), booking.getService(), callback(activity));
    }

    private static Callback<ChatThread> callback(Activity activity) {
        return new Callback<ChatThread>() {
            @Override
            public void onSuccess(ChatThread chat) {
                Intent i = new Intent(activity, ChatActivity.class);
                i.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.getChatId());
                activity.startActivity(i);
            }

            @Override
            public void onError(String message) {
                UiUtils.toast(activity, message);
            }
        };
    }
}
