package com.skillconnect.app.utils;

import android.util.Patterns;

/** Input validation. Never trust user input – everything is checked before saving. */
public final class ValidationUtils {

    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final double MAX_PRICE = 100000;

    private ValidationUtils() {
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidName(String name) {
        return !isEmpty(name) && name.trim().length() >= 2 && name.trim().length() <= 60
                && name.trim().matches("[\\p{L} .'-]+");
    }

    /** Removes spaces, dashes and an optional +91 / 0 prefix. */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[\\s-]", "");
        if (digits.startsWith("+91")) digits = digits.substring(3);
        else if (digits.startsWith("91") && digits.length() == 12) digits = digits.substring(2);
        else if (digits.startsWith("0") && digits.length() == 11) digits = digits.substring(1);
        return digits;
    }

    /** Indian mobile number: 10 digits starting with 6-9. */
    public static boolean isValidPhone(String phone) {
        return normalizePhone(phone).matches("[6-9][0-9]{9}");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /** Returns the price, or -1 if it is not a valid positive amount. */
    public static double parsePrice(String value) {
        try {
            double price = Double.parseDouble(value.trim());
            if (price <= 0 || price > MAX_PRICE) return -1;
            return price;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Returns years of experience (0-60) or -1 if invalid. */
    public static int parseExperience(String value) {
        try {
            int years = Integer.parseInt(value.trim());
            return years >= 0 && years <= 60 ? years : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
