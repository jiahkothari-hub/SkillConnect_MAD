package com.skillconnect.app.activities;

import com.skillconnect.app.models.Booking;

import java.util.List;

/**
 * Implemented by the home activities. They keep ONE real-time bookings listener and
 * share the latest list with their tabs (fragments), so every tab stays in sync.
 */
public interface BookingsHost {

    interface BookingsListener {
        void onBookingsChanged(List<Booking> bookings);

        void onBookingsError(String message);
    }

    /** Latest bookings, or null if still loading. */
    List<Booking> getLatestBookings();

    void addBookingsListener(BookingsListener listener);

    void removeBookingsListener(BookingsListener listener);

    void selectTab(int menuItemId);
}
