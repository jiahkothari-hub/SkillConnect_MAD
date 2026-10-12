package com.skillconnect.app.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.skillconnect.app.data.Callback;

/** Thin wrapper around Firebase Authentication (email + password). */
public class FirebaseAuthHelper {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void login(String email, String password, Callback<String> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void register(String email, String password, Callback<String> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void sendPasswordReset(String email, Callback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(FirebaseErrors.message(e)));
    }

    public void logout() {
        auth.signOut();
    }
}
