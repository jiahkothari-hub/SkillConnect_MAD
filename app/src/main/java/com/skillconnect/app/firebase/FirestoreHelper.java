package com.skillconnect.app.firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All Cloud Firestore reads and writes.
 *
 * Collections:
 * users/{userId}, providers/{providerId}, bookings/{bookingId}, reviews/{reviewId},
 * portfolio/{portfolioId}, favorites/{userId_providerId}, categories/{categoryId}
 *
 * Queries use a single equality filter and are sorted on the device, so no
 * composite indexes need to be created in the Firebase console.
 */
public class FirestoreHelper {

    public static final String USERS = "users";
    public static final String PROVIDERS = "providers";
    public static final String BOOKINGS = "bookings";
    public static final String REVIEWS = "reviews";
    public static final String PORTFOLIO = "portfolio";
    public static final String FAVORITES = "favorites";
    public static final String CATEGORIES = "categories";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Keeps a local cache so previously loaded data is visible offline. */
    public static void enableOfflinePersistence() {
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (IllegalStateException ignored) {
            // Firestore was already used; settings can only be applied once.
        }
    }

    // ------------------------------------------------------------------ users

    public void getUser(String userId, Callback<User> callback) {
        db.collection(USERS).document(userId).get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists() ? doc.toObject(User.class) : null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void saveUser(User user, Callback<Void> callback) {
        db.collection(USERS).document(user.getUserId()).set(user)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void updateUserField(String userId, String field, Object value, Callback<Void> callback) {
        db.collection(USERS).document(userId).update(field, value)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(FirebaseErrors.message(e));
                });
    }

