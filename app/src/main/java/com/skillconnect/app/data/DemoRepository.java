package com.skillconnect.app.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.PrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DEMO MODE backend. Keeps sample data in memory so every screen can be explored
 * without Firebase. Data resets when the app process is restarted.
 * A short artificial delay is added so loading states are visible, like a real network.
 */
public class DemoRepository implements AppRepository {

    private static final long FAKE_NETWORK_DELAY_MS = 350;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<User> users = new ArrayList<>();
    private final List<Provider> providers = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<PortfolioItem> portfolio = new ArrayList<>();
    private final List<Callback<List<Booking>>> bookingListeners = new ArrayList<>();

    private User currentUser;

    public DemoRepository(Context context) {
        this.context = context.getApplicationContext();
        users.addAll(DemoData.users());
        providers.addAll(DemoData.providers());
        bookings.addAll(DemoData.bookings());
        reviews.addAll(DemoData.reviews());
        portfolio.addAll(DemoData.portfolio());
        String savedId = PrefsManager.get(context).getDemoUserId();
        if (savedId != null) currentUser = findUser(savedId);
    }

    private <T> void deliver(Callback<T> cb, T result) {
        handler.postDelayed(() -> cb.onSuccess(result), FAKE_NETWORK_DELAY_MS);
    }

    private <T> void fail(Callback<T> cb, String message) {
        handler.postDelayed(() -> cb.onError(message), FAKE_NETWORK_DELAY_MS);
    }

    private User findUser(String id) {
        for (User u : users) if (u.getUserId().equals(id)) return u;
        return null;
    }

    private Provider findProvider(String id) {
        for (Provider p : providers) if (p.getProviderId().equals(id)) return p;
        return null;
    }

    private Booking findBooking(String id) {
        for (Booking b : bookings) if (b.getBookingId().equals(id)) return b;
        return null;
    }

    // ---------------------------------------------------------------- Auth

    @Override
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    @Override
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    @Override
    public User getCachedUser() {
        return currentUser;
    }

