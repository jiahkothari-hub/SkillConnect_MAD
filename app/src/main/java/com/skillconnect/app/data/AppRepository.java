package com.skillconnect.app.data;

import android.content.Context;
import android.net.Uri;

import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.ChatThread;
import com.skillconnect.app.models.Message;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;

import java.util.List;

/**
 * Everything the UI needs from the backend. There are two implementations:
 * <ul>
 *   <li>{@link com.skillconnect.app.firebase.FirebaseRepository} – the real cloud backend
 *   (Firebase Authentication + Cloud Firestore + Firebase Storage).</li>
 *   <li>{@link DemoRepository} – local sample data so the UI can be explored before
 *   Firebase is configured. Nothing leaves the device.</li>
 * </ul>
 * Screens get the active implementation from {@link RepositoryProvider#get()}.
 */
public interface AppRepository {

    /** Error message used when a provider document doesn't exist (e.g. a provider who hasn't finished setup). */
    String PROVIDER_NOT_FOUND = "Provider not found";

    // ----- Authentication & users -----
    boolean isLoggedIn();

    String getCurrentUserId();

    /** The logged-in user loaded at login/splash (may be null before loading). */
    User getCachedUser();

    void login(String email, String password, Callback<User> callback);

    void register(User user, String password, Callback<User> callback);

    void sendPasswordReset(String email, Callback<Void> callback);

    void loadCurrentUser(Callback<User> callback);

    void updateUser(User user, Callback<Void> callback);

    void logout();

    void getAllUsers(Callback<List<User>> callback);

    void setUserActive(String userId, boolean active, Callback<Void> callback);

    // ----- Providers -----
    void getProviders(boolean includeInactive, Callback<List<Provider>> callback);

    void getProvider(String providerId, Callback<Provider> callback);

    void saveProviderProfile(Provider provider, Callback<Void> callback);

    void setProviderActive(String providerId, boolean active, Callback<Void> callback);

    void setProviderVerified(String providerId, boolean verified, Callback<Void> callback);

    // ----- Images -----
    /** Compresses and uploads a profile photo; returns its download URL. */
    void uploadProfileImage(Context context, Uri imageUri, Callback<String> callback);

    // ----- Portfolio -----
    void getPortfolio(String providerId, Callback<List<PortfolioItem>> callback);

    void addPortfolioItem(Context context, Uri imageUri, String description, Callback<PortfolioItem> callback);

    void deletePortfolioItem(PortfolioItem item, Callback<Void> callback);

    // ----- Bookings -----
    void createBooking(Booking booking, Callback<Booking> callback);

    void getBooking(String bookingId, Callback<Booking> callback);

    /** Real-time list of the current user's bookings (as customer or provider). */
    Subscription listenToMyBookings(Callback<List<Booking>> callback);

    void getAllBookings(Callback<List<Booking>> callback);

    void updateBookingStatus(String bookingId, String newStatus, Callback<Void> callback);

    // ----- Reviews -----
    void getReviews(String providerId, Callback<List<Review>> callback);

    /** Saves the review, marks the booking reviewed and updates the provider's rating. */
    void submitReview(Review review, Callback<Void> callback);

    // ----- Chat -----
    /**
     * Returns the conversation between this customer and provider, creating it if it doesn't exist.
     * Either participant can open it (e.g. a provider from a booking).
     */
    void openChat(String customerId, String customerName, String providerId, String providerName,
                  String providerTitle, Callback<ChatThread> callback);

    void getChat(String chatId, Callback<ChatThread> callback);

    /** Real-time list of the current user's conversations, newest first. */
    Subscription listenToMyChats(Callback<List<ChatThread>> callback);

    /** Real-time messages of one conversation, oldest first. */
    Subscription listenToMessages(String chatId, Callback<List<Message>> callback);

    void sendMessage(ChatThread chat, String text, Callback<Void> callback);

    /** Compresses and uploads a photo, then sends it as a message. */
    void sendImageMessage(Context context, ChatThread chat, Uri imageUri, Callback<Void> callback);

    /** Clears the current user's unread counter for this chat. */
    void markChatRead(ChatThread chat);

    // ----- Favorites (cloud backup of the local SQLite favorites) -----
    void syncFavorite(String providerId, boolean saved);

    // ----- Admin -----
    /** Uploads the sample providers/categories to the backend. Returns the number of providers written. */
    void seedDemoData(Callback<Integer> callback);
}
