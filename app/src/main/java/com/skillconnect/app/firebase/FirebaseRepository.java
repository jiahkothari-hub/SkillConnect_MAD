package com.skillconnect.app.firebase;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.DemoData;
import com.skillconnect.app.data.Subscription;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FIREBASE MODE backend: Firebase Authentication + Cloud Firestore + Firebase Storage.
 * This is the real client–server implementation used in production.
 */
public class FirebaseRepository implements AppRepository {

    private final FirebaseAuthHelper authHelper = new FirebaseAuthHelper();
    private final FirestoreHelper firestore = new FirestoreHelper();
    private final StorageHelper storage = new StorageHelper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private User cachedUser;

    // ---------------------------------------------------------------- Auth

    @Override
    public boolean isLoggedIn() {
        return authHelper.getCurrentUser() != null;
    }

    @Override
    public String getCurrentUserId() {
        return authHelper.getUid();
    }

    @Override
    public User getCachedUser() {
        return cachedUser;
    }

    @Override
    public void login(String email, String password, Callback<User> callback) {
        authHelper.login(email, password, new Callback<String>() {
            @Override
            public void onSuccess(String uid) {
                loadAndCheckUser(uid, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void loadAndCheckUser(String uid, Callback<User> callback) {
        firestore.getUser(uid, new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user == null) {
                    authHelper.logout();
                    callback.onError("Your profile could not be found. Please register again.");
                    return;
                }
                if (!user.isActive()) {
                    authHelper.logout();
                    callback.onError("This account has been deactivated. Contact support.");
                    return;
                }
                cachedUser = user;
                saveFcmToken();
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    @Override
    public void register(User user, String password, Callback<User> callback) {
        authHelper.register(user.getEmail(), password, new Callback<String>() {
            @Override
            public void onSuccess(String uid) {
                user.setUserId(uid);
                user.setCreatedAt(System.currentTimeMillis());
                user.setActive(true);
                firestore.saveUser(user, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (user.isProvider()) createProviderStub(user, callback);
                        else finishRegistration(user, callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void createProviderStub(User user, Callback<User> callback) {
        Provider p = new Provider();
        p.setProviderId(user.getUserId());
        p.setName(user.getName());
        p.setEmail(user.getEmail());
        p.setPhone(user.getPhone());
        p.setActive(true);
        p.setVerified(false);
        p.setAvailable(true);
        p.setCreatedAt(user.getCreatedAt());
        firestore.createProvider(p, new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                finishRegistration(user, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void finishRegistration(User user, Callback<User> callback) {
        cachedUser = user;
        saveFcmToken();
        callback.onSuccess(user);
    }

    /** Stores this device's FCM token so a Cloud Function can send push notifications. */
    private void saveFcmToken() {
        String uid = getCurrentUserId();
        if (uid == null) return;
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> firestore.updateUserField(uid, "fcmToken", token, null));
        } catch (Exception ignored) {
            // Messaging not available – notifications are optional.
        }
    }

    @Override
    public void sendPasswordReset(String email, Callback<Void> callback) {
        authHelper.sendPasswordReset(email, callback);
    }

    @Override
    public void loadCurrentUser(Callback<User> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Not logged in");
            return;
        }
        loadAndCheckUser(uid, callback);
    }

    @Override
    public void updateUser(User user, Callback<Void> callback) {
        firestore.saveUser(user, new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                cachedUser = user;
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    @Override
    public void logout() {
        cachedUser = null;
        authHelper.logout();
    }

    @Override
    public void getAllUsers(Callback<List<User>> callback) {
        firestore.getAllUsers(callback);
    }

    @Override
    public void setUserActive(String userId, boolean active, Callback<Void> callback) {
        firestore.updateUserField(userId, "active", active, callback);
    }

    // ---------------------------------------------------------------- Providers

    @Override
    public void getProviders(boolean includeInactive, Callback<List<Provider>> callback) {
        firestore.getProviders(includeInactive, callback);
    }

    @Override
    public void getProvider(String providerId, Callback<Provider> callback) {
        firestore.getProvider(providerId, callback);
    }

    @Override
    public void saveProviderProfile(Provider provider, Callback<Void> callback) {
        firestore.saveProviderProfile(provider, callback);
    }

    @Override
    public void setProviderActive(String providerId, boolean active, Callback<Void> callback) {
        firestore.updateProviderField(providerId, "active", active, callback);
    }

    @Override
    public void setProviderVerified(String providerId, boolean verified, Callback<Void> callback) {
        firestore.updateProviderField(providerId, "verified", verified, callback);
    }

    // ---------------------------------------------------------------- Images

    /** Compresses on a background thread, then uploads to Firebase Storage. */
    private void compressAndUpload(Context context, Uri uri, String path, Callback<String> callback) {
        executor.execute(() -> {
            try {
                byte[] bytes = ImageUtils.compressImage(context, uri);
                mainHandler.post(() -> storage.uploadImage(path, bytes, callback));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Could not read this image. Please try another photo."));
            }
        });
    }

    @Override
    public void uploadProfileImage(Context context, Uri imageUri, Callback<String> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Not logged in");
            return;
        }
        compressAndUpload(context, imageUri, StorageHelper.profileImagePath(uid), callback);
    }

    // ---------------------------------------------------------------- Portfolio

    @Override
    public void getPortfolio(String providerId, Callback<List<PortfolioItem>> callback) {
        firestore.getPortfolio(providerId, callback);
    }

    @Override
    public void addPortfolioItem(Context context, Uri imageUri, String description, Callback<PortfolioItem> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Not logged in");
            return;
        }
        String id = firestore.newPortfolioId();
        String path = StorageHelper.portfolioImagePath(uid, id);
        compressAndUpload(context, imageUri, path, new Callback<String>() {
            @Override
            public void onSuccess(String url) {
                PortfolioItem item = new PortfolioItem();
                item.setPortfolioId(id);
                item.setProviderId(uid);
                item.setImageUrl(url);
                item.setStoragePath(path);
                item.setDescription(description);
                item.setTimestamp(System.currentTimeMillis());
                firestore.addPortfolioItem(item, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    @Override
    public void deletePortfolioItem(PortfolioItem item, Callback<Void> callback) {
        firestore.deletePortfolioItem(item.getPortfolioId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                storage.delete(item.getStoragePath());
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // ---------------------------------------------------------------- Bookings

    @Override
    public void createBooking(Booking booking, Callback<Booking> callback) {
        booking.setStatus(Booking.STATUS_PENDING);
        booking.setCreatedAt(System.currentTimeMillis());
        booking.setUpdatedAt(booking.getCreatedAt());
        firestore.createBooking(booking, callback);
    }

    @Override
    public void getBooking(String bookingId, Callback<Booking> callback) {
        firestore.getBooking(bookingId, callback);
    }

    @Override
    public Subscription listenToMyBookings(Callback<List<Booking>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Not logged in");
            return () -> { };
        }
        String field = cachedUser != null && cachedUser.isProvider() ? "providerId" : "customerId";
        ListenerRegistration registration = firestore.listenToBookings(field, uid, callback);
        return registration::remove;
    }

    @Override
    public void getAllBookings(Callback<List<Booking>> callback) {
        firestore.getAllBookings(callback);
    }

    @Override
    public void updateBookingStatus(String bookingId, String newStatus, Callback<Void> callback) {
        firestore.updateBookingStatus(bookingId, newStatus, callback);
    }

    // ---------------------------------------------------------------- Reviews

    @Override
    public void getReviews(String providerId, Callback<List<Review>> callback) {
        firestore.getReviews(providerId, callback);
    }

    @Override
    public void submitReview(Review review, Callback<Void> callback) {
        firestore.submitReview(review, callback);
    }

    @Override
    public void syncFavorite(String providerId, boolean saved) {
        String uid = getCurrentUserId();
        if (uid != null) firestore.setFavorite(uid, providerId, saved);
    }

    @Override
    public void seedDemoData(Callback<Integer> callback) {
        firestore.seed(DemoData.providers(), DemoData.portfolio(), DemoData.reviews(), callback);
    }
}
