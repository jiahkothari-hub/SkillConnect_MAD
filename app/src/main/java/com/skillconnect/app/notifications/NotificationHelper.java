package com.skillconnect.app.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.skillconnect.app.R;
import com.skillconnect.app.activities.BookingDetailsActivity;
import com.skillconnect.app.activities.SplashActivity;
import com.skillconnect.app.utils.PrefsManager;

/** Creates the notification channel and shows booking notifications. */
public final class NotificationHelper {

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        NotificationChannel channel = new NotificationChannel(
                context.getString(R.string.channel_bookings_id),
                context.getString(R.string.channel_bookings_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_bookings_desc));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    public static boolean canNotify(Context context) {
        if (!PrefsManager.get(context).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Shows a notification; tapping it opens the booking (or the app if bookingId is null). */
    public static void show(Context context, String title, String body, String bookingId) {
        if (!canNotify(context)) return;
        Intent intent;
        if (bookingId != null) {
            intent = new Intent(context, BookingDetailsActivity.class);
            intent.putExtra(BookingDetailsActivity.EXTRA_BOOKING_ID, bookingId);
        } else {
            intent = new Intent(context, SplashActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int requestCode = bookingId != null ? bookingId.hashCode() : 0;
        PendingIntent pi = PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.channel_bookings_id))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build());
        } catch (SecurityException ignored) {
            // Permission revoked between the check and the call
        }
    }
}
