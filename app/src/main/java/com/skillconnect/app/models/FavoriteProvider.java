package com.skillconnect.app.models;

/**
 * A row of the local SQLite tables favorite_providers / recently_viewed.
 * This is a small offline "snapshot" of a Provider.
 */
public class FavoriteProvider {

    private long id;
    private String providerId;
    private String name;
    private String skill;
    private String category;
    private String imageUrl;
    private double rating;
    private double price;
    private String priceUnit;
    private long savedAt;

    public FavoriteProvider() {
    }

    public static FavoriteProvider fromProvider(Provider p) {
        FavoriteProvider f = new FavoriteProvider();
        f.providerId = p.getProviderId();
        f.name = p.getName();
        f.skill = p.getTitle();
        f.category = p.getCategory();
        f.imageUrl = p.getProfileImage();
        f.rating = p.getAverageRating();
        f.price = p.getStartingPrice();
        f.priceUnit = p.getPriceUnit();
        f.savedAt = System.currentTimeMillis();
        return f;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }
}
