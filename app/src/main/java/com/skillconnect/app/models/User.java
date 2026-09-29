package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

/**
 * A SkillConnect account. Stored in Firestore at users/{userId}.
 * Passwords are NEVER stored here – Firebase Authentication handles them.
 */
public class User {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String ROLE_ADMIN = "ADMIN";

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String profileImage;
    private String address;
    private String fcmToken;
    private double latitude;
    private double longitude;
    private long createdAt;
    private boolean active = true;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String userId, String name, String email, String phone, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    @Exclude
    public boolean isCustomer() {
        return ROLE_CUSTOMER.equals(role);
    }

    @Exclude
    public boolean isProvider() {
        return ROLE_PROVIDER.equals(role);
    }

    @Exclude
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    @Exclude
    public String getFirstName() {
        if (name == null || name.trim().isEmpty()) return "there";
        return name.trim().split("\\s+")[0];
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
