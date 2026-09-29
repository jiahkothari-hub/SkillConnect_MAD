/**
 * OPTIONAL Cloud Functions for SkillConnect push notifications (FCM).
 * The Android app already saves each user's FCM token in users/{uid}.fcmToken and
 * shows local notifications while it is running. Deploy these functions to also
 * receive notifications when the app is closed.
 *
 *   cd firebase/functions && npm install && firebase deploy --only functions
 */
const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
admin.initializeApp();

async function notify(userId, title, body, bookingId) {
  const user = await admin.firestore().doc(`users/${userId}`).get();
  const token = user.exists ? user.get('fcmToken') : null;
  if (!token) return;
  await admin.messaging().send({ token, data: { title, body, bookingId } });
}

// Provider: "You have a new service request."
exports.onBookingCreated = onDocumentCreated('bookings/{bookingId}', async (event) => {
  const b = event.data.data();
  await notify(b.providerId, 'You have a new service request',
    `${b.customerName} requested ${b.service} on ${b.date} at ${b.time}.`, event.params.bookingId);
});

// Customer: "Your booking request was accepted." (and other status changes)
exports.onBookingUpdated = onDocumentUpdated('bookings/{bookingId}', async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (before.status === after.status) return;
  const id = event.params.bookingId;
  if (after.status === 'ACCEPTED') {
    await notify(after.customerId, 'Your booking request was accepted',
      `${after.providerName} accepted your ${after.service} request for ${after.date} at ${after.time}.`, id);
  } else if (after.status === 'REJECTED') {
    await notify(after.customerId, 'Booking request declined',
      `${after.providerName} can't take your ${after.service} request.`, id);
  } else if (after.status === 'COMPLETED') {
    await notify(after.customerId, 'How was your experience?', `Rate ${after.providerName} for ${after.service}.`, id);
  } else if (after.status === 'CANCELLED') {
    await notify(after.providerId, 'Booking cancelled', `${after.customerName} cancelled ${after.service}.`, id);
  }
});
