package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Public profile of a skilled person. Stored in Firestore at providers/{providerId}
 * where providerId is the same as the provider's userId.
 */
public class Provider {

    private String providerId;
    private String name;
    private String title;          // primary skill, e.g. "Electrician"
    private String category;       // category id, e.g. "HOME"
    private List<String> skills = new ArrayList<>();
    private int experienceYears;
    private String description;
    private double startingPrice;
    private String priceUnit = "per visit";
    private double latitude;
    private double longitude;
    private String address;        // locality, e.g. "Andheri East, Mumbai"
    private List<String> availableDays = new ArrayList<>();
    private String availableFrom = "09:00";
    private String availableTo = "19:00";
    private boolean available = true;   // currently accepting new work
    private String phone;
    private String email;
    private String profileImage;
    private boolean verified;
    private double averageRating;
    private int reviewCount;
    private boolean active = true;      // admin can deactivate a listing
    private long createdAt;

    // Calculated on the device, never stored in Firestore
    private double distanceKm = -1;

    public Provider() {
        // Required empty constructor for Firestore
    }

    @Exclude
    public double getDistanceKm() { return distanceKm; }

    @Exclude
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    @Exclude
    public boolean hasLocation() {
        return latitude != 0 || longitude != 0;
    }

    @Exclude
    public boolean isProfileComplete() {
        return hasLocation() && description != null && description.trim().length() >= 20
                && startingPrice > 0 && title != null && !title.trim().isEmpty();
    }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills != null ? skills : new ArrayList<>(); }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<String> getAvailableDays() { return availableDays; }
    public void setAvailableDays(List<String> availableDays) { this.availableDays = availableDays != null ? availableDays : new ArrayList<>(); }
    public String getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(String availableFrom) { this.availableFrom = availableFrom; }
    public String getAvailableTo() { return availableTo; }
    public void setAvailableTo(String availableTo) { this.availableTo = availableTo; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
