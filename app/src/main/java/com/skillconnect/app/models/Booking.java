package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

/**
 * A service request from a customer to a provider. Stored at bookings/{bookingId}.
 */
public class Booking {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String bookingId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String providerId;
    private String providerName;
    private String providerPhone;
    private String service;
    private String description;
    private String date;        // dd-MM-yyyy
    private String time;        // HH:mm (24h)
    private long scheduledAt;   // date + time in millis, used for sorting / "upcoming"
    private double price;
    private String location;
    private String status = STATUS_PENDING;
    private boolean reviewed;
    private long createdAt;
    private long updatedAt;

    public Booking() {
        // Required empty constructor for Firestore
    }

    @Exclude
    public boolean isOpen() {
        return STATUS_PENDING.equals(status) || STATUS_ACCEPTED.equals(status);
    }

    @Exclude
    public boolean isClosed() {
        return STATUS_CANCELLED.equals(status) || STATUS_REJECTED.equals(status);
    }

    @Exclude
    public boolean canBeReviewed() {
        return STATUS_COMPLETED.equals(status) && !reviewed;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderPhone() { return providerPhone; }
    public void setProviderPhone(String providerPhone) { this.providerPhone = providerPhone; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public long getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(long scheduledAt) { this.scheduledAt = scheduledAt; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
