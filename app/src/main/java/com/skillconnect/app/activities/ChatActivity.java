package com.skillconnect.app.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.MessageAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.data.Subscription;
import com.skillconnect.app.models.ChatThread;
import com.skillconnect.app.models.Message;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.AvatarDrawable;
import com.skillconnect.app.utils.ImagePickerHelper;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/**
 * One-to-one chat between a customer and a provider. Messages update in real time;
 * customers can ask quick questions, share photos and jump straight to booking.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chat_id";

    /** Chat currently on screen – used to avoid notifying about messages the user is already reading. */
    private static String visibleChatId;

    public static String getVisibleChatId() {
        return visibleChatId;
    }

    private AppRepository repo;
    private User me;
    private String chatId;
    private ChatThread chat;
    private Provider provider;          // loaded when the current user is the customer
    private Subscription subscription;
    private ImagePickerHelper imagePicker;

    private MessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private View emptyChat;
    private View quickReplies;
    private StateView state;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = RepositoryProvider.get();
        me = repo.getCachedUser();
        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        if (me == null || chatId == null) {
            // Opened from a notification after the app was closed – start normally.
            Intent i = new Intent(this, SplashActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        setContentView(R.layout.activity_chat);
        imagePicker = new ImagePickerHelper(this, this::confirmSendPhoto);
        imagePicker.restoreState(savedInstanceState);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_chat);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_book) {
                openBooking();
                return true;
            } else if (item.getItemId() == R.id.action_call) {
                callProvider();
                return true;
            } else if (item.getItemId() == R.id.action_profile) {
                openProviderProfile();
                return true;
            }
            return false;
        });

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        emptyChat = findViewById(R.id.layoutEmptyChat);
        quickReplies = findViewById(R.id.scrollQuickReplies);
        state = new StateView(findViewById(R.id.chatState));

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        adapter = new MessageAdapter(me.getUserId(), this::showImage);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendText(etMessage.getText().toString()));
        findViewById(R.id.btnAttach).setOnClickListener(v -> imagePicker.showSourceDialog());
        findViewById(R.id.layoutHeader).setOnClickListener(v -> openProviderProfile());

        loadChat();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagePicker != null) imagePicker.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        visibleChatId = chatId;
        if (chat != null) repo.markChatRead(chat);
    }

    @Override
    protected void onPause() {
        if (chatId != null && chatId.equals(visibleChatId)) visibleChatId = null;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (subscription != null) subscription.remove();
        super.onDestroy();
    }

    // ------------------------------------------------------------------ loading

    private void loadChat() {
        state.showLoading(getString(R.string.loading));
        repo.getChat(chatId, new Callback<ChatThread>() {
            @Override
            public void onSuccess(ChatThread result) {
                chat = result;
                state.hide();
                bindHeader();
                listenForMessages();
                repo.markChatRead(chat);
            }

            @Override
            public void onError(String message) {
                state.showError(message, ChatActivity.this::loadChat);
            }
        });
    }

    private void bindHeader() {
        boolean iAmCustomer = chat.isCustomer(me.getUserId());
        String otherName = chat.otherName(me.getUserId());
        ((TextView) findViewById(R.id.tvName)).setText(otherName);
        ((TextView) findViewById(R.id.tvSubtitle)).setText(iAmCustomer
                ? (chat.getProviderTitle() != null ? chat.getProviderTitle() : "Provider") : "Customer");
        ((ImageView) findViewById(R.id.ivAvatar)).setImageDrawable(new AvatarDrawable(otherName));
        ((TextView) findViewById(R.id.tvEmptyTitle)).setText("Start a conversation with " + otherName.split(" ")[0]);

        // Booking, calling and profile actions make sense for the customer side
        toolbar.getMenu().findItem(R.id.action_book).setVisible(iAmCustomer);
        toolbar.getMenu().findItem(R.id.action_call).setVisible(false);
        toolbar.getMenu().findItem(R.id.action_profile).setVisible(iAmCustomer);

        if (iAmCustomer) {
            setupQuickReplies();
            repo.getProvider(chat.getProviderId(), new Callback<Provider>() {
                @Override
                public void onSuccess(Provider p) {
                    provider = p;
                    ImageUtils.loadAvatar(findViewById(R.id.ivAvatar), p.getProfileImage(), p.getName());
                    boolean hasPhone = p.getPhone() != null && !p.getPhone().isEmpty();
                    toolbar.getMenu().findItem(R.id.action_call).setVisible(hasPhone);
                }

                @Override
                public void onError(String message) {
                    // Keep the initials avatar
                }
            });
        }
    }

    private void setupQuickReplies() {
        ChipGroup group = findViewById(R.id.chipsQuickReplies);
        group.removeAllViews();
        int[] questions = {R.string.qr_available, R.string.qr_price, R.string.qr_photos};
        for (int q : questions) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, group, false);
            chip.setText(q);
            chip.setOnClickListener(v -> sendText(getString(q)));
            group.addView(chip);
        }
    }

    private void listenForMessages() {
        subscription = repo.listenToMessages(chatId, new Callback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                adapter.submit(messages);
                boolean empty = messages.isEmpty();
                emptyChat.setVisibility(empty ? View.VISIBLE : View.GONE);
                boolean iAmCustomer = chat.isCustomer(me.getUserId());
                quickReplies.setVisibility(iAmCustomer && messages.size() < 2 ? View.VISIBLE : View.GONE);
                if (!empty) rvMessages.scrollToPosition(messages.size() - 1);
                // New messages arriving while the chat is open are already read
                if (chatId.equals(visibleChatId)) repo.markChatRead(chat);
            }

            @Override
            public void onError(String message) {
                UiUtils.toast(ChatActivity.this, message);
            }
        });
    }

    // ------------------------------------------------------------------ sending

    private void sendText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || chat == null) return;
        etMessage.setText("");
        repo.sendMessage(chat, text, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // The real-time listener shows the new message
            }

            @Override
            public void onError(String message) {
                UiUtils.toast(ChatActivity.this, message);
                etMessage.setText(text);
                etMessage.setSelection(text.length());
            }
        });
    }

    /** Preview the photo before sending it. */
    private void confirmSendPhoto(Uri uri) {
        View view = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageUtils.loadImage(view.findViewById(R.id.ivPreview), uri);
        ((TextView) view.findViewById(R.id.tvPreviewCaption)).setText("Send this photo to " + chat.otherName(me.getUserId()) + "?");
        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send, (d, w) -> sendPhoto(uri))
                .show();
    }

    private void sendPhoto(Uri uri) {
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        UiUtils.toast(this, "Sending photo…");
        repo.sendImageMessage(this, chat, uri, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Shown by the listener
            }

            @Override
            public void onError(String message) {
                UiUtils.toast(ChatActivity.this, message);
            }
        });
    }

    private void showImage(Message message) {
        View view = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageUtils.loadImage(view.findViewById(R.id.ivPreview), message.getImageUrl());
        ((TextView) view.findViewById(R.id.tvPreviewCaption)).setText("Sent by " + message.getSenderName());
        new MaterialAlertDialogBuilder(this).setView(view).setPositiveButton(R.string.close, null).show();
    }

    // ------------------------------------------------------------------ actions

    private void openBooking() {
        if (chat == null) return;
        Intent i = new Intent(this, BookingActivity.class);
        i.putExtra(BookingActivity.EXTRA_PROVIDER_ID, chat.getProviderId());
        startActivity(i);
    }

    private void openProviderProfile() {
        if (chat == null || !chat.isCustomer(me.getUserId())) return;
        Intent i = new Intent(this, ProviderDetailsActivity.class);
        i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, chat.getProviderId());
        startActivity(i);
    }

    private void callProvider() {
        if (provider == null || provider.getPhone() == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + provider.getPhone())));
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(this, "No phone app found");
        }
    }
}
