package com.skillconnect.app.models;

import java.io.Serializable;

/**
 * The choices made in the Filters bottom sheet. Serializable so it can be passed
 * between screens in a Bundle.
 */
public class FilterOptions implements Serializable {

    public static final int SORT_RELEVANCE = 0;
    public static final int SORT_RATING = 1;
    public static final int SORT_PRICE_LOW = 2;
    public static final int SORT_DISTANCE = 3;

    public static final double[] DISTANCE_VALUES = {0, 2, 5, 10, 25};

    public String categoryId;       // null = all categories
    public String skill;            // null = any skill (set by category screen)
    public int distanceIndex;       // index into DISTANCE_VALUES, 0 = any
    public int priceRange;          // 0 any, 1 <500, 2 500-1000, 3 1000-2000, 4 2000+
    public double minRating;        // 0, 4.0 or 4.5
    public boolean availableOnly;
    public boolean verifiedOnly;
    public int sortBy = SORT_RELEVANCE;

    public FilterOptions copy() {
        FilterOptions f = new FilterOptions();
        f.categoryId = categoryId;
        f.skill = skill;
        f.distanceIndex = distanceIndex;
        f.priceRange = priceRange;
        f.minRating = minRating;
        f.availableOnly = availableOnly;
        f.verifiedOnly = verifiedOnly;
        f.sortBy = sortBy;
        return f;
    }

    /** Number of active filters (used for the "Filters (2)" badge). */
    public int activeCount() {
        int count = 0;
        if (categoryId != null) count++;
        if (distanceIndex > 0) count++;
        if (priceRange > 0) count++;
        if (minRating > 0) count++;
        if (availableOnly) count++;
        if (verifiedOnly) count++;
        if (sortBy != SORT_RELEVANCE) count++;
        return count;
    }

    public double maxDistanceKm() {
        if (distanceIndex <= 0 || distanceIndex >= DISTANCE_VALUES.length) return 0;
        return DISTANCE_VALUES[distanceIndex];
    }

    public boolean matchesPrice(double price) {
        switch (priceRange) {
            case 1: return price < 500;
            case 2: return price >= 500 && price <= 1000;
            case 3: return price > 1000 && price <= 2000;
            case 4: return price > 2000;
            default: return true;
        }
    }
}
