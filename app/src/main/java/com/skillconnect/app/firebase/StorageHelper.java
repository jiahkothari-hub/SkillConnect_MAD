package com.skillconnect.app.firebase;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.skillconnect.app.data.Callback;

/** Uploads/deletes image files in Firebase Storage. */
public class StorageHelper {

    public static String profileImagePath(String userId) {
        return "profile_images/" + userId + ".jpg";
    }

    public static String portfolioImagePath(String providerId, String portfolioId) {
        return "portfolio/" + providerId + "/" + portfolioId + ".jpg";
    }

    /** Uploads JPEG bytes and returns the public download URL. */
    public void uploadImage(String path, byte[] data, Callback<String> callback) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
        ref.putBytes(data, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void delete(String path) {
        if (path == null || path.isEmpty()) return;
        FirebaseStorage.getInstance().getReference().child(path).delete();
    }
}
