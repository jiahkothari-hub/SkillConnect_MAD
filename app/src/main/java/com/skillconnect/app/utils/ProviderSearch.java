package com.skillconnect.app.utils;

import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.models.FilterOptions;
import com.skillconnect.app.models.Provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search + filter + sort logic for provider lists. Search matches name, skill (title and
 * skills list), category and description. Every word typed must match somewhere.
 */
public final class ProviderSearch {

    private ProviderSearch() {
    }

    private static String haystack(Provider p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName()).append(' ')
                .append(p.getTitle()).append(' ')
                .append(CategoryData.getCategoryName(p.getCategory())).append(' ')
                .append(p.getDescription()).append(' ')
                .append(p.getAddress()).append(' ');
        for (String s : p.getSkills()) sb.append(s).append(' ');
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    public static boolean matches(Provider p, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String text = haystack(p);
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!text.contains(word)) return false;
        }
        return true;
    }

    /** Higher = better match. Skill/title matches rank above description matches. */
    private static int relevance(Provider p, String query) {
        if (query == null || query.trim().isEmpty()) return 0;
        String q = query.toLowerCase(Locale.ROOT).trim();
        int score = 0;
        String title = p.getTitle() != null ? p.getTitle().toLowerCase(Locale.ROOT) : "";
        if (title.startsWith(q)) score += 6;
        else if (title.contains(q)) score += 4;
        for (String s : p.getSkills()) if (s.toLowerCase(Locale.ROOT).contains(q)) score += 2;
        if (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)) score += 3;
        return score;
    }

    public static List<Provider> apply(List<Provider> all, String query, FilterOptions f) {
        List<Provider> result = new ArrayList<>();
        double maxKm = f.maxDistanceKm();
        for (Provider p : all) {
            if (!matches(p, query)) continue;
            if (f.categoryId != null && !f.categoryId.equals(p.getCategory())) continue;
            if (f.skill != null && !CategoryData.providerHasSkill(p, f.skill)) continue;
            if (maxKm > 0 && (p.getDistanceKm() < 0 || p.getDistanceKm() > maxKm)) continue;
            if (!f.matchesPrice(p.getStartingPrice())) continue;
            if (p.getAverageRating() < f.minRating) continue;
            if (f.availableOnly && !p.isAvailable()) continue;
            if (f.verifiedOnly && !p.isVerified()) continue;
            result.add(p);
        }

        Comparator<Provider> byRating = (a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating());
        Comparator<Provider> byDistance = (a, b) -> {
            double da = a.getDistanceKm() < 0 ? Double.MAX_VALUE : a.getDistanceKm();
            double db = b.getDistanceKm() < 0 ? Double.MAX_VALUE : b.getDistanceKm();
            return Double.compare(da, db);
        };
        switch (f.sortBy) {
            case FilterOptions.SORT_RATING:
                Collections.sort(result, byRating);
                break;
            case FilterOptions.SORT_PRICE_LOW:
                Collections.sort(result, (a, b) -> Double.compare(a.getStartingPrice(), b.getStartingPrice()));
                break;
            case FilterOptions.SORT_DISTANCE:
                Collections.sort(result, byDistance);
                break;
            default:
                if (query != null && !query.trim().isEmpty()) {
                    Collections.sort(result, (a, b) -> {
                        int r = Integer.compare(relevance(b, query), relevance(a, query));
                        return r != 0 ? r : byRating.compare(a, b);
                    });
                } else {
                    // No query: nearby, well-rated and available people first
                    Collections.sort(result, (a, b) -> {
                        if (a.isAvailable() != b.isAvailable()) return a.isAvailable() ? -1 : 1;
                        return byDistance.compare(a, b);
                    });
                }
        }
        return result;
    }
}
