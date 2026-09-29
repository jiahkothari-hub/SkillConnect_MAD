package com.skillconnect.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.models.Booking;

import java.text.NumberFormat;
import java.util.Locale;

/** Small UI helpers shared by many screens. */
public final class UiUtils {

    private UiUtils() {
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /** ₹1,500 (Indian number format). */
    public static String formatPrice(double price) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        return "₹" + nf.format(price);
    }

    /** "From ₹300" */
    public static String startingPrice(double price) {
        return "From " + formatPrice(price);
    }

    public static String formatRating(double rating) {
        return String.format(Locale.US, "%.1f", rating);
    }

    /** "1 review" / "37 reviews" */
    public static String reviewCount(int count) {
        return count == 1 ? "1 review" : count + " reviews";
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /** Standard confirmation dialog: [Cancel] [positive]. */
    public static void confirm(Context context, String title, String message, String positive, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positive, (d, w) -> onConfirm.run())
                .show();
    }

    public static String statusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case Booking.STATUS_PENDING: return "Pending";
            case Booking.STATUS_ACCEPTED: return "Accepted";
            case Booking.STATUS_REJECTED: return "Declined";
            case Booking.STATUS_COMPLETED: return "Completed";
            case Booking.STATUS_CANCELLED: return "Cancelled";
            default: return status;
        }
    }

    /** Colours a pill TextView according to the booking status. */
    public static void styleStatusPill(TextView pill, String status) {
        int text;
        int bg;
        if (Booking.STATUS_ACCEPTED.equals(status)) {
            text = R.color.info;
            bg = R.color.info_light;
        } else if (Booking.STATUS_COMPLETED.equals(status)) {
            text = R.color.success;
            bg = R.color.success_light;
        } else if (Booking.STATUS_PENDING.equals(status)) {
            text = R.color.warning;
            bg = R.color.warning_light;
        } else {
            text = R.color.error;
            bg = R.color.error_light;
        }
        Context c = pill.getContext();
        pill.setText(statusLabel(status));
        pill.setTextColor(ContextCompat.getColor(c, text));
        pill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, bg)));
    }

    public static void tintBackground(View view, int colorRes) {
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), colorRes)));
    }

    public static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
