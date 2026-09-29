package com.skillconnect.app.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.skillconnect.app.R;
import com.skillconnect.app.activities.BookingsHost;
import com.skillconnect.app.activities.EditProfileActivity;
import com.skillconnect.app.activities.EditProviderProfileActivity;
import com.skillconnect.app.activities.LocationPickerActivity;
import com.skillconnect.app.activities.PortfolioActivity;
import com.skillconnect.app.activities.SettingsActivity;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.DateTimeUtils;
import com.skillconnect.app.utils.FileUtils;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/** Profile tab for both roles. Providers see their professional details and extra tools. */
public class ProfileFragment extends Fragment {

    private AppRepository repo;
    private User user;
    private Provider provider;
    private View root;

    /** Result of LocationPickerActivity (explicit intent + activity result). */
    private final ActivityResultLauncher<Intent> locationPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || provider == null) return;
                Intent data = result.getData();
                provider.setLatitude(data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0));
                provider.setLongitude(data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0));
                provider.setAddress(data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS));
                repo.saveProviderProfile(provider, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!isAdded()) return;
                        UiUtils.toast(requireContext(), "Service location updated");
                        bindProvider();
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) UiUtils.toast(requireContext(), message);
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        root = view;
        repo = RepositoryProvider.get();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (root != null && !isHidden()) refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && root != null) refresh();
    }

    private void refresh() {
        user = repo.getCachedUser();
        if (user == null) return;
        bindUser();
        setupMenu();
        if (user.isProvider()) loadProvider();
    }

    private void bindUser() {
        ImageUtils.loadAvatar(root.findViewById(R.id.ivAvatar), user.getProfileImage(), user.getName());
        ((TextView) root.findViewById(R.id.tvName)).setText(user.getName());
        ((TextView) root.findViewById(R.id.tvEmail)).setText(user.getEmail());
        ((TextView) root.findViewById(R.id.tvRole)).setText(user.isProvider() ? "Service Provider"
                : user.isAdmin() ? "Admin" : "Customer");

        String location = user.getAddress();
        if (location == null || location.isEmpty()) location = PrefsManager.get(requireContext()).getLocality();
        row(R.id.rowPhone, R.drawable.ic_phone, "Phone", user.getPhone() != null ? "+91 " + user.getPhone() : null, true);
        row(R.id.rowLocation, R.drawable.ic_location, "Location", location, true);

        boolean isProvider = user.isProvider();
        for (int id : new int[]{R.id.rowProfession, R.id.rowExperience, R.id.rowRating, R.id.rowAvailability, R.id.rowVerification}) {
            root.findViewById(id).setVisibility(isProvider ? View.VISIBLE : View.GONE);
        }
        root.findViewById(R.id.chipsSkills).setVisibility(View.GONE);

        ((TextView) root.findViewById(R.id.tvDataMode)).setText(RepositoryProvider.isDemoMode()
                ? "Demo mode · sample data stored on this device"
                : "Connected to Firebase · your data is synced to the cloud");
    }

    private void loadProvider() {
        repo.getProvider(user.getUserId(), new Callback<Provider>() {
            @Override
            public void onSuccess(Provider result) {
                if (!isAdded()) return;
                provider = result;
                bindProvider();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) UiUtils.toast(requireContext(), message);
            }
        });
    }

    private void bindProvider() {
        Provider p = provider;
        row(R.id.rowProfession, R.drawable.ic_work, "Profession",
                p.getTitle() != null ? p.getTitle() + " · " + CategoryData.getCategoryName(p.getCategory()) : null, true);
        row(R.id.rowExperience, R.drawable.ic_history, "Experience",
                p.getExperienceYears() + (p.getExperienceYears() == 1 ? " year" : " years"), true);
        row(R.id.rowRating, R.drawable.ic_star, "Rating", p.getReviewCount() > 0
                ? UiUtils.formatRating(p.getAverageRating()) + " (" + UiUtils.reviewCount(p.getReviewCount()) + ")" : "No reviews yet", true);
        row(R.id.rowAvailability, R.drawable.ic_calendar, "Availability",
                DateTimeUtils.summarizeDays(p.getAvailableDays()) + ", " + DateTimeUtils.hoursRange(p.getAvailableFrom(), p.getAvailableTo()), true);
        row(R.id.rowVerification, R.drawable.ic_verified, "Status", p.isVerified() ? "Verified provider" : "Verification pending", true);
        if (p.getAddress() != null) row(R.id.rowLocation, R.drawable.ic_location, "Location", p.getAddress(), true);

        ChipGroup chips = root.findViewById(R.id.chipsSkills);
        chips.removeAllViews();
        for (String skill : p.getSkills()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_action, chips, false);
            chip.setText(skill);
            chip.setClickable(false);
            chips.addView(chip);
        }
        chips.setVisibility(p.getSkills().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void row(int rowId, int icon, String label, String value, boolean visible) {
        View row = root.findViewById(rowId);
        row.setVisibility(visible ? View.VISIBLE : View.GONE);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value != null && !value.isEmpty() ? value : "Not set");
    }

    // ------------------------------------------------------------------ menu

    private void setupMenu() {
        boolean isProvider = user.isProvider();
        boolean isCustomer = user.isCustomer();
        BookingsHost host = getActivity() instanceof BookingsHost ? (BookingsHost) getActivity() : null;

        menu(R.id.menuEditProfile, R.drawable.ic_edit, getString(R.string.edit_profile), true, () ->
                startActivity(new Intent(requireContext(), isProvider ? EditProviderProfileActivity.class : EditProfileActivity.class)));
        menu(R.id.menuPortfolio, R.drawable.ic_image, getString(R.string.manage_portfolio), isProvider, () ->
                startActivity(new Intent(requireContext(), PortfolioActivity.class)));
        menu(R.id.menuAvailability, R.drawable.ic_time, getString(R.string.manage_availability), isProvider, () -> {
            Intent i = new Intent(requireContext(), EditProviderProfileActivity.class);
            i.putExtra(EditProviderProfileActivity.EXTRA_SCROLL_TO_AVAILABILITY, true);
            startActivity(i);
        });
        menu(R.id.menuLocation, R.drawable.ic_location, getString(R.string.service_location), isProvider, () -> {
            if (provider == null) return;
            Intent i = new Intent(requireContext(), LocationPickerActivity.class);
            i.putExtra(LocationPickerActivity.EXTRA_LAT, provider.getLatitude());
            i.putExtra(LocationPickerActivity.EXTRA_LNG, provider.getLongitude());
            i.putExtra(LocationPickerActivity.EXTRA_ADDRESS, provider.getAddress());
            locationPicker.launch(i);
        });
        menu(R.id.menuBookings, R.drawable.ic_calendar, isProvider ? getString(R.string.my_jobs) : getString(R.string.my_bookings),
                host != null, () -> host.selectTab(isProvider ? R.id.nav_jobs : R.id.nav_bookings));
        menu(R.id.menuSaved, R.drawable.ic_favorite_border, getString(R.string.saved_providers), isCustomer && host != null,
                () -> host.selectTab(R.id.nav_saved));
        menu(R.id.menuExportBookings, R.drawable.ic_download,
                isProvider ? getString(R.string.export_jobs) : getString(R.string.export_bookings), host != null,
                () -> exportBookings(host, isProvider ? "job_history.csv" : "booking_history.csv"));
        menu(R.id.menuExportProfile, R.drawable.ic_file, getString(R.string.export_profile), isProvider, () -> {
            if (provider == null) return;
            FileUtils.exportAndPreview(requireActivity(), "provider_profile.txt", FileUtils.MIME_TXT,
                    FileUtils.buildProviderProfileTxt(provider));
        });
        menu(R.id.menuShareProfile, R.drawable.ic_share, getString(R.string.share_profile), isProvider, this::shareProfile);
        menu(R.id.menuSettings, R.drawable.ic_settings, getString(R.string.settings), true, () ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        menu(R.id.menuAdmin, R.drawable.ic_shield, getString(R.string.admin_panel), false, null);
        menu(R.id.menuLogout, R.drawable.ic_logout, getString(R.string.logout), true, () ->
                NavigationUtils.confirmLogout(requireActivity()));

        // Logout in red
        View logout = root.findViewById(R.id.menuLogout);
        ImageView icon = logout.findViewById(R.id.menuIcon);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error));
        icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_light)));
        ((TextView) logout.findViewById(R.id.menuTitle)).setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        logout.findViewById(R.id.menuChevron).setVisibility(View.GONE);
    }

    private void menu(int id, int icon, String title, boolean visible, Runnable action) {
        View row = root.findViewById(id);
        row.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        ((ImageView) row.findViewById(R.id.menuIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.menuTitle)).setText(title);
        row.setOnClickListener(v -> action.run());
    }

    private void exportBookings(BookingsHost host, String fileName) {
        List<Booking> bookings = host.getLatestBookings();
        if (bookings == null || bookings.isEmpty()) {
            UiUtils.toast(requireContext(), "No bookings to export yet");
            return;
        }
        FileUtils.exportAndPreview(requireActivity(), fileName, FileUtils.MIME_CSV, FileUtils.buildBookingsCsv(bookings));
    }

    private void shareProfile() {
        if (provider == null) return;
        String text = "Hi! I'm " + provider.getName() + ", a " + provider.getTitle() + " on SkillConnect.\n"
                + UiUtils.startingPrice(provider.getStartingPrice()) + " " + provider.getPriceUnit()
                + (provider.getAddress() != null ? " · " + provider.getAddress() : "") + "\n"
                + "Book me on the SkillConnect app!";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.share_profile)));
    }
}
