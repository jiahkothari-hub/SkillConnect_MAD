package com.skillconnect.app.models;

/**
 * A customer's rating of a completed booking. Stored at reviews/{reviewId}
 * (reviewId == bookingId, so each booking can be reviewed only once).
 */
public class Review {

    private String reviewId;
    private String bookingId;
    private String customerId;
    private String providerId;
    private String customerName;
    private double rating;
    private String comment;
    private long createdAt;

    public Review() {
        // Required empty constructor for Firestore
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
