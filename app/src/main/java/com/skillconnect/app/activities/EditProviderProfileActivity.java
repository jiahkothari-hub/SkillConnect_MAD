package com.skillconnect.app.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.ImagePickerHelper;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.LocationUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;
import com.skillconnect.app.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Create / edit the provider's professional profile: photo, title, category, skills,
 * experience, price, description, service location, availability and verification status.
 */
public class EditProviderProfileActivity extends AppCompatActivity {

    public static final String EXTRA_FIRST_SETUP = "first_setup";
    public static final String EXTRA_SCROLL_TO_AVAILABILITY = "scroll_to_availability";
    private static final int MAX_SKILLS = 10;

    private AppRepository repo;
    private User user;
    private Provider provider;
    private boolean firstSetup;
    private ImagePickerHelper imagePicker;
    private Uri newPhotoUri;

    private final List<Category> categories = CategoryData.getMainCategories();
    private final List<String> skills = new ArrayList<>();
    private String[] priceUnits;
    private String availableFrom = "09:00";
    private String availableTo = "19:00";

    private StateView state;
    private ScrollView scrollView;
    private ImageView ivAvatar;
    private TextInputLayout tilName, tilTitle, tilPhone, tilSkill, tilExperience, tilPrice, tilDescription;
    private TextInputEditText etName, etPhone, etSkill, etExperience, etPrice, etDescription, etFrom, etTo;
    private MaterialAutoCompleteTextView actTitle;
    private Spinner spinnerCategory, spinnerPriceUnit;
    private ChipGroup chipsSkills, chipsDays;
    private TextView tvLocationValue;
    private MaterialSwitch switchAvailable;
    private MaterialButton btnSave;
    private View progress;