    public void getAllUsers(Callback<List<User>> callback) {
        db.collection(USERS).get()
                .addOnSuccessListener(snap -> {
                    List<User> users = snap.toObjects(User.class);
                    Collections.sort(users, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    // -------------------------------------------------------------- providers

    public void getProviders(boolean includeInactive, Callback<List<Provider>> callback) {
        com.google.firebase.firestore.Query query = db.collection(PROVIDERS);
        if (!includeInactive) query = query.whereEqualTo("active", true);
        query.get()
                .addOnSuccessListener(snap -> {
                    List<Provider> result = new ArrayList<>();
                    for (Provider p : snap.toObjects(Provider.class)) {
                        boolean listed = p.getTitle() != null && !p.getTitle().trim().isEmpty();
                        if (includeInactive || listed) result.add(p);
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void getProvider(String providerId, Callback<Provider> callback) {
        db.collection(PROVIDERS).document(providerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onSuccess(doc.toObject(Provider.class));
                    else callback.onError("Provider not found");
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    /** Creates the initial provider document right after a provider registers. */
    public void createProvider(Provider provider, Callback<Void> callback) {
        db.collection(PROVIDERS).document(provider.getProviderId()).set(provider)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    /**
     * Saves only the fields a provider is allowed to edit. Rating, review count,
     * verification and active status are protected by the security rules.
     */
    public void saveProviderProfile(Provider p, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("providerId", p.getProviderId());
        data.put("name", p.getName());
        data.put("title", p.getTitle());
        data.put("category", p.getCategory());
        data.put("skills", p.getSkills());
        data.put("experienceYears", p.getExperienceYears());
        data.put("description", p.getDescription());
        data.put("startingPrice", p.getStartingPrice());
        data.put("priceUnit", p.getPriceUnit());
        data.put("latitude", p.getLatitude());
        data.put("longitude", p.getLongitude());
        data.put("address", p.getAddress());
        data.put("availableDays", p.getAvailableDays());
        data.put("availableFrom", p.getAvailableFrom());
        data.put("availableTo", p.getAvailableTo());
        data.put("available", p.isAvailable());
        data.put("phone", p.getPhone());
        data.put("email", p.getEmail());
        data.put("profileImage", p.getProfileImage());
        db.collection(PROVIDERS).document(p.getProviderId()).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void updateProviderField(String providerId, String field, Object value, Callback<Void> callback) {
        db.collection(PROVIDERS).document(providerId).update(field, value)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    // -------------------------------------------------------------- portfolio

    public String newPortfolioId() {
        return db.collection(PORTFOLIO).document().getId();
    }

    public void getPortfolio(String providerId, Callback<List<PortfolioItem>> callback) {
        db.collection(PORTFOLIO).whereEqualTo("providerId", providerId).get()
                .addOnSuccessListener(snap -> {
                    List<PortfolioItem> items = snap.toObjects(PortfolioItem.class);
                    Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void addPortfolioItem(PortfolioItem item, Callback<PortfolioItem> callback) {
        db.collection(PORTFOLIO).document(item.getPortfolioId()).set(item)
                .addOnSuccessListener(unused -> callback.onSuccess(item))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void deletePortfolioItem(String portfolioId, Callback<Void> callback) {
        db.collection(PORTFOLIO).document(portfolioId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    // --------------------------------------------------------------- bookings

    public void createBooking(Booking booking, Callback<Booking> callback) {
        DocumentReference ref = db.collection(BOOKINGS).document();
        booking.setBookingId(ref.getId());
        ref.set(booking)
                .addOnSuccessListener(unused -> callback.onSuccess(booking))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void getBooking(String bookingId, Callback<Booking> callback) {
        db.collection(BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onSuccess(doc.toObject(Booking.class));
                    else callback.onError("Booking not found");
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    /** Real-time listener on bookings where {field} (customerId or providerId) == userId. */
    public ListenerRegistration listenToBookings(String field, String userId, Callback<List<Booking>> callback) {
        return db.collection(BOOKINGS).whereEqualTo(field, userId)
                .addSnapshotListener((QuerySnapshot snap, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        callback.onError(FirebaseErrors.message(e));
                        return;
                    }
                    if (snap != null) callback.onSuccess(snap.toObjects(Booking.class));
                });
    }

    public void getAllBookings(Callback<List<Booking>> callback) {
        db.collection(BOOKINGS).get()
                .addOnSuccessListener(snap -> callback.onSuccess(snap.toObjects(Booking.class)))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void updateBookingStatus(String bookingId, String status, Callback<Void> callback) {
        db.collection(BOOKINGS).document(bookingId)
                .update("status", status, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    // ---------------------------------------------------------------- reviews

    public void getReviews(String providerId, Callback<List<Review>> callback) {
        db.collection(REVIEWS).whereEqualTo("providerId", providerId).get()
                .addOnSuccessListener(snap -> {
                    List<Review> reviews = snap.toObjects(Review.class);
                    Collections.sort(reviews, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    /**
     * Uses a transaction so the review, the booking's "reviewed" flag and the
     * provider's average rating are always updated together.
     */
    public void submitReview(Review review, Callback<Void> callback) {
        DocumentReference bookingRef = db.collection(BOOKINGS).document(review.getBookingId());
        DocumentReference providerRef = db.collection(PROVIDERS).document(review.getProviderId());
        DocumentReference reviewRef = db.collection(REVIEWS).document(review.getBookingId());
        review.setReviewId(review.getBookingId());
        review.setCreatedAt(System.currentTimeMillis());

        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot booking = tx.get(bookingRef);
            DocumentSnapshot provider = tx.get(providerRef);
            if (!Booking.STATUS_COMPLETED.equals(booking.getString("status"))) {
                throw new FirebaseFirestoreException("Only completed bookings can be reviewed.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (Boolean.TRUE.equals(booking.getBoolean("reviewed"))) {
                throw new FirebaseFirestoreException("You have already reviewed this booking.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Double avg = provider.getDouble("averageRating");
            Long count = provider.getLong("reviewCount");
            double oldAvg = avg != null ? avg : 0;
            long oldCount = count != null ? count : 0;
            long newCount = oldCount + 1;
            double newAvg = Math.round(((oldAvg * oldCount) + review.getRating()) / newCount * 10.0) / 10.0;

            tx.set(reviewRef, review);
            tx.update(bookingRef, "reviewed", true, "updatedAt", System.currentTimeMillis());
            tx.update(providerRef, "averageRating", newAvg, "reviewCount", newCount);
            return null;
        })
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    // -------------------------------------------------------------- favorites

    public void setFavorite(String userId, String providerId, boolean saved) {
        DocumentReference ref = db.collection(FAVORITES).document(userId + "_" + providerId);
        if (saved) {
            Map<String, Object> data = new HashMap<>();
            data.put("customerId", userId);
            data.put("providerId", providerId);
            data.put("createdAt", System.currentTimeMillis());
            ref.set(data);
        } else {
            ref.delete();
        }
    }

    // ------------------------------------------------------------------ admin

    public void seed(List<Provider> providers, List<PortfolioItem> portfolio, List<Review> reviews,
                     Callback<Integer> callback) {
        WriteBatch batch = db.batch();
        for (Provider p : providers) {
            batch.set(db.collection(PROVIDERS).document(p.getProviderId()), p);
        }
        for (PortfolioItem item : portfolio) {
            batch.set(db.collection(PORTFOLIO).document("seed_" + item.getPortfolioId()), item);
        }
        for (Review r : reviews) {
            batch.set(db.collection(REVIEWS).document("seed_" + r.getReviewId()), r);
        }
        int order = 0;
        for (Category c : CategoryData.getMainCategories()) {
            Map<String, Object> data = new HashMap<>();
            data.put("categoryId", c.getId());
            data.put("name", c.getName());
            data.put("skills", CategoryData.getSkills(c.getId()));
            data.put("order", order++);
            batch.set(db.collection(CATEGORIES).document(c.getId()), data);
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(providers.size()))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }
}
