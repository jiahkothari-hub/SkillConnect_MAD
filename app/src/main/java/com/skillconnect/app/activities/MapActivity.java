package com.skillconnect.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skillconnect.app.BuildConfig;
import com.skillconnect.app.R;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/**
 * "Nearby Providers" Google Map. Each provider is a marker; tapping a marker shows a
 * card with name, skill, rating, starting price and a View Profile button.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_FOCUS_PROVIDER_ID = "focus_provider_id";
    public static final String EXTRA_CATEGORY_ID = "category_id";

    private GoogleMap map;
    private String focusProviderId;
    private String categoryId;
    private StateView state;
    private View cardProvider;
    private TextView tvSubtitle;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationUtils.hasPermission(this)) {
                    enableMyLocationLayer();
                    goToMyLocation();
                } else {
                    UiUtils.toast(this, "Location permission denied. Showing the default city.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        focusProviderId = getIntent().getStringExtra(EXTRA_FOCUS_PROVIDER_ID);
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);

        state = new StateView(findViewById(R.id.mapState));
        cardProvider = findViewById(R.id.cardProvider);
        tvSubtitle = findViewById(R.id.tvMapSubtitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> {
            if (LocationUtils.hasPermission(this)) goToMyLocation();
            else locationPermission.launch(LocationUtils.PERMISSIONS);
        });

        if (!BuildConfig.MAPS_KEY_CONFIGURED) {
            findViewById(R.id.tvMapWarning).setVisibility(View.VISIBLE);
        }

        // Google Maps needs Google Play services – show a friendly message instead of crashing.
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            state.showEmpty(R.drawable.ic_map, "Google Maps is unavailable",
                    "This device doesn't have Google Play services. You can still browse providers in the list.",
                    getString(R.string.back), this::finish);
            return;
        }
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (fragment != null) fragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setPadding(0, UiUtils.dp(this, 72), 0, 0);
        enableMyLocationLayer();

        double[] me = LocationUtils.getUserLocation(this);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(me[0], me[1]), 11.5f));

        map.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Provider) {
                showProviderCard((Provider) tag);
                marker.showInfoWindow();
                return true;
            }
            return false;
        });
        map.setOnMapClickListener(latLng -> cardProvider.setVisibility(View.GONE));
        loadProviders();
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationLayer() {
        if (map != null && LocationUtils.hasPermission(this)) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
                // permission revoked
            }
        }
    }

    private void goToMyLocation() {
        LocationUtils.fetchCurrentLocation(this, new LocationUtils.LocationListener() {
            @Override
            public void onLocation(double latitude, double longitude) {
                if (map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 13f));
                }
            }

            @Override
            public void onFailed(String message) {
                UiUtils.toast(MapActivity.this, message);
            }
        });
    }

    private void loadProviders() {
        RepositoryProvider.get().getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> providers) {
                if (map == null) return;
                LocationUtils.applyDistances(MapActivity.this, providers);
                int count = 0;
                Marker focusMarker = null;
                Provider focusProvider = null;
                for (Provider p : providers) {
                    if (!p.hasLocation()) continue;
                    if (categoryId != null && !categoryId.equals(p.getCategory())) continue;
                    boolean isFocus = p.getProviderId().equals(focusProviderId);
                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(p.getLatitude(), p.getLongitude()))
                            .title(p.getName())
                            .snippet(p.getTitle() + " · ★ " + UiUtils.formatRating(p.getAverageRating()))
                            .icon(BitmapDescriptorFactory.defaultMarker(isFocus
                                    ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_VIOLET)));
                    if (marker != null) {
                        marker.setTag(p);
                        if (isFocus) {
                            focusMarker = marker;
                            focusProvider = p;
                        }
                    }
                    count++;
                }
                String scope = categoryId != null ? CategoryData.getCategoryName(categoryId) + " · " : "";
                tvSubtitle.setText(scope + (count == 1 ? "1 provider on the map" : count + " providers on the map"));
                if (focusMarker != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(focusMarker.getPosition(), 14f));
                    focusMarker.showInfoWindow();
                    showProviderCard(focusProvider);
                }
            }

            @Override
            public void onError(String message) {
                tvSubtitle.setText(message);
            }
        });
    }

    private void showProviderCard(Provider p) {
        cardProvider.setVisibility(View.VISIBLE);
        ImageUtils.loadAvatar(findViewById(R.id.ivCardAvatar), p.getProfileImage(), p.getName());
        TextView name = findViewById(R.id.tvCardName);
        name.setText(p.getName());
        name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, p.isVerified() ? R.drawable.ic_verified : 0, 0);
        ((TextView) findViewById(R.id.tvCardSkill)).setText(p.getTitle());
        String meta = (p.getReviewCount() > 0 ? UiUtils.formatRating(p.getAverageRating()) + " (" + p.getReviewCount() + ")" : "New");
        if (p.getDistanceKm() >= 0) meta += "  ·  " + LocationUtils.formatDistance(p.getDistanceKm());
        ((TextView) findViewById(R.id.tvCardMeta)).setText(meta);
        ((TextView) findViewById(R.id.tvCardPrice)).setText(UiUtils.startingPrice(p.getStartingPrice()));

        findViewById(R.id.btnDirections).setOnClickListener(v ->
                LocationUtils.openInMaps(this, p.getLatitude(), p.getLongitude(), p.getName()));
        findViewById(R.id.btnCardProfile).setOnClickListener(v -> {
            if (p.getProviderId().equals(focusProviderId)) {
                finish(); // we came from this profile
                return;
            }
            Intent i = new Intent(this, ProviderDetailsActivity.class);
            i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, p.getProviderId());
            startActivity(i);
        });
    }
}
