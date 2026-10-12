package com.skillconnect.app.models;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * A main category (e.g. "Home Services") or a skill inside it (e.g. "Electrician").
 * Categories are bundled with the app (see CategoryData) and mirrored to
 * Firestore's categories collection by the admin seeder.
 */
public class Category {

    private final String id;
    private final String name;
    private final String parentId;   // null for main categories
    @DrawableRes private final int iconRes;
    @ColorRes private final int colorRes;
    @ColorRes private final int backgroundRes;
    private int providerCount;

    public Category(String id, String name, String parentId, int iconRes, int colorRes, int backgroundRes) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
        this.backgroundRes = backgroundRes;
    }

    public boolean isMainCategory() {
        return parentId == null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public int getIconRes() { return iconRes; }
    public int getColorRes() { return colorRes; }
    public int getBackgroundRes() { return backgroundRes; }
    public int getProviderCount() { return providerCount; }
    public void setProviderCount(int providerCount) { this.providerCount = providerCount; }
}
