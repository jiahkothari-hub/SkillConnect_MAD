package com.skillconnect.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;

import java.io.File;

/**
 * Lets the user pick a photo using IMPLICIT INTENTS:
 * <ul>
 *   <li>Camera: MediaStore.ACTION_IMAGE_CAPTURE (built-in camera app)</li>
 *   <li>Gallery: Intent.ACTION_GET_CONTENT with type image/*</li>
 * </ul>
 * The CAMERA runtime permission is requested before opening the camera.
 * Create this in the Activity's onCreate().
 */
public class ImagePickerHelper {

    public interface Listener {
        void onImagePicked(Uri uri);
    }

    private static final String STATE_CAMERA_URI = "picker_camera_uri";

    private final AppCompatActivity activity;
    private final Listener listener;
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<Intent> cameraLauncher;
    private final ActivityResultLauncher<Intent> galleryLauncher;
    private final ActivityResultLauncher<String> permissionLauncher;

    public ImagePickerHelper(AppCompatActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;

        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && pendingCameraUri != null) {
                        listener.onImagePicked(pendingCameraUri);
                    }
                });

        galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                            && result.getData().getData() != null) {
                        listener.onImagePicked(result.getData().getData());
                    }
                });

        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) launchCamera();
                    else UiUtils.toast(activity, "Camera permission is needed to take photos. You can still choose from the gallery.");
                });
    }

    /** Dialog: Take photo / Choose from gallery. */
    public void showSourceDialog() {
        String[] options = {activity.getString(R.string.take_photo), activity.getString(R.string.choose_gallery)};
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Add a photo")
                .setItems(options, (d, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Camera access")
                    .setMessage(R.string.camera_rationale)
                    .setNegativeButton(R.string.not_now, null)
                    .setPositiveButton(R.string.allow, (d, w) -> permissionLauncher.launch(Manifest.permission.CAMERA))
                    .show();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = new File(activity.getFilesDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create images folder");
            File photo = new File(dir, "IMG_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", photo);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(activity, "No camera app found on this device");
        } catch (Exception e) {
            UiUtils.toast(activity, "Couldn't open the camera");
        }
    }

    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            galleryLauncher.launch(Intent.createChooser(intent, "Select a photo"));
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(activity, "No gallery app found on this device");
        }
    }

    public void saveState(Bundle out) {
        if (pendingCameraUri != null) out.putString(STATE_CAMERA_URI, pendingCameraUri.toString());
    }

    public void restoreState(Bundle in) {
        if (in != null && in.getString(STATE_CAMERA_URI) != null) {
            pendingCameraUri = Uri.parse(in.getString(STATE_CAMERA_URI));
        }
    }
}
