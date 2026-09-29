package com.skillconnect.app.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.skillconnect.app.firebase.FirestoreHelper;

/**
 * Firebase Cloud Messaging entry point. Push messages are sent by the optional
 * Cloud Function in firebase/functions (see README). Data payload keys:
 * title, body, bookingId.
 */
public class SkillConnectMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection(FirestoreHelper.USERS).document(uid).update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String bookingId = message.getData().get("bookingId");
        if (message.getNotification() != null) {
            if (title == null) title = message.getNotification().getTitle();
            if (body == null) body = message.getNotification().getBody();
        }
        if (title != null && body != null) {
            NotificationHelper.show(this, title, body, bookingId);
        }
    }
}
