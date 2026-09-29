package com.skillconnect.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.PortfolioAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.ImagePickerHelper;
import com.skillconnect.app.utils.ImagePreviewDialog;
import com.skillconnect.app.utils.ImageUtils;
import com.skillconnect.app.utils.StateView;
import com.skillconnect.app.utils.UiUtils;

import java.util.List;

/**
 * Provider portfolio manager. Photos come from the camera or gallery (implicit intents),
 * are previewed, then uploaded to Firebase Storage; the URL is saved in Firestore.
 */
public class PortfolioActivity extends AppCompatActivity {

    private static final int MAX_ITEMS = 24;

    private AppRepository repo;
    private ImagePickerHelper imagePicker;
    private PortfolioAdapter adapter;
    private StateView state;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fabAdd;
    private int placeholderIcon = R.drawable.ic_image;
    private int itemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);
        repo = RepositoryProvider.get();
        imagePicker = new ImagePickerHelper(this, this::showUploadPreview);
        imagePicker.restoreState(savedInstanceState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        state = new StateView(findViewById(R.id.portfolioState));
        recyclerView = findViewById(R.id.rvPortfolio);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            if (itemCount >= MAX_ITEMS) UiUtils.toast(this, "You can add up to " + MAX_ITEMS + " photos");
            else imagePicker.showSourceDialog();
        });

        // Use the provider's category icon for demo placeholders
        repo.getProvider(repo.getCurrentUserId(), new Callback<Provider>() {
            @Override
            public void onSuccess(Provider p) {
                placeholderIcon = CategoryData.getCategoryIcon(p.getCategory());
                setupAdapter();
                load();
            }

            @Override
            public void onError(String message) {
                setupAdapter();
                load();
            }
        });
        state.showLoading(getString(R.string.loading));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        imagePicker.saveState(outState);
    }

    private void setupAdapter() {
        adapter = new PortfolioAdapter(true, placeholderIcon, new PortfolioAdapter.Listener() {
            @Override
            public void onItemClick(PortfolioItem item) {
                ImagePreviewDialog.show(PortfolioActivity.this, item, placeholderIcon);
            }

            @Override
            public void onItemLongClick(PortfolioItem item) {
                confirmDelete(item);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void load() {
        repo.getPortfolio(repo.getCurrentUserId(), new Callback<List<PortfolioItem>>() {
            @Override
            public void onSuccess(List<PortfolioItem> items) {
                itemCount = items.size();
                adapter.submit(items);
                if (items.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    state.showEmpty(R.drawable.ic_image, "No portfolio photos yet",
                            "Profiles with photos of real work get more bookings.",
                            getString(R.string.add_photo), () -> imagePicker.showSourceDialog());
                } else {
                    state.hide();
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                recyclerView.setVisibility(View.GONE);
                state.showError(message, PortfolioActivity.this::load);
            }
        });
    }

    /** Preview the picked image and ask for an optional description before uploading. */
    private void showUploadPreview(Uri uri) {
        View view = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView preview = view.findViewById(R.id.ivPreview);
        ImageUtils.loadImage(preview, uri);
        view.findViewById(R.id.tvPreviewCaption).setVisibility(View.GONE);
        view.findViewById(R.id.tilPreviewDescription).setVisibility(View.VISIBLE);
        TextInputEditText etDescription = view.findViewById(R.id.etPreviewDescription);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Add to portfolio")
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.upload, (d, w) -> {
                    String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
                    upload(uri, description);
                })
                .create();
        dialog.show();
    }

    private void upload(Uri uri, String description) {
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        fabAdd.setEnabled(false);
        fabAdd.setText("Uploading…");
        repo.addPortfolioItem(this, uri, description, new Callback<PortfolioItem>() {
            @Override
            public void onSuccess(PortfolioItem item) {
                resetFab();
                UiUtils.toast(PortfolioActivity.this, "Photo added to your portfolio");
                load();
            }

            @Override
            public void onError(String message) {
                resetFab();
                UiUtils.toast(PortfolioActivity.this, message);
            }
        });
    }

    private void resetFab() {
        fabAdd.setEnabled(true);
        fabAdd.setText(R.string.add_photo);
    }

    private void confirmDelete(PortfolioItem item) {
        UiUtils.confirm(this, getString(R.string.delete_portfolio_confirm),
                "This photo will be removed from your public profile.", getString(R.string.delete), () ->
                        repo.deletePortfolioItem(item, new Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                UiUtils.toast(PortfolioActivity.this, "Photo deleted");
                                load();
                            }

                            @Override
                            public void onError(String message) {
                                UiUtils.toast(PortfolioActivity.this, message);
                            }
                        }));
    }
}