    private final ActivityResultLauncher<Intent> locationPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent d = result.getData();
                    setLocation(d.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0),
                            d.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0),
                            d.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS));
                }
            });

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationUtils.hasPermission(this)) useCurrentLocation();
                else UiUtils.toast(this, "Location permission denied. Use \"Choose on map\" instead.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_provider_profile);
        repo = RepositoryProvider.get();
        user = repo.getCachedUser();
        if (user == null) {
            finish();
            return;
        }
        firstSetup = getIntent().getBooleanExtra(EXTRA_FIRST_SETUP, false);
        imagePicker = new ImagePickerHelper(this, uri -> {
            newPhotoUri = uri;
            ImageUtils.loadImage(ivAvatar, uri);
        });
        imagePicker.restoreState(savedInstanceState);
        bindViews();
        load();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagePicker != null) imagePicker.saveState(outState);
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(firstSetup ? "Set up your profile" : getString(R.string.edit_profile));
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.tvSetupIntro).setVisibility(firstSetup ? View.VISIBLE : View.GONE);

        state = new StateView(findViewById(R.id.editState));
        scrollView = findViewById(R.id.scrollView);
        ivAvatar = findViewById(R.id.ivAvatar);
        tilName = findViewById(R.id.tilName);
        tilTitle = findViewById(R.id.tilTitle);
        tilPhone = findViewById(R.id.tilPhone);
        tilSkill = findViewById(R.id.tilSkill);
        tilExperience = findViewById(R.id.tilExperience);
        tilPrice = findViewById(R.id.tilPrice);
        tilDescription = findViewById(R.id.tilDescription);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etSkill = findViewById(R.id.etSkill);
        etExperience = findViewById(R.id.etExperience);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        etFrom = findViewById(R.id.etFrom);
        etTo = findViewById(R.id.etTo);
        actTitle = findViewById(R.id.actTitle);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriceUnit = findViewById(R.id.spinnerPriceUnit);
        chipsSkills = findViewById(R.id.chipsSkills);
        chipsDays = findViewById(R.id.chipsDays);
        tvLocationValue = findViewById(R.id.tvLocationValue);
        switchAvailable = findViewById(R.id.switchAvailable);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);

        // Category spinner
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, names);
        catAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerCategory.setAdapter(catAdapter);

        // Price unit spinner
        priceUnits = getResources().getStringArray(R.array.price_units);
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, priceUnits);
        unitAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerPriceUnit.setAdapter(unitAdapter);

        // Title suggestions: all skills. Picking one also selects its category.
        actTitle.setSimpleItems(CategoryData.getAllSkills().toArray(new String[0]));
        actTitle.setOnItemClickListener((parent, view, position, id) -> {
            String cat = CategoryData.findCategoryForSkill(actTitle.getText().toString());
            selectCategory(cat);
        });

        // Working days chips
        for (String day : DateTimeUtils.DAYS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter, chipsDays, false);
            chip.setText(day);
            chip.setCheckable(true);
            chipsDays.addView(chip);
        }

        View.OnClickListener pickPhoto = v -> imagePicker.showSourceDialog();
        ivAvatar.setOnClickListener(pickPhoto);
        findViewById(R.id.btnChangePhoto).setOnClickListener(pickPhoto);
        findViewById(R.id.btnAddSkill).setOnClickListener(v -> addSkillFromInput());
        etSkill.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSkillFromInput();
                return true;
            }
            return false;
        });
        etFrom.setOnClickListener(v -> pickTime(true));
        etTo.setOnClickListener(v -> pickTime(false));
        findViewById(R.id.btnChooseOnMap).setOnClickListener(v -> {
            Intent i = new Intent(this, LocationPickerActivity.class);
            if (provider != null && provider.hasLocation()) {
                i.putExtra(LocationPickerActivity.EXTRA_LAT, provider.getLatitude());
                i.putExtra(LocationPickerActivity.EXTRA_LNG, provider.getLongitude());
                i.putExtra(LocationPickerActivity.EXTRA_ADDRESS, provider.getAddress());
            }
            locationPicker.launch(i);
        });
        findViewById(R.id.btnCurrentLocation).setOnClickListener(v -> {
            if (LocationUtils.hasPermission(this)) useCurrentLocation();
            else locationPermission.launch(LocationUtils.PERMISSIONS);
        });
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void load() {
        state.showLoading(getString(R.string.loading));
        repo.getProvider(user.getUserId(), new Callback<Provider>() {
            @Override
            public void onSuccess(Provider result) {
                provider = result;
                showForm();
            }

            @Override
            public void onError(String message) {
                if ("Provider not found".equals(message)) {
                    provider = new Provider();
                    provider.setProviderId(user.getUserId());
                    provider.setCreatedAt(System.currentTimeMillis());
                    showForm();
                } else {
                    state.showError(message, EditProviderProfileActivity.this::load);
                }
            }
        });
    }

    private void showForm() {
        state.hide();
        scrollView.setVisibility(View.VISIBLE);
        Provider p = provider;

        String photo = p.getProfileImage() != null ? p.getProfileImage() : user.getProfileImage();
        ImageUtils.loadAvatar(ivAvatar, photo, p.getName() != null ? p.getName() : user.getName());
        etName.setText(p.getName() != null ? p.getName() : user.getName());
        etPhone.setText(p.getPhone() != null ? p.getPhone() : user.getPhone());
        if (p.getTitle() != null) actTitle.setText(p.getTitle(), false);
        selectCategory(p.getCategory());
        skills.clear();
        skills.addAll(p.getSkills());
        renderSkills();
        if (p.getExperienceYears() > 0 || p.getTitle() != null) etExperience.setText(String.valueOf(p.getExperienceYears()));
        if (p.getStartingPrice() > 0) etPrice.setText(String.valueOf((int) p.getStartingPrice()));
        int unitIndex = Arrays.asList(priceUnits).indexOf(p.getPriceUnit());
        spinnerPriceUnit.setSelection(Math.max(0, unitIndex));
        etDescription.setText(p.getDescription());
        updateLocationLabel();

        List<String> days = p.getAvailableDays().isEmpty()
                ? Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat") : p.getAvailableDays();
        for (int i = 0; i < chipsDays.getChildCount(); i++) {
            Chip chip = (Chip) chipsDays.getChildAt(i);
            chip.setChecked(days.contains(chip.getText().toString()));
        }
        if (p.getAvailableFrom() != null) availableFrom = p.getAvailableFrom();
        if (p.getAvailableTo() != null) availableTo = p.getAvailableTo();
        etFrom.setText(DateTimeUtils.to12Hour(availableFrom));
        etTo.setText(DateTimeUtils.to12Hour(availableTo));
        switchAvailable.setChecked(p.isAvailable());

        ImageView ivVerification = findViewById(R.id.ivVerification);
        ivVerification.setColorFilter(ContextCompat.getColor(this, p.isVerified() ? R.color.info : R.color.text_hint));
        ((TextView) findViewById(R.id.tvVerification)).setText(p.isVerified() ? "Verified provider" : "Verification pending");

        if (getIntent().getBooleanExtra(EXTRA_SCROLL_TO_AVAILABILITY, false)) {
            View section = findViewById(R.id.sectionAvailability);
            scrollView.post(() -> scrollView.smoothScrollTo(0, section.getTop()));
        }
    }

    private void selectCategory(String categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(categoryId)) spinnerCategory.setSelection(i);
        }
    }

    // ------------------------------------------------------------ skills

    private void addSkillFromInput() {
        tilSkill.setError(null);
        String skill = etSkill.getText() != null ? etSkill.getText().toString().trim() : "";
        if (skill.length() < 2) {
            tilSkill.setError("Type a skill");
            return;
        }
        for (String s : skills) {
            if (s.equalsIgnoreCase(skill)) {
                tilSkill.setError("Already added");
                return;
            }
        }
        if (skills.size() >= MAX_SKILLS) {
            tilSkill.setError("You can add up to " + MAX_SKILLS + " skills");
            return;
        }
        skills.add(skill);
        etSkill.setText("");
        renderSkills();
    }

    private void renderSkills() {
        chipsSkills.removeAllViews();
        for (String skill : skills) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, chipsSkills, false);
            chip.setText(skill);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                skills.remove(skill);
                renderSkills();
            });
            chipsSkills.addView(chip);
        }
    }

    // ------------------------------------------------------------ time & location

    private void pickTime(boolean from) {
        int[] hm = DateTimeUtils.parseTime(from ? availableFrom : availableTo);
        new TimePickerDialog(this, (view, h, m) -> {
            String value = DateTimeUtils.formatTime24(h, m);
            if (from) {
                availableFrom = value;
                etFrom.setText(DateTimeUtils.to12Hour(value));
            } else {
                availableTo = value;
                etTo.setText(DateTimeUtils.to12Hour(value));
            }
        }, hm != null ? hm[0] : 9, hm != null ? hm[1] : 0, false).show();
    }

    private void useCurrentLocation() {
        UiUtils.toast(this, "Getting your location…");
        LocationUtils.fetchCurrentLocation(this, new LocationUtils.LocationListener() {
            @Override
            public void onLocation(double lat, double lng) {
                LocationUtils.reverseGeocode(EditProviderProfileActivity.this, lat, lng, address ->
                        setLocation(lat, lng, address != null ? address : "Current location"));
            }

            @Override
            public void onFailed(String message) {
                UiUtils.toast(EditProviderProfileActivity.this, message);
            }
        });
    }

    private void setLocation(double lat, double lng, String address) {
        provider.setLatitude(lat);
        provider.setLongitude(lng);
        provider.setAddress(address);
        updateLocationLabel();
    }

    private void updateLocationLabel() {
        tvLocationValue.setText(provider.hasLocation() && provider.getAddress() != null
                ? provider.getAddress() : "No service location set yet");
    }

    // ------------------------------------------------------------ save

    private void validateAndSave() {
        for (TextInputLayout t : new TextInputLayout[]{tilName, tilTitle, tilPhone, tilExperience, tilPrice, tilDescription}) {
            t.setError(null);
        }
        String name = text(etName);
        String title = actTitle.getText() != null ? actTitle.getText().toString().trim() : "";
        String phone = text(etPhone);
        int experience = ValidationUtils.parseExperience(text(etExperience));
        double price = ValidationUtils.parsePrice(text(etPrice));
        String description = text(etDescription);
        List<String> days = new ArrayList<>();
        for (int i = 0; i < chipsDays.getChildCount(); i++) {
            Chip chip = (Chip) chipsDays.getChildAt(i);
            if (chip.isChecked()) days.add(chip.getText().toString());
        }

        String firstError = null;
        if (!ValidationUtils.isValidName(name)) { tilName.setError("Enter your full name"); firstError = "name"; }
        if (title.length() < 2) { tilTitle.setError("Choose your main skill, e.g. Electrician"); firstError = "title"; }
        if (!ValidationUtils.isValidPhone(phone)) { tilPhone.setError("Enter a valid 10-digit mobile number"); firstError = "phone"; }
        if (experience < 0) { tilExperience.setError("Enter years of experience (0–60)"); firstError = "experience"; }
        if (price < 0) { tilPrice.setError("Enter a price between ₹1 and ₹1,00,000"); firstError = "price"; }
        if (description.length() < 20) { tilDescription.setError("Write at least 20 characters about your work"); firstError = "description"; }
        if (firstError != null) {
            UiUtils.toast(this, "Please fix the highlighted fields");
            return;
        }
        if (!provider.hasLocation() || provider.getAddress() == null) {
            UiUtils.toast(this, "Please set your service location");
            return;
        }
        if (days.isEmpty()) {
            UiUtils.toast(this, "Select at least one working day");
            return;
        }
        if (availableFrom.compareTo(availableTo) >= 0) {
            UiUtils.toast(this, "\"To\" time must be after \"From\" time");
            return;
        }
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }

        provider.setName(name);
        provider.setTitle(title);
        provider.setCategory(categories.get(spinnerCategory.getSelectedItemPosition()).getId());
        provider.setPhone(ValidationUtils.normalizePhone(phone));
        provider.setEmail(user.getEmail());
        provider.setSkills(new ArrayList<>(skills));
        provider.setExperienceYears(experience);
        provider.setStartingPrice(price);
        provider.setPriceUnit(priceUnits[spinnerPriceUnit.getSelectedItemPosition()]);
        provider.setDescription(description);
        provider.setAvailableDays(days);
        provider.setAvailableFrom(availableFrom);
        provider.setAvailableTo(availableTo);
        provider.setAvailable(switchAvailable.isChecked());

        setLoading(true);
        if (newPhotoUri != null) {
            repo.uploadProfileImage(this, newPhotoUri, new Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    provider.setProfileImage(url);
                    saveProvider();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    UiUtils.toast(EditProviderProfileActivity.this, message);
                }
            });
        } else {
            if (provider.getProfileImage() == null) provider.setProfileImage(user.getProfileImage());
            saveProvider();
        }
    }

    private void saveProvider() {
        repo.saveProviderProfile(provider, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Keep the user document in sync (name, phone, photo, location)
                user.setName(provider.getName());
                user.setPhone(provider.getPhone());
                user.setProfileImage(provider.getProfileImage());
                user.setAddress(provider.getAddress());
                user.setLatitude(provider.getLatitude());
                user.setLongitude(provider.getLongitude());
                repo.updateUser(user, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        finishSave();
                    }

                    @Override
                    public void onError(String message) {
                        finishSave();
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                UiUtils.toast(EditProviderProfileActivity.this, message);
            }
        });
    }

    private void finishSave() {
        setLoading(false);
        UiUtils.toast(this, "Profile updated successfully");
        if (firstSetup) {
            Intent i = new Intent(this, ProviderHomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
