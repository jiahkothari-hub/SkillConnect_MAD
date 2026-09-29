package com.skillconnect.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Date and time formatting helpers. */
public final class DateTimeUtils {

    public static final List<String> DAYS = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    private DateTimeUtils() {
    }

    /** 28-09-2026 (the format stored in bookings). */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date(millis));
    }

    /** Mon, 28 Sep 2026 */
    public static String displayDate(long millis) {
        return new SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(new Date(millis));
    }

    /** 17:30 */
    public static String formatTime24(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    /** "17:30" -> "5:30 PM" */
    public static String to12Hour(String time24) {
        int[] hm = parseTime(time24);
        if (hm == null) return time24 != null ? time24 : "";
        int h = hm[0] % 12;
        if (h == 0) h = 12;
        return String.format(Locale.US, "%d:%02d %s", h, hm[1], hm[0] < 12 ? "AM" : "PM");
    }

    /** "17:30" -> {17, 30}, or null if invalid. */
    public static int[] parseTime(String time24) {
        if (time24 == null) return null;
        String[] parts = time24.split(":");
        if (parts.length != 2) return null;
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Combines the calendar day of {@code dayMillis} with an "HH:mm" time. */
    public static long combine(long dayMillis, String time24) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayMillis);
        int[] hm = parseTime(time24);
        c.set(Calendar.HOUR_OF_DAY, hm != null ? hm[0] : 0);
        c.set(Calendar.MINUTE, hm != null ? hm[1] : 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Parses the stored "dd-MM-yyyy" date back into millis (0 if invalid). */
    public static long parseDate(String date) {
        try {
            Date d = new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date);
            return d != null ? d.getTime() : 0;
        } catch (ParseException | NullPointerException e) {
            return 0;
        }
    }

    public static boolean isTomorrow(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR);
    }

    /** "just now", "5 min ago", "3 days ago", or a date. */
    public static String relativeTime(long millis) {
        long diff = System.currentTimeMillis() - millis;
        long minutes = diff / 60000;
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");
        return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(new Date(millis));
    }

    /** ["Mon".."Sat"] -> "Mon - Sat"; ["Mon","Wed"] -> "Mon, Wed". */
    public static String summarizeDays(List<String> days) {
        if (days == null || days.isEmpty()) return "Not set";
        List<Integer> idx = new ArrayList<>();
        for (String d : DAYS) if (days.contains(d)) idx.add(DAYS.indexOf(d));
        if (idx.size() == 7) return "Every day";
        boolean consecutive = idx.size() >= 3;
        for (int i = 1; i < idx.size() && consecutive; i++) {
            if (idx.get(i) != idx.get(i - 1) + 1) consecutive = false;
        }
        if (consecutive) return DAYS.get(idx.get(0)) + " - " + DAYS.get(idx.get(idx.size() - 1));
        StringBuilder sb = new StringBuilder();
        for (int i : idx) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(DAYS.get(i));
        }
        return sb.toString();
    }

    public static String hoursRange(String from, String to) {
        return to12Hour(from) + " - " + to12Hour(to);
    }
}
