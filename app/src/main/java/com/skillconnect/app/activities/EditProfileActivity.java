package com.skillconnect.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.ImagePickerHelper;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.UiUtils;
import com.skillconnect.app.utils.ValidationUtils;

/** Edit customer profile: photo (camera/gallery), name, phone and location. */
public class EditProfileActivity extends AppCompatActivity {

    private AppRepository repo;
    private User user;
    private ImagePickerHelper imagePicker;
    private Uri newPhotoUri;
    private double latitude;
    private double longitude;

    private ImageView ivAvatar;
    private TextInputLayout tilName, tilPhone, tilAddress;
    private TextInputEditText etName, etPhone, etAddress;
    private MaterialButton btnSave;
    private View progress;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationUtils.hasPermission(this)) useCurrentLocation();
                else UiUtils.toast(this, "Location permission denied. You can type your locality instead.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        repo = RepositoryProvider.get();
        user = repo.getCachedUser();
        if (user == null) {
            finish();
            return;
        }
        imagePicker = new ImagePickerHelper(this, uri -> {
            newPhotoUri = uri;
            ImageUtils.loadImage(ivAvatar, uri);   // preview before upload
        });
        imagePicker.restoreState(savedInstanceState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ivAvatar = findViewById(R.id.ivAvatar);
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilAddress = findViewById(R.id.tilAddress);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);

        ImageUtils.loadAvatar(ivAvatar, user.getProfileImage(), user.getName());
        etName.setText(user.getName());
        etPhone.setText(user.getPhone());
        etAddress.setText(user.getAddress());
        latitude = user.getLatitude();
        longitude = user.getLongitude();
        ((TextView) findViewById(R.id.tvEmail)).setText("Email: " + user.getEmail() + " (cannot be changed)");

        View.OnClickListener pickPhoto = v -> imagePicker.showSourceDialog();
        findViewById(R.id.btnChangePhoto).setOnClickListener(pickPhoto);
        findViewById(R.id.tvChangePhoto).setOnClickListener(pickPhoto);
        ivAvatar.setOnClickListener(pickPhoto);
        findViewById(R.id.btnUseLocation).setOnClickListener(v -> {
            if (LocationUtils.hasPermission(this)) useCurrentLocation();
            else locationPermission.launch(LocationUtils.PERMISSIONS);
        });
        btnSave.setOnClickListener(v -> save());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagePicker != null) imagePicker.saveState(outState);
    }

    private void useCurrentLocation() {
        UiUtils.toast(this, "Getting your location…");
        LocationUtils.fetchCurrentLocation(this, new LocationUtils.LocationListener() {
            @Override
            public void onLocation(double lat, double lng) {
                latitude = lat;
                longitude = lng;
                LocationUtils.reverseGeocode(EditProfileActivity.this, lat, lng, address -> {
                    String value = address != null ? address : "Current location";
                    etAddress.setText(value);
                    PrefsManager.get(EditProfileActivity.this).saveLocation(lat, lng, value);
                });
            }

            @Override
            public void onFailed(String message) {
                UiUtils.toast(EditProfileActivity.this, message);
            }
        });
    }

    private void save() {
        tilName.setError(null);
        tilPhone.setError(null);
        String name = text(etName);
        String phone = text(etPhone);
        boolean valid = true;
        if (!ValidationUtils.isValidName(name)) {
            tilName.setError("Enter your full name (letters only)");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            tilPhone.setError("Enter a valid 10-digit mobile number");
            valid = false;
        }
        if (!valid) return;
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        setLoading(true);
        if (newPhotoUri != null) {
            repo.uploadProfileImage(this, newPhotoUri, new Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    saveUser(name, phone, url);
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    UiUtils.toast(EditProfileActivity.this, message);
                }
            });
        } else {
            saveUser(name, phone, user.getProfileImage());
        }
    }

    private void saveUser(String name, String phone, String imageUrl) {
        user.setName(name);
        user.setPhone(ValidationUtils.normalizePhone(phone));
        user.setAddress(text(etAddress));
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setProfileImage(imageUrl);
        repo.updateUser(user, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                UiUtils.toast(EditProfileActivity.this, "Profile updated successfully");
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                UiUtils.toast(EditProfileActivity.this, message);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