    @Override
    public void login(String email, String password, Callback<User> callback) {
        for (User u : users) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim())) {
                if (!DemoData.DEMO_PASSWORD.equals(password)) {
                    fail(callback, "Incorrect password. Demo accounts use \"" + DemoData.DEMO_PASSWORD + "\".");
                    return;
                }
                if (!u.isActive()) {
                    fail(callback, "This account has been deactivated.");
                    return;
                }
                currentUser = u;
                PrefsManager.get(context).setDemoUserId(u.getUserId());
                deliver(callback, u);
                return;
            }
        }
        fail(callback, "No demo account found for this email. Try " + DemoData.CUSTOMER_EMAIL + " or register.");
    }

    @Override
    public void register(User user, String password, Callback<User> callback) {
        for (User u : users) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(user.getEmail())) {
                fail(callback, "An account with this email already exists.");
                return;
            }
        }
        user.setUserId("demo_" + UUID.randomUUID().toString().substring(0, 8));
        user.setCreatedAt(System.currentTimeMillis());
        users.add(user);
        if (user.isProvider()) {
            Provider p = new Provider();
            p.setProviderId(user.getUserId());
            p.setName(user.getName());
            p.setEmail(user.getEmail());
            p.setPhone(user.getPhone());
            p.setCreatedAt(user.getCreatedAt());
            providers.add(p);
        }
        currentUser = user;
        PrefsManager.get(context).setDemoUserId(user.getUserId());
        deliver(callback, user);
    }

    @Override
    public void sendPasswordReset(String email, Callback<Void> callback) {
        fail(callback, "Password reset emails need Firebase. In demo mode every password is \"" + DemoData.DEMO_PASSWORD + "\".");
    }

    @Override
    public void loadCurrentUser(Callback<User> callback) {
        if (currentUser == null) fail(callback, "Not logged in");
        else deliver(callback, currentUser);
    }

    @Override
    public void updateUser(User user, Callback<Void> callback) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(user.getUserId())) users.set(i, user);
        }
        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) currentUser = user;
        deliver(callback, null);
    }

    @Override
    public void logout() {
        currentUser = null;
        PrefsManager.get(context).setDemoUserId(null);
    }

    @Override
    public void getAllUsers(Callback<List<User>> callback) {
        deliver(callback, new ArrayList<>(users));
    }

    @Override
    public void setUserActive(String userId, boolean active, Callback<Void> callback) {
        User u = findUser(userId);
        if (u != null) u.setActive(active);
        deliver(callback, null);
    }

    // ---------------------------------------------------------------- Providers

    @Override
    public void getProviders(boolean includeInactive, Callback<List<Provider>> callback) {
        List<Provider> result = new ArrayList<>();
        for (Provider p : providers) {
            // Newly registered providers without a title are not listed until they complete their profile
            boolean listed = p.getTitle() != null && !p.getTitle().isEmpty();
            if (includeInactive || (p.isActive() && listed)) result.add(p);
        }
        deliver(callback, result);
    }

    @Override
    public void getProvider(String providerId, Callback<Provider> callback) {
        Provider p = findProvider(providerId);
        if (p == null) fail(callback, "Provider not found");
        else deliver(callback, p);
    }

    @Override
    public void saveProviderProfile(Provider provider, Callback<Void> callback) {
        Provider existing = findProvider(provider.getProviderId());
        if (existing != null) {
            // keep fields the provider cannot change themselves
            provider.setAverageRating(existing.getAverageRating());
            provider.setReviewCount(existing.getReviewCount());
            provider.setVerified(existing.isVerified());
            provider.setActive(existing.isActive());
            providers.set(providers.indexOf(existing), provider);
        } else {
            providers.add(provider);
        }
        deliver(callback, null);
    }

    @Override
    public void setProviderActive(String providerId, boolean active, Callback<Void> callback) {
        Provider p = findProvider(providerId);
        if (p != null) p.setActive(active);
        deliver(callback, null);
    }

    @Override
    public void setProviderVerified(String providerId, boolean verified, Callback<Void> callback) {
        Provider p = findProvider(providerId);
        if (p != null) p.setVerified(verified);
        deliver(callback, null);
    }

    // ---------------------------------------------------------------- Images

    /** In demo mode images are compressed and kept in the app's private storage. */
    private void saveImageLocally(Uri uri, Callback<String> callback) {
        executor.execute(() -> {
            try {
                byte[] bytes = ImageUtils.compressImage(context, uri);
                File dir = new File(context.getFilesDir(), "demo_images");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create folder");
                File file = new File(dir, UUID.randomUUID() + ".jpg");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(bytes);
                }
                String result = Uri.fromFile(file).toString();
                handler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                handler.post(() -> callback.onError("Could not process the image. Please try another photo."));
            }
        });
    }

    @Override
    public void uploadProfileImage(Context ctx, Uri imageUri, Callback<String> callback) {
        saveImageLocally(imageUri, callback);
    }

    // ---------------------------------------------------------------- Portfolio

    @Override
    public void getPortfolio(String providerId, Callback<List<PortfolioItem>> callback) {
        List<PortfolioItem> result = new ArrayList<>();
        for (PortfolioItem item : portfolio) if (item.getProviderId().equals(providerId)) result.add(item);
        Collections.sort(result, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        deliver(callback, result);
    }

    @Override
    public void addPortfolioItem(Context ctx, Uri imageUri, String description, Callback<PortfolioItem> callback) {
        saveImageLocally(imageUri, new Callback<String>() {
            @Override
            public void onSuccess(String url) {
                PortfolioItem item = new PortfolioItem();
                item.setPortfolioId(UUID.randomUUID().toString());
                item.setProviderId(getCurrentUserId());
                item.setImageUrl(url);
                item.setDescription(description);
                item.setTimestamp(System.currentTimeMillis());
                portfolio.add(item);
                callback.onSuccess(item);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    @Override
    public void deletePortfolioItem(PortfolioItem item, Callback<Void> callback) {
        for (int i = portfolio.size() - 1; i >= 0; i--) {
            if (portfolio.get(i).getPortfolioId().equals(item.getPortfolioId())) portfolio.remove(i);
        }
        deliver(callback, null);
    }

    // ---------------------------------------------------------------- Bookings

    @Override
    public void createBooking(Booking booking, Callback<Booking> callback) {
        booking.setBookingId("bk_" + UUID.randomUUID().toString().substring(0, 8));
        booking.setCreatedAt(System.currentTimeMillis());
        booking.setUpdatedAt(booking.getCreatedAt());
        booking.setStatus(Booking.STATUS_PENDING);
        bookings.add(booking);
        deliver(callback, booking);
        notifyBookingListeners();
    }

    @Override
    public void getBooking(String bookingId, Callback<Booking> callback) {
        Booking b = findBooking(bookingId);
        if (b == null) fail(callback, "Booking not found");
        else deliver(callback, b);
    }

    private List<Booking> myBookings() {
        List<Booking> result = new ArrayList<>();
        String uid = getCurrentUserId();
        if (uid == null) return result;
        for (Booking b : bookings) {
            if (uid.equals(b.getCustomerId()) || uid.equals(b.getProviderId())) result.add(b);
        }
        return result;
    }

    private void notifyBookingListeners() {
        List<Callback<List<Booking>>> copy = new ArrayList<>(bookingListeners);
        for (Callback<List<Booking>> l : copy) deliver(l, myBookings());
    }

    @Override
    public Subscription listenToMyBookings(Callback<List<Booking>> callback) {
        bookingListeners.add(callback);
        deliver(callback, myBookings());
        return () -> bookingListeners.remove(callback);
    }

    @Override
    public void getAllBookings(Callback<List<Booking>> callback) {
        deliver(callback, new ArrayList<>(bookings));
    }

    @Override
    public void updateBookingStatus(String bookingId, String newStatus, Callback<Void> callback) {
        Booking b = findBooking(bookingId);
        if (b == null) {
            fail(callback, "Booking not found");
            return;
        }
        b.setStatus(newStatus);
        b.setUpdatedAt(System.currentTimeMillis());
        deliver(callback, null);
        notifyBookingListeners();
    }

    // ---------------------------------------------------------------- Reviews

    @Override
    public void getReviews(String providerId, Callback<List<Review>> callback) {
        List<Review> result = new ArrayList<>();
        for (Review r : reviews) if (r.getProviderId().equals(providerId)) result.add(r);
        Collections.sort(result, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        deliver(callback, result);
    }

    @Override
    public void submitReview(Review review, Callback<Void> callback) {
        Booking booking = findBooking(review.getBookingId());
        if (booking == null || !Booking.STATUS_COMPLETED.equals(booking.getStatus())) {
            fail(callback, "Only completed bookings can be reviewed.");
            return;
        }
        if (booking.isReviewed()) {
            fail(callback, "You have already reviewed this booking.");
            return;
        }
        review.setReviewId(booking.getBookingId());
        review.setCreatedAt(System.currentTimeMillis());
        reviews.add(review);
        booking.setReviewed(true);
        Provider p = findProvider(review.getProviderId());
        if (p != null) {
            double total = p.getAverageRating() * p.getReviewCount() + review.getRating();
            p.setReviewCount(p.getReviewCount() + 1);
            p.setAverageRating(Math.round(total / p.getReviewCount() * 10.0) / 10.0);
        }
        deliver(callback, null);
        notifyBookingListeners();
    }

    @Override
    public void syncFavorite(String providerId, boolean saved) {
        // Demo mode: favorites live only in the local SQLite database.
    }

    @Override
    public void seedDemoData(Callback<Integer> callback) {
        fail(callback, "Demo data is already loaded. Seeding is only needed for Firebase.");
    }
}
