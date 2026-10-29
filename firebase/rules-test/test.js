// Replays the SkillConnect app's Firestore operations against the rules in the Firestore emulator.
const fs = require('fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');
const FieldValue = firebase.firestore.FieldValue;

const RULES = process.argv[2];
let pass = 0, fail = 0;
async function check(name, expectOk, fn) {
  try {
    await (expectOk ? assertSucceeds(fn()) : assertFails(fn()));
    pass++; console.log('  ok   ' + name);
  } catch (e) {
    fail++; console.log('  FAIL ' + name + ' -> ' + (e.message || e).split('\n')[0]);
  }
}

(async () => {
  const env = await initializeTestEnvironment({
    projectId: 'demo-skillconnect',
    firestore: { rules: fs.readFileSync(RULES, 'utf8'), host: '127.0.0.1', port: 8080 },
  });
  const now = Date.now();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const user = (id, role) => db.doc(`users/${id}`).set({ userId: id, name: id, email: id + '@x.com', role, active: true });
    await user('cust1', 'CUSTOMER'); await user('cust2', 'CUSTOMER'); await user('prov1', 'PROVIDER'); await user('admin', 'ADMIN');
    // Existing listing from before this change: no ratingTotal yet (rules fall back to average × count)
    await db.doc('providers/prov1').set({ providerId: 'prov1', name: 'Rohan', title: 'Electrician', active: true, verified: false,
      averageRating: 4.0, reviewCount: 2, available: true });
    const booking = (id, status, customer) => db.doc(`bookings/${id}`).set({ bookingId: id, customerId: customer, providerId: 'prov1',
      service: 'Wiring', status, reviewed: false, price: 500, createdAt: now, updatedAt: now });
    await booking('b1', 'COMPLETED', 'cust1'); await booking('b2', 'PENDING', 'cust1'); await booking('b3', 'COMPLETED', 'cust1');
    await booking('b4', 'COMPLETED', 'cust1');
  });

  const as = (uid) => env.authenticatedContext(uid).firestore();
  const cust1 = as('cust1'), cust2 = as('cust2'), prov1 = as('prov1'), admin = as('admin');

  console.log('Registration and provider setup');
  const newProv = as('newprov');
  await check('new user creates own users doc as PROVIDER', true, () => newProv.doc('users/newprov').set(
    { userId: 'newprov', name: 'Asha', email: 'a@x.com', phone: '9876543210', role: 'PROVIDER', active: true, createdAt: now }));
  await check('user cannot register as ADMIN', false, () => as('evil').doc('users/evil').set(
    { userId: 'evil', role: 'ADMIN', active: true }));
  await check('provider setup creates listing (merge, first save)', true, () => newProv.doc('providers/newprov').set(
    { providerId: 'newprov', name: 'Asha', title: 'Tutor', category: 'education', startingPrice: 400, latitude: 19.1, longitude: 72.8,
      address: 'Andheri', active: true, verified: false, averageRating: 0.0, reviewCount: 0, ratingTotal: 0.0, createdAt: now }, { merge: true }));
  await check('provider edits own profile', true, () => newProv.doc('providers/newprov').set({ startingPrice: 450 }, { merge: true }));
  await check('provider cannot verify themselves', false, () => newProv.doc('providers/newprov').update({ verified: true }));
  await check('provider cannot set own rating', false, () => newProv.doc('providers/newprov').update({ averageRating: 5 }));
  await check('user cannot change own role', false, () => newProv.doc('users/newprov').update({ role: 'ADMIN' }));
  await check('user saves FCM token', true, () => newProv.doc('users/newprov').update({ fcmToken: 'tok' }));
  await check('user cannot read another user', false, () => cust2.doc('users/cust1').get());

  console.log('Bookings');
  await check('customer creates booking', true, () => cust1.doc('bookings/b9').set({ bookingId: 'b9', customerId: 'cust1',
    providerId: 'prov1', service: 'Fan', status: 'PENDING', reviewed: false, price: 300, createdAt: now, updatedAt: now }));
  await check('customer cannot create booking as ACCEPTED', false, () => cust1.doc('bookings/b10').set({ bookingId: 'b10',
    customerId: 'cust1', providerId: 'prov1', status: 'ACCEPTED', reviewed: false }));
  await check('customer lists own bookings', true, () => cust1.collection('bookings').where('customerId', '==', 'cust1').get());
  await check('provider lists own bookings', true, () => prov1.collection('bookings').where('providerId', '==', 'prov1').get());
  await check('other customer cannot read booking', false, () => cust2.doc('bookings/b9').get());
  await check('provider accepts', true, () => prov1.doc('bookings/b9').update({ status: 'ACCEPTED', updatedAt: now }));
  await check('provider completes', true, () => prov1.doc('bookings/b9').update({ status: 'COMPLETED', updatedAt: now }));
  await check('customer cannot cancel a completed booking', false, () => cust1.doc('bookings/b9').update({ status: 'CANCELLED', updatedAt: now }));
  await check('customer cancels a pending booking', true, () => cust1.doc('bookings/b2').update({ status: 'CANCELLED', updatedAt: now }));
  await check('customer cannot mark reviewed without a review', false, () => cust1.doc('bookings/b3').update({ reviewed: true, updatedAt: now }));

  console.log('Chats');
  const chatId = 'cust1_prov1';
  await check('customer gets not-yet-existing chat', true, () => cust1.doc(`chats/${chatId}`).get());
  await check('customer creates chat (ChatThread fields)', true, () => cust1.doc(`chats/${chatId}`).set({ chatId, customerId: 'cust1',
    customerName: 'Priya', providerId: 'prov1', providerName: 'Rohan', providerTitle: 'Electrician', participants: ['cust1', 'prov1'],
    lastMessage: null, lastSenderId: null, updatedAt: now, unreadCustomer: 0, unreadProvider: 0 }));
  await check('stranger cannot create chat for others', false, () => cust2.doc('chats/cust1_prov1x').set({ chatId: 'cust1_prov1x',
    customerId: 'cust1', providerId: 'prov1x', participants: ['cust1', 'prov1x'], unreadCustomer: 0, unreadProvider: 0 }));
  await check('customer lists chats (customerId ==)', true, () => cust1.collection('chats').where('customerId', '==', 'cust1').get());
  await check('provider lists chats (providerId ==)', true, () => prov1.collection('chats').where('providerId', '==', 'prov1').get());
  await check('customer cannot list another customer\'s chats', false, () => cust2.collection('chats').where('customerId', '==', 'cust1').get());
  const send = (db, sender, fromCustomer, text) => {
    const chatRef = db.doc(`chats/${chatId}`);
    const msgRef = chatRef.collection('messages').doc();
    const batch = db.batch();
    batch.set(msgRef, { messageId: msgRef.id, senderId: sender, senderName: sender, text, imageUrl: null, createdAt: Date.now() });
    batch.update(chatRef, { lastMessage: text, lastSenderId: sender, updatedAt: Date.now(),
      [fromCustomer ? 'unreadProvider' : 'unreadCustomer']: FieldValue.increment(1) });
    return batch.commit();
  };
  await check('customer sends message (batch)', true, () => send(cust1, 'cust1', true, 'Are you free Saturday?'));
  await check('provider loads message history', true, () => prov1.doc(`chats/${chatId}`).collection('messages').orderBy('createdAt').get());
  await check('provider replies', true, () => send(prov1, 'prov1', false, 'Yes, after 4 PM'));
  await check('customer marks chat read', true, () => cust1.doc(`chats/${chatId}`).update({ unreadCustomer: 0 }));
  await check('outsider cannot read chat', false, () => cust2.doc(`chats/${chatId}`).get());
  await check('outsider cannot read messages', false, () => cust2.doc(`chats/${chatId}`).collection('messages').get());
  await check('outsider cannot send message', false, () => send(cust2, 'cust2', true, 'spam'));
  await check('cannot send as someone else', false, () => cust1.doc(`chats/${chatId}/messages/m1`).set(
    { messageId: 'm1', senderId: 'prov1', text: 'fake', createdAt: now }));
  await check('member cannot change participants', false, () => cust1.doc(`chats/${chatId}`).update({ participants: ['cust1', 'cust2'] }));

  console.log('Reviews');
  // Same computation as FirestoreHelper.submitReview()
  const submitReview = (db, bookingId, providerId, customerId, rating, tamper) => db.runTransaction(async (tx) => {
    const bookingRef = db.doc(`bookings/${bookingId}`), providerRef = db.doc(`providers/${providerId}`);
    const reviewRef = db.doc(`reviews/${bookingId}`);
    await tx.get(bookingRef);
    const p = (await tx.get(providerRef)).data();
    const oldAvg = p.averageRating || 0, oldCount = p.reviewCount || 0;
    const oldTotal = p.ratingTotal !== undefined ? p.ratingTotal : oldAvg * oldCount;
    const newCount = oldCount + 1, newTotal = oldTotal + rating;
    const newAvg = Math.round(newTotal / newCount * 10) / 10;
    tx.set(reviewRef, { reviewId: bookingId, bookingId, customerId, providerId, customerName: 'Priya', rating,
      comment: 'Great work', createdAt: now });
    tx.update(bookingRef, { reviewed: true, updatedAt: now });
    tx.update(providerRef, Object.assign({ averageRating: newAvg, reviewCount: newCount, ratingTotal: newTotal,
      lastReviewId: bookingId }, tamper || {}));
  });
  await check('customer reviews completed booking (old listing without ratingTotal)', true,
    () => submitReview(cust1, 'b1', 'prov1', 'cust1', 5));
  const after = (await env.withSecurityRulesDisabled(async (c) => {
    const d = (await c.firestore().doc('providers/prov1').get()).data(); console.log('       provider now:', JSON.stringify(
      { averageRating: d.averageRating, reviewCount: d.reviewCount, ratingTotal: d.ratingTotal })); }));
  await check('second review of the same booking is rejected', false, () => submitReview(cust1, 'b1', 'prov1', 'cust1', 5));
  await check('review with inflated rating total is rejected', false, () => submitReview(cust1, 'b3', 'prov1', 'cust1', 1,
    { ratingTotal: 100, averageRating: 5 }));
  await check('review that skips the count is rejected', false, () => submitReview(cust1, 'b3', 'prov1', 'cust1', 4,
    { reviewCount: 10 }));
  await check('review of a pending booking is rejected', false, () => submitReview(cust1, 'b2', 'prov1', 'cust1', 5));
  await check('another customer cannot review this booking', false, () => submitReview(cust2, 'b3', 'prov1', 'cust2', 1));
  await check('rating 6 is rejected', false, () => submitReview(cust1, 'b3', 'prov1', 'cust1', 6));
  await check('customer cannot write a provider rating directly', false, () => cust1.doc('providers/prov1').update({ averageRating: 5 }));
  await check('second honest review on another booking', true, () => submitReview(cust1, 'b4', 'prov1', 'cust1', 3));
  await check('anyone signed in can read reviews', true, () => cust2.collection('reviews').where('providerId', '==', 'prov1').get());

  console.log('Admin, portfolio, favorites, categories');
  await check('admin seeds a provider', true, () => admin.doc('providers/seed_1').set({ providerId: 'seed_1', active: true,
    verified: true, averageRating: 4.8, reviewCount: 120, ratingTotal: 576 }));
  await check('admin verifies a provider', true, () => admin.doc('providers/newprov').update({ verified: true }));
  await check('admin lists all users', true, () => admin.collection('users').get());
  await check('customer cannot list all users', false, () => cust1.collection('users').get());
  await check('provider adds portfolio item', true, () => newProv.doc('portfolio/pf1').set({ portfolioId: 'pf1', providerId: 'newprov', imageUrl: 'x' }));
  await check('customer cannot add portfolio for provider', false, () => cust1.doc('portfolio/pf2').set({ portfolioId: 'pf2', providerId: 'newprov' }));
  await check('customer saves favorite', true, () => cust1.doc('favorites/cust1_prov1').set({ customerId: 'cust1', providerId: 'prov1', createdAt: now }));
  await check('customer removes favorite', true, () => cust1.doc('favorites/cust1_prov1').delete());
  await check('anyone signed in reads categories', true, () => cust1.collection('categories').get());
  await check('customer cannot write categories', false, () => cust1.doc('categories/x').set({ name: 'x' }));
  await check('signed-out users read nothing', false, () => env.unauthenticatedContext().firestore().doc('providers/prov1').get());

  console.log(`\n${pass} passed, ${fail} failed`);
  await env.cleanup();
  process.exit(fail ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
