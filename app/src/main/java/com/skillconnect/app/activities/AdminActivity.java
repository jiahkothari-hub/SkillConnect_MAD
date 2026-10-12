package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.AdminAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-app admin panel: view/deactivate provider listings, verify providers,
 * view/deactivate users, view bookings, view categories and seed demo data.
 */
public class AdminActivity extends AppCompatActivity {

    private static final int TAB_PROVIDERS = 0;
    private static final int TAB_USERS = 1;
    private static final int TAB_BOOKINGS = 2;
    private static final int TAB_CATEGORIES = 3;

    private AppRepository repo;
    private AdminAdapter adapter;
    private StateView state;
    private RecyclerView recyclerView;
    private TextView tvSummary;
    private int currentTab = TAB_PROVIDERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = RepositoryProvider.get();
        User me = repo.getCachedUser();
        if (me == null || !me.isAdmin()) {
            NavigationUtils.goToLogin(this);
            return;
        }
        setContentView(R.layout.activity_admin);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_admin);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_seed) {
                confirmSeed();
                return true;
            } else if (item.getItemId() == R.id.action_logout) {
                NavigationUtils.confirmLogout(this);
                return true;
            }
            return false;
        });

        state = new StateView(findViewById(R.id.adminState));
        recyclerView = findViewById(R.id.rvAdmin);
        tvSummary = findViewById(R.id.tvSummary);
        adapter = new AdminAdapter(new AdminAdapter.Listener() {
            @Override
            public void onRowClick(AdminAdapter.Row row) {
                openRow(row);
            }

            @Override
            public void onAction(AdminAdapter.Row row) {
                primaryAction(row);
            }

            @Override
            public void onAction2(AdminAdapter.Row row) {
                secondaryAction(row);
            }
        });
        recyclerView.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.tabs);
        for (String t : new String[]{"Providers", "Users", "Bookings", "Categories"}) tabs.addTab(tabs.newTab().setText(t));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                load();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                load();
            }
        });
        load();
    }

    private void load() {
        recyclerView.setVisibility(View.GONE);
        tvSummary.setText("");
        state.showLoading(getString(R.string.loading));
        switch (currentTab) {
            case TAB_USERS:
                loadUsers();
                break;
            case TAB_BOOKINGS:
                loadBookings();
                break;
            case TAB_CATEGORIES:
                loadCategories();
                break;
            default:
                loadProviders();
                break;
        }
    }

    private <T> Callback<List<T>> listCallback(Converter<T> converter, String noun) {
        return new Callback<List<T>>() {
            @Override
            public void onSuccess(List<T> items) {
                List<AdminAdapter.Row> rows = new ArrayList<>();
                for (T item : items) rows.add(converter.toRow(item));
                show(rows, noun);
            }

            @Override
            public void onError(String message) {
                state.showError(message, AdminActivity.this::load);
            }
        };
    }

    private interface Converter<T> {
        AdminAdapter.Row toRow(T item);
    }

    private void show(List<AdminAdapter.Row> rows, String noun) {
        adapter.submit(rows);
        tvSummary.setText(rows.size() + " " + noun);
        if (rows.isEmpty()) {
            state.showEmpty(R.drawable.ic_inbox, "Nothing here yet", null);
        } else {
            state.hide();
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ------------------------------------------------------------------ tabs

    private void loadProviders() {
        repo.getProviders(true, listCallback((Provider p) -> {
            AdminAdapter.Row r = new AdminAdapter.Row();
            r.title = p.getName() != null ? p.getName() : "(no name)";
            r.subtitle = (p.getTitle() != null ? p.getTitle() : "Profile not set up") + " · "
                    + (p.getAddress() != null ? p.getAddress() : "No location") + " · ★ " + UiUtils.formatRating(p.getAverageRating());
            r.avatarUrl = p.getProfileImage();
            r.meta = p.isActive() ? (p.isVerified() ? "Verified" : "Active") : "Deactivated";
            r.metaTextColor = p.isActive() ? (p.isVerified() ? R.color.info : R.color.success) : R.color.error;
            r.metaBgColor = p.isActive() ? (p.isVerified() ? R.color.info_light : R.color.success_light) : R.color.error_light;
            r.action = p.isActive() ? "Deactivate" : "Activate";
            r.action2 = p.isVerified() ? "Remove badge" : "Verify";
            r.payload = p;
            return r;
        }, "provider listings"));
    }

    private void loadUsers() {
        String myId = repo.getCurrentUserId();
        repo.getAllUsers(listCallback((User u) -> {
            AdminAdapter.Row r = new AdminAdapter.Row();
            r.title = u.getName();
            r.subtitle = u.getEmail() + " · " + u.getRole();
            r.avatarUrl = u.getProfileImage();
            r.meta = u.isActive() ? "Active" : "Deactivated";
            r.metaTextColor = u.isActive() ? R.color.success : R.color.error;
            r.metaBgColor = u.isActive() ? R.color.success_light : R.color.error_light;
            r.action = u.getUserId().equals(myId) || u.isAdmin() ? null : (u.isActive() ? "Deactivate" : "Activate");
            r.payload = u;
            return r;
        }, "users"));
    }

    private void loadBookings() {
        repo.getAllBookings(new Callback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                List<Booking> sorted = new ArrayList<>(bookings);
                Collections.sort(sorted, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                List<AdminAdapter.Row> rows = new ArrayList<>();
                for (Booking b : sorted) {
                    AdminAdapter.Row r = new AdminAdapter.Row();
                    r.title = b.getService();
                    r.subtitle = b.getCustomerName() + " → " + b.getProviderName() + " · " + b.getDate() + " " + b.getTime();
                    r.avatarName = b.getService();
                    r.meta = UiUtils.statusLabel(b.getStatus());
                    r.metaTextColor = Booking.STATUS_COMPLETED.equals(b.getStatus()) ? R.color.success
                            : b.isClosed() ? R.color.error : Booking.STATUS_PENDING.equals(b.getStatus()) ? R.color.warning : R.color.info;
                    r.metaBgColor = Booking.STATUS_COMPLETED.equals(b.getStatus()) ? R.color.success_light
                            : b.isClosed() ? R.color.error_light : Booking.STATUS_PENDING.equals(b.getStatus()) ? R.color.warning_light : R.color.info_light;
                    r.payload = b;
                    rows.add(r);
                }
                show(rows, "bookings");
            }

            @Override
            public void onError(String message) {
                state.showError(message, AdminActivity.this::load);
            }
        });
    }

    private void loadCategories() {
        repo.getProviders(true, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> providers) {
                List<AdminAdapter.Row> rows = new ArrayList<>();
                for (Category c : CategoryData.getMainCategories()) {
                    AdminAdapter.Row r = new AdminAdapter.Row();
                    r.title = c.getName();
                    r.subtitle = TextUtils.join(", ", CategoryData.getSkills(c.getId()));
                    r.meta = CategoryData.countForCategory(providers, c.getId()) + " providers";
                    r.payload = c;
                    rows.add(r);
                }
                show(rows, "categories");
            }

            @Override
            public void onError(String message) {
                state.showError(message, AdminActivity.this::load);
            }
        });
    }

    // ------------------------------------------------------------------ actions

    private void openRow(AdminAdapter.Row row) {
        if (row.payload instanceof Provider) {
            Intent i = new Intent(this, ProviderDetailsActivity.class);
            i.putExtra(ProviderDetailsActivity.EXTRA_PROVIDER_ID, ((Provider) row.payload).getProviderId());
            startActivity(i);
        } else if (row.payload instanceof Booking) {
            Intent i = new Intent(this, BookingDetailsActivity.class);
            i.putExtra(BookingDetailsActivity.EXTRA_BOOKING_ID, ((Booking) row.payload).getBookingId());
            startActivity(i);
        } else if (row.payload instanceof Category) {
            Intent i = new Intent(this, ProviderListActivity.class);
            i.putExtra(ProviderListActivity.EXTRA_TITLE, ((Category) row.payload).getName());
            i.putExtra(ProviderListActivity.EXTRA_CATEGORY_ID, ((Category) row.payload).getId());
            startActivity(i);
        }
    }

    private final Callback<Void> reloadCallback = new Callback<Void>() {
        @Override
        public void onSuccess(Void result) {
            UiUtils.toast(AdminActivity.this, "Updated");
            load();
        }

        @Override
        public void onError(String message) {
            UiUtils.toast(AdminActivity.this, message);
        }
    };

    private void primaryAction(AdminAdapter.Row row) {
        if (row.payload instanceof Provider) {
            Provider p = (Provider) row.payload;
            boolean activate = !p.isActive();
            UiUtils.confirm(this, activate ? "Activate listing?" : "Deactivate listing?",
                    activate ? p.getName() + " will appear in search again."
                            : p.getName() + " will be hidden from search and cannot receive new bookings.",
                    activate ? "Activate" : "Deactivate",
                    () -> repo.setProviderActive(p.getProviderId(), activate, reloadCallback));
        } else if (row.payload instanceof User) {
            User u = (User) row.payload;
            boolean activate = !u.isActive();
            UiUtils.confirm(this, activate ? "Activate user?" : "Deactivate user?",
                    activate ? u.getName() + " will be able to log in again." : u.getName() + " will not be able to log in.",
                    activate ? "Activate" : "Deactivate",
                    () -> repo.setUserActive(u.getUserId(), activate, reloadCallback));
        }
    }

    private void secondaryAction(AdminAdapter.Row row) {
        if (row.payload instanceof Provider) {
            Provider p = (Provider) row.payload;
            repo.setProviderVerified(p.getProviderId(), !p.isVerified(), reloadCallback);
        }
    }

    private void confirmSeed() {
        if (RepositoryProvider.isDemoMode()) {
            UiUtils.toast(this, "You're in demo mode – sample data is already loaded. Seeding writes to Firebase.");
            return;
        }
        UiUtils.confirm(this, "Seed demo data?",
                "This writes the fictional sample providers, portfolio items, reviews and categories to your Firestore database.",
                "Seed", () -> {
                    state.showLoading("Uploading sample data…");
                    repo.seedDemoData(new Callback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            UiUtils.toast(AdminActivity.this, count + " sample providers added to Firestore");
                            load();
                        }

                        @Override
                        public void onError(String message) {
                            UiUtils.toast(AdminActivity.this, message);
                            load();
                        }
                    });
                });
    }
}
