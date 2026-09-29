package com.skillconnect.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.skillconnect.app.models.Provider;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Location helpers. Location is used ONLY for discovery (distance to providers) and
 * for providers to set their service area – never for continuous tracking.
 */
public final class LocationUtils {

    /** Default location (Andheri West, Mumbai) used when permission is denied. */
    public static final double DEFAULT_LAT = 19.1197;
    public static final double DEFAULT_LNG = 72.8468;

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface LocationListener {
        void onLocation(double latitude, double longitude);

        void onFailed(String message);
    }

    public interface AddressListener {
        void onAddress(String address);
    }

    private LocationUtils() {
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** The last known user location, or the default city location. */
    public static double[] getUserLocation(Context context) {
        PrefsManager prefs = PrefsManager.get(context);
        return new double[]{prefs.getLatitude(DEFAULT_LAT), prefs.getLongitude(DEFAULT_LNG)};
    }

    public static boolean isUsingDefault(Context context) {
        return !PrefsManager.get(context).hasSavedLocation();
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, result);
        return result[0] / 1000.0;
    }

    public static String formatDistance(double km) {
        if (km < 0) return "";
        if (km < 1) return String.format(Locale.US, "%d m away", Math.max(50, Math.round(km * 1000 / 50.0) * 50));
        return String.format(Locale.US, "%.1f km away", km);
    }

    /** Calculates distance from the user to every provider that has a location. */
    public static void applyDistances(Context context, List<Provider> providers) {
        double[] me = getUserLocation(context);
        for (Provider p : providers) {
            if (p.hasLocation()) p.setDistanceKm(distanceKm(me[0], me[1], p.getLatitude(), p.getLongitude()));
            else p.setDistanceKm(-1);
        }
    }

    /** One-off location request (no continuous tracking). */
    @SuppressLint("MissingPermission")
    public static void fetchCurrentLocation(Context context, LocationListener listener) {
        if (!hasPermission(context)) {
            listener.onFailed("Location permission not granted");
            return;
        }
        try {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
            CancellationTokenSource cts = new CancellationTokenSource();
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            listener.onLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            client.getLastLocation().addOnSuccessListener(last -> {
                                if (last != null) listener.onLocation(last.getLatitude(), last.getLongitude());
                                else listener.onFailed("Couldn't get your location. Is location turned on?");
                            }).addOnFailureListener(e -> listener.onFailed("Couldn't get your location."));
                        }
                    })
                    .addOnFailureListener(e -> listener.onFailed("Couldn't get your location. Is location turned on?"));
        } catch (Exception e) {
            listener.onFailed("Location services are unavailable on this device.");
        }
    }

    /** Converts coordinates into a short locality name on a background thread. */
    @SuppressWarnings("deprecation")
    public static void reverseGeocode(Context context, double lat, double lng, AddressListener listener) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            String result = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> list = new Geocoder(app, Locale.ENGLISH).getFromLocation(lat, lng, 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        StringBuilder sb = new StringBuilder();
                        if (a.getSubLocality() != null) sb.append(a.getSubLocality());
                        if (a.getLocality() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(a.getLocality());
                        }
                        if (sb.length() == 0 && a.getAddressLine(0) != null) sb.append(a.getAddressLine(0));
                        result = sb.length() > 0 ? sb.toString() : null;
                    }
                }
            } catch (Exception ignored) {
                // No network or geocoder – fall through to null
            }
            String finalResult = result;
            MAIN.post(() -> listener.onAddress(finalResult));
        });
    }

    /** Implicit intent: opens the location in Google Maps (or any maps app / browser). */
    public static void openInMaps(Context context, double lat, double lng, String label) {
        String query = lat + "," + lng + "(" + Uri.encode(label) + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + lat + "," + lng + "?q=" + query));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Uri web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, web));
            } catch (ActivityNotFoundException e2) {
                UiUtils.toast(context, "No maps app found on this device");
            }
        }
    }

    /** Implicit intent: search an address in Google Maps. */
    public static void openAddressInMaps(Context context, String address) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(context, "No maps app found on this device");
        }
    }
}
