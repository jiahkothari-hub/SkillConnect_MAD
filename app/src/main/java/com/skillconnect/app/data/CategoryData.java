package com.skillconnect.app.data;

import com.skillconnect.app.R;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.Provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The fixed catalogue of categories and the skills inside each one. */
public final class CategoryData {

    public static final String HOME = "HOME";
    public static final String CREATIVE = "CREATIVE";
    public static final String EDUCATION = "EDUCATION";
    public static final String TECH = "TECH";
    public static final String LIFESTYLE = "LIFESTYLE";

    private static final Map<String, List<String>> SKILLS = new LinkedHashMap<>();

    static {
        SKILLS.put(HOME, Arrays.asList("Electrician", "Plumber", "Carpenter", "Painter", "AC Repair",
                "Home Decorator", "Appliance Repair"));
        SKILLS.put(CREATIVE, Arrays.asList("Photographer", "Video Editor", "Graphic Designer",
                "UI/UX Designer", "Makeup Artist", "Mehendi Artist", "UGC Creator"));
        SKILLS.put(EDUCATION, Arrays.asList("Guitar Teacher", "Coding Tutor", "Math Tutor",
                "Language Tutor", "Music Teacher", "Dance Teacher"));
        SKILLS.put(TECH, Arrays.asList("Web Developer", "Android Developer", "Data Analyst",
                "Digital Marketer", "Copywriter", "Resume Writer"));
        SKILLS.put(LIFESTYLE, Arrays.asList("Fitness Trainer", "Yoga Instructor", "Personal Stylist",
                "Home Baker", "Event Decorator", "Chef"));
    }

    private CategoryData() {
    }

    public static List<Category> getMainCategories() {
        List<Category> list = new ArrayList<>();
        for (String id : SKILLS.keySet()) {
            list.add(getCategory(id));
        }
        return list;
    }

    public static Category getCategory(String id) {
        if (id == null) return null;
        switch (id) {
            case HOME:
                return new Category(HOME, "Home Services", null, R.drawable.ic_cat_home, R.color.cat_home, R.color.cat_home_bg);
            case CREATIVE:
                return new Category(CREATIVE, "Creative", null, R.drawable.ic_cat_creative, R.color.cat_creative, R.color.cat_creative_bg);
            case EDUCATION:
                return new Category(EDUCATION, "Education", null, R.drawable.ic_cat_education, R.color.cat_education, R.color.cat_education_bg);
            case TECH:
                return new Category(TECH, "Technology", null, R.drawable.ic_cat_tech, R.color.cat_tech, R.color.cat_tech_bg);
            case LIFESTYLE:
                return new Category(LIFESTYLE, "Lifestyle", null, R.drawable.ic_cat_lifestyle, R.color.cat_lifestyle, R.color.cat_lifestyle_bg);
            default:
                return null;
        }
    }

    public static String getCategoryName(String id) {
        Category c = getCategory(id);
        return c != null ? c.getName() : "Other";
    }

    public static int getCategoryIcon(String id) {
        Category c = getCategory(id);
        return c != null ? c.getIconRes() : R.drawable.ic_work;
    }

    public static List<String> getSkills(String categoryId) {
        List<String> skills = SKILLS.get(categoryId);
        return skills != null ? skills : new ArrayList<>();
    }

    public static List<String> getAllSkills() {
        List<String> all = new ArrayList<>();
        for (List<String> s : SKILLS.values()) all.addAll(s);
        return all;
    }

    /** Main categories followed by their skills, in display order (for the Categories screen). */
    public static List<Category> getCategoryTree() {
        List<Category> tree = new ArrayList<>();
        for (Category main : getMainCategories()) {
            tree.add(main);
            for (String skill : getSkills(main.getId())) {
                tree.add(new Category(skill, skill, main.getId(), main.getIconRes(), main.getColorRes(), main.getBackgroundRes()));
            }
        }
        return tree;
    }

    public static String findCategoryForSkill(String skill) {
        if (skill == null) return null;
        for (Map.Entry<String, List<String>> e : SKILLS.entrySet()) {
            for (String s : e.getValue()) {
                if (s.equalsIgnoreCase(skill.trim())) return e.getKey();
            }
        }
        return null;
    }

    public static boolean providerHasSkill(Provider p, String skill) {
        if (skill == null) return true;
        if (p.getTitle() != null && p.getTitle().equalsIgnoreCase(skill)) return true;
        for (String s : p.getSkills()) {
            if (s.equalsIgnoreCase(skill)) return true;
        }
        return false;
    }

    public static int countForCategory(List<Provider> providers, String categoryId) {
        int count = 0;
        for (Provider p : providers) if (categoryId.equals(p.getCategory())) count++;
        return count;
    }

    public static int countForSkill(List<Provider> providers, String skill) {
        int count = 0;
        for (Provider p : providers) if (providerHasSkill(p, skill)) count++;
        return count;
    }
}
