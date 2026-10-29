package com.skillconnect.app.firebase;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;

/** Converts Firebase exceptions into short messages a user can understand. */
public final class FirebaseErrors {

    public static final String NETWORK = "Something went wrong. Check your connection and try again.";
    /**
     * Firebase Authentication works but Cloud Firestore can't be reached. With a working internet
     * connection this almost always means the Firestore database hasn't been created for the project.
     */
    public static final String DATABASE_UNREACHABLE = "Can't reach the SkillConnect database. Check your internet "
            + "connection. If it's working, Cloud Firestore may not be set up yet (Firebase console → "
            + "Firestore Database → Create database).";

    private FirebaseErrors() {
    }

    public static String message(Exception e) {
        if (e == null) return NETWORK;
        if (e instanceof FirebaseNetworkException) return NETWORK;
        if (e instanceof FirebaseAuthWeakPasswordException) return "Please choose a stronger password (at least 6 characters).";
        if (e instanceof FirebaseAuthInvalidUserException) return "No active account found with this email.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Incorrect email or password.";
        if (e instanceof FirebaseAuthUserCollisionException) return "An account with this email already exists.";
        if (e instanceof StorageException) return "Image upload failed. Please check your connection and try again.";
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "You don't have permission to do that.";
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return DATABASE_UNREACHABLE;
                case FAILED_PRECONDITION:
                case ABORTED:
                    return e.getMessage() != null ? e.getMessage() : NETWORK;
                default:
                    break;
            }
        }
        return e.getMessage() != null ? e.getMessage() : NETWORK;
    }
}
