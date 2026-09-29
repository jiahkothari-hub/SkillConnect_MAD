package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

/**
 * One image in a provider's portfolio. Stored at portfolio/{portfolioId};
 * the image file itself lives in Firebase Storage at portfolio/{providerId}/{portfolioId}.jpg.
 */
public class PortfolioItem {

    /** Demo-mode sample images use this prefix instead of a real URL. */
    public static final String DEMO_PREFIX = "demo:";

    private String portfolioId;
    private String providerId;
    private String imageUrl;
    private String storagePath;
    private String description;
    private long timestamp;

    public PortfolioItem() {
        // Required empty constructor for Firestore
    }

    @Exclude
    public boolean isDemoPlaceholder() {
        return imageUrl != null && imageUrl.startsWith(DEMO_PREFIX);
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
