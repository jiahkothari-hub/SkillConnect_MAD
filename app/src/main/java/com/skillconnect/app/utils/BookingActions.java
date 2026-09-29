package com.skillconnect.app.utils;

import android.content.Context;

import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;

/** Confirms and performs a booking status change (accept, decline, complete, cancel). */
public final class BookingActions {

    private BookingActions() {
    }

    public static void changeStatus(Context context, Booking booking, String newStatus, Runnable onDone) {
        String title;
        String message;
        String positive;
        String success;
        String when = booking.getService() + " on " + booking.getDate() + " at " + DateTimeUtils.to12Hour(booking.getTime());
        switch (newStatus) {
            case Booking.STATUS_ACCEPTED:
                title = "Accept this request?";
                message = booking.getCustomerName() + " will be notified that you accepted " + when + ".";
                positive = "Accept";
                success = "Request accepted";
                break;
            case Booking.STATUS_REJECTED:
                title = "Decline this request?";
                message = booking.getCustomerName() + " will be notified that you can't take this job.";
                positive = "Decline";
                success = "Request declined";
                break;
            case Booking.STATUS_COMPLETED:
                title = "Mark job as completed?";
                message = "Only do this after the service is done. " + booking.getCustomerName()
                        + " will then be able to leave a review.";
                positive = "Mark completed";
                success = "Job marked as completed";
                break;
            default:
                title = "Cancel booking?";
                message = "Are you sure you want to cancel this booking?\n\n" + when;
                positive = "Yes, cancel";
                success = "Booking cancelled";
                break;
        }
        UiUtils.confirm(context, title, message, positive, () -> {
            if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(context)) {
                UiUtils.toast(context, "You're offline. Check your connection and try again.");
                return;
            }
            RepositoryProvider.get().updateBookingStatus(booking.getBookingId(), newStatus, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    booking.setStatus(newStatus);
                    UiUtils.toast(context, success);
                    if (onDone != null) onDone.run();
                }

                @Override
                public void onError(String error) {
                    UiUtils.toast(context, error);
                }
            });
        });
    }
}
