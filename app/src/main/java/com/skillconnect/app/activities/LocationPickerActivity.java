package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.UiUtils;

import java.util.Locale;

/**
 * Lets a provider (or customer) choose a service location: move the map under the
 * centre pin, or use the current GPS location. Returns latitude, longitude and address.
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    public static final String EXTRA_ADDRESS = "address";

    private GoogleMap map;
    private double latitude;
    private double longitude;
    private TextInputLayout tilAddress;
    private TextInputEditText etAddress;
    private TextView tvCoordinates;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationUtils.hasPermission(this)) useCurrentLocation();
                else UiUtils.toast(this, "Location permission denied. Move the map to choose your location.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        double[] fallback = LocationUtils.getUserLocation(this);
        latitude = getIntent().getDoubleExtra(EXTRA_LAT, 0);
        longitude = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        if (latitude == 0 && longitude == 0) {
            latitude = fallback[0];
            longitude = fallback[1];
        }

        tilAddress = findViewById(R.id.tilAddress);
        etAddress = findViewById(R.id.etAddress);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        if (address != null) etAddress.setText(address);
        updateCoordinates();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUseCurrent).setOnClickListener(v -> {
            if (LocationUtils.hasPermission(this)) useCurrentLocation();
            else locationPermission.launch(LocationUtils.PERMISSIONS);
        });
        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirm());

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
            if (fragment != null) fragment.getMapAsync(this);
        } else {
            ((TextView) findViewById(R.id.tvPickerHint)).setText(
                    "Maps unavailable on this device. Use your current location and type your locality.");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15f));
        map.setOnCameraIdleListener(() -> {
            LatLng target = map.getCameraPosition().target;
            latitude = target.latitude;
            longitude = target.longitude;
            updateCoordinates();
            // Reverse geocoding: coordinates -> locality name (skipped while the user is typing)
            if (!etAddress.hasFocus()) {
                LocationUtils.reverseGeocode(this, latitude, longitude, result -> {
                    if (result != null && !etAddress.hasFocus()) etAddress.setText(result);
                });
            }
        });
    }

    private void useCurrentLocation() {
        UiUtils.toast(this, "Getting your location…");
        LocationUtils.fetchCurrentLocation(this, new LocationUtils.LocationListener() {
            @Override
            public void onLocation(double lat, double lng) {
                latitude = lat;
                longitude = lng;
                updateCoordinates();
                etAddress.clearFocus();
                if (map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16f));
                } else {
                    LocationUtils.reverseGeocode(LocationPickerActivity.this, lat, lng, result -> {
                        if (result != null) etAddress.setText(result);
                    });
                }
            }

            @Override
            public void onFailed(String message) {
                UiUtils.toast(LocationPickerActivity.this, message);
            }
        });
    }

    private void updateCoordinates() {
        tvCoordinates.setText(String.format(Locale.US, "%.5f, %.5f", latitude, longitude));
    }

    private void confirm() {
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        if (address.length() < 3) {
            tilAddress.setError("Enter your locality, e.g. Andheri East, Mumbai");
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_LAT, latitude);
        data.putExtra(EXTRA_LNG, longitude);
        data.putExtra(EXTRA_ADDRESS, address);
        setResult(RESULT_OK, data);
        finish();
    }
}
