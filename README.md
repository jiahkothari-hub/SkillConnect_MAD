# SkillConnect

**Find the right skill. Connect with the right person.**

SkillConnect is a location-based Android marketplace that connects people who need a service (customers) with people who provide a skill (providers): electricians, plumbers, tutors, photographers, designers, makeup artists, developers, fitness trainers and more.

This repository is the **V1 MVP**, built for a Mobile Application Development course and designed to grow into a startup product. It is written in **Java + XML** and uses **Firebase** as the cloud backend, **Google Maps** for discovery and **SQLite** for local data.

---

## Contents

1. [Features](#1-features)
2. [Technology stack](#2-technology-stack)
3. [Requirements](#3-requirements)
4. [Opening and running the project](#4-opening-and-running-the-project)
5. [Demo mode vs. Firebase mode](#5-demo-mode-vs-firebase-mode)
6. [Firebase setup](#6-firebase-setup)
7. [Google Maps API key](#7-google-maps-api-key)
8. [Running on an emulator or phone](#8-running-on-an-emulator-or-phone)
9. [Firestore collections and schema](#9-firestore-collections-and-schema)
10. [Demo users and sample data](#10-demo-users-and-sample-data)
11. [Permissions](#11-permissions)
12. [How each course topic is demonstrated](#12-how-each-course-topic-is-demonstrated)
13. [Project structure](#13-project-structure)
14. [User flows](#14-user-flows)
15. [Known limitations](#15-known-limitations)
16. [Future roadmap](#16-future-roadmap)

---

## 1. Features

**Everyone**
- Animated splash screen → 3-page onboarding (stored in SharedPreferences) → login/register
- Email/password registration with role selection (**Customer** or **Provider**), full validation
- Login, logout, forgot password (Firebase password-reset email)
- Local notifications for booking updates, plus Firebase Cloud Messaging support

**Customer**
- Home: greeting, location chip, search bar, popular categories, *Nearby Skilled People*, *Recommended for You*
- Categories screen (5 categories, 30+ skills, provider counts)
- Live search across name, skill, category and description, with recent searches (SQLite) and an empty state
- Filters bottom sheet: category, distance, price range, minimum rating, availability, verified only, sort (relevance, rating, price, distance)
- Provider cards: photo, name, verified badge, skill, rating, review count, starting price, distance ("1.4 km away"), availability dot
- Provider profile: stats, availability, about, skills, portfolio grid, reviews, Contact (call/SMS/WhatsApp), Map, Share, Save ♡
- **Nearby Providers** Google Map with markers; tapping a marker shows name, skill, rating, price and *View Profile*
- Booking: service, **DatePickerDialog**, **TimePickerDialog**, address, description, confirmation dialog
- My Bookings (Upcoming / Completed / Cancelled) in real time, booking details, cancel booking
- 5-star review after a completed job; the provider's average rating updates automatically
- Saved providers and recently viewed (SQLite)
- Export booking history as **booking_history.csv** (preview, share or save to Downloads)

**Provider**
- First-time profile setup, later editable: photo (camera/gallery), title, category, skills, experience, price + unit, description, service location (map picker or GPS + reverse geocoding), working days and hours, availability switch, verification status
- Dashboard: pending / confirmed / completed counts, rating, availability switch, newest requests with Accept/Decline
- Requests tab (Accept / Decline) and Jobs tab (Accepted → *Mark as Completed*, Completed, Closed)
- Portfolio manager: camera or gallery → preview → upload to Firebase Storage; tap to view, long-press to delete
- Export profile as **provider_profile.txt**, export job history as CSV, share profile

**Admin** (basic, inside the app)
- View all provider listings, deactivate/activate them, verify/unverify providers
- View users and deactivate accounts, view all bookings, view categories with provider counts
- *Seed demo data to Firestore* menu action

---

## 2. Technology stack

| Area | Technology |
|---|---|
| Language / UI | Java 17, XML layouts, Material Design 3 components |
| Architecture | Activities + Fragments, adapters, model classes, a repository interface with Firebase and Demo implementations |
| Auth | Firebase Authentication (email/password) |
| Cloud database | Cloud Firestore (with offline cache) |
| File storage | Firebase Storage |
| Notifications | Firebase Cloud Messaging + local notifications |
| Maps & location | Google Maps SDK, Fused Location Provider, Geocoder |
| Local database | SQLite (`SQLiteOpenHelper`) |
| Images | Glide, camera/gallery implicit intents, `FileProvider` |
| Build | Gradle 8.9, Android Gradle Plugin 8.5.2, compileSdk/targetSdk 34, minSdk 26 |

---

## 3. Requirements

- **Android Studio** Koala (2024.1) or newer (any recent version with AGP 8.5 support)
- **JDK 17** – the one bundled with Android Studio works (Settings → Build Tools → Gradle → Gradle JDK)
- Android SDK Platform 34 (Android Studio installs it on first sync)
- A device or emulator running **Android 8.0 (API 26) or newer**. Use an emulator image **with Google Play** for Maps and location.

---

## 4. Opening and running the project

1. Unzip the project (or clone this repository).
2. Android Studio → **File → Open** → select the `SkillConnect` folder (the one containing `settings.gradle`).
3. Wait for **Gradle sync** to finish. If it doesn't start, click **File → Sync Project with Gradle Files**.
4. Press **Run ▶**.

The project compiles and runs **without any configuration** – it starts in **demo mode**. Add Firebase and the Maps key (sections 6 and 7) to enable the real backend.

A GitHub Actions workflow (`.github/workflows/android.yml`) also builds a debug APK on every push and uploads it as a build artifact.

---

## 5. Demo mode vs. Firebase mode

The app has one interface, `data/AppRepository.java`, with two implementations. `data/RepositoryProvider.java` picks which one to use.

| | **DEMO DATA** | **REAL FIREBASE DATA** |
|---|---|---|
| When | `google-services.json` is missing, **or** you tap *Explore with a demo account* on the login screen | `google-services.json` is present and you log in with a real account |
| Class | `DemoRepository` | `FirebaseRepository` → `FirebaseAuthHelper`, `FirestoreHelper`, `StorageHelper` |
| Accounts | Fixed fictional accounts (password `demo123`) | Firebase Authentication |
| Data | Fictional sample data in memory; resets when the app process restarts | Cloud Firestore, shared across devices |
| Images | Compressed and saved in app-private storage | Uploaded to Firebase Storage |
| Indicator | Yellow *Demo mode* banner on login; profile footer says "Demo mode" | Profile footer says "Connected to Firebase" |

Demo mode exists so the UI can be explored and presented before Firebase is set up. It does **not** pretend to be Firebase: nothing is sent to a server. SQLite, file export, maps, camera and notifications work the same in both modes.

---

## 6. Firebase setup

### 6.1 Create the project and add the Android app
1. Go to <https://console.firebase.google.com> → **Add project** (Google Analytics is optional).
2. Click the **Android** icon to add an app. Use package name **`com.skillconnect.app`** (it must match `applicationId` in `app/build.gradle`).
3. Download **`google-services.json`** and put it in the **`app/`** folder:
   ```
   SkillConnect/
   └── app/
       ├── google-services.json   ← here
       ├── build.gradle
       └── src/
   ```
4. **Sync Gradle.** `app/build.gradle` applies the Google Services plugin automatically when this file exists. The file is in `.gitignore`, so it is never committed.

### 6.2 Authentication
Firebase console → **Build → Authentication → Get started → Sign-in method → Email/Password → Enable**.

### 6.3 Cloud Firestore
1. **Build → Firestore Database → Create database**. Pick a location near your users (e.g. `asia-south1` Mumbai) and start in **production mode**.
2. Open the **Rules** tab, paste the contents of **`firebase/firestore.rules`** and click **Publish**.
3. No composite indexes are needed. Every query uses a single equality filter and sorting happens on the device.

### 6.4 Firebase Storage
1. **Build → Storage → Get started** (depending on your Firebase plan, Storage may require the Blaze plan for new projects).
2. Open the **Rules** tab, paste **`firebase/storage.rules`** and **Publish**.

### 6.5 Cloud Messaging (optional)
- The app already requests notification permission, registers `SkillConnectMessagingService` and saves each user's FCM token in `users/{uid}.fcmToken`.
- While the app is open, booking changes arrive through Firestore's real-time listener and are shown as local notifications, with no server needed.
- For push notifications while the app is **closed**, deploy the sample Cloud Functions in `firebase/functions/` (requires the Blaze plan and the Firebase CLI):
  ```bash
  cd firebase/functions && npm install
  firebase deploy --only functions
  ```

### 6.6 Create an admin and add sample data
1. Register a normal account in the app.
2. In the Firestore console, open `users/{yourUid}` and change `role` to **`ADMIN`**.
3. Log in again → you land on the **Admin Panel** → ⋮ menu → **Seed demo data to Firestore**.
   This writes the 17 fictional providers, portfolio placeholders, reviews and the `categories` collection. Seeded providers have no login accounts; they exist so customers can browse and book them.

Real providers appear in search as soon as they register and complete their profile.

---

## 7. Google Maps API key

1. Open <https://console.cloud.google.com> and select the Google Cloud project Firebase created (or any other project).
2. **APIs & Services → Library → Maps SDK for Android → Enable.**
3. **APIs & Services → Credentials → Create credentials → API key.** Restrict it to *Android apps*, package `com.skillconnect.app` plus your debug SHA-1 (run `./gradlew signingReport`).
4. Add the key to **`local.properties`** in the project root (this file is git-ignored):
   ```properties
   MAPS_API_KEY=AIza...your_key...
   ```
5. Sync Gradle and run. The key is injected into `AndroidManifest.xml` through `manifestPlaceholders`, so it never appears in the source code.

Without a key the app still works: the map screen shows a notice, markers and provider cards still function, and *Directions* opens the Google Maps app through an implicit intent.

---

## 8. Running on an emulator or phone

**Emulator**
1. Device Manager → create a Pixel device with an **Android 13/14 image that includes Google Play**.
2. To test location, open the emulator's **⋯ → Location**, choose a Mumbai address (e.g. Andheri) and click *Set location*.
3. To test the camera, the emulator's virtual camera works (Extended controls → Camera).

**Physical phone**
1. Enable *Developer options* (tap Build number 7 times) → turn on **USB debugging**.
2. Connect via USB, accept the prompt and select the phone in Android Studio → **Run ▶**.

---

## 9. Firestore collections and schema

Relationships use document IDs instead of copying large amounts of data. Bookings keep a small snapshot of names and phone numbers so each person sees the booking without reading the other person's private user document.

| Collection | Document ID | Fields |
|---|---|---|
| `users` | `{userId}` (Auth UID) | userId, name, email, phone, role (CUSTOMER / PROVIDER / ADMIN), profileImage, address, latitude, longitude, fcmToken, active, createdAt |
| `providers` | `{providerId}` = userId | providerId, name, title, category, skills[], experienceYears, description, startingPrice, priceUnit, latitude, longitude, address, availableDays[], availableFrom, availableTo, available, phone, email, profileImage, verified, averageRating, reviewCount, active, createdAt |
| `bookings` | auto ID | bookingId, customerId, customerName, customerPhone, providerId, providerName, providerPhone, service, description, date (dd-MM-yyyy), time (HH:mm), scheduledAt, price, location, status (PENDING / ACCEPTED / REJECTED / COMPLETED / CANCELLED), reviewed, createdAt, updatedAt |
| `reviews` | `{bookingId}` (one review per booking) | reviewId, bookingId, customerId, providerId, customerName, rating, comment, createdAt |
| `portfolio` | auto ID | portfolioId, providerId, imageUrl, storagePath, description, timestamp |
| `favorites` | `{userId}_{providerId}` | customerId, providerId, createdAt (cloud backup of SQLite favorites) |
| `categories` | `{categoryId}` | categoryId, name, skills[], order |

**Storage:** `profile_images/{userId}.jpg`, `portfolio/{providerId}/{portfolioId}.jpg`

**Security rules summary** (`firebase/firestore.rules`):
- Users can create only their own user document and cannot give themselves the ADMIN role.
- A provider can edit only their own provider profile, and cannot change `verified`, `averageRating`, `reviewCount` or `active`.
- Only the customer creates a booking, always as `PENDING`. The provider can only move it to ACCEPTED, REJECTED or COMPLETED. The customer can only cancel it or mark it reviewed. Nobody can modify someone else's booking.
- A review can be written once per booking, only by that booking's customer.
- Admins can manage everything.
- Storage: users can upload images (max 5 MB) only to their own paths.

---

## 10. Demo users and sample data

All sample data is **fictional**. Names, phone numbers and emails are made up.

| Role | Email | Password |
|---|---|---|
| Customer | `priya@demo.com` | `demo123` |
| Provider | `rahul@demo.com` (Rahul Sharma, Electrician) | `demo123` |
| Admin | `admin@demo.com` | `demo123` |

Every sample provider can log in as `firstname@demo.com` / `demo123` (e.g. `aarav@demo.com`, `riya@demo.com`).

Sample providers include Rahul Sharma (Electrician, Andheri East, ₹300, ★4.8), Aarav Mehta (Guitar Teacher, Bandra, ₹500/hour, ★4.7), Riya Kapoor (Graphic Designer, Powai, ₹800/project, ★4.9), Neha Patil (Makeup Artist, Borivali, ₹1,500, ★4.8), Kabir Shah (Video Editor, Malad, ₹1,000/project, ★4.6) and Meera Joshi (Math Tutor, Goregaon, ₹400/hour, ★4.9), plus 11 more across all categories. The demo customer starts with completed, accepted, pending and cancelled bookings, and Rahul has pending requests, so every workflow can be shown immediately.

---

## 11. Permissions

| Permission | Why | When requested |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Firebase, Maps, checking connectivity before writes | Automatically granted |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Distance to providers, "use current location" for service area | At runtime, after an explanation dialog. The app works with a default location if denied, and never tracks continuously. |
| `CAMERA` | Taking profile/portfolio photos with the built-in camera | At runtime, when *Take photo* is chosen |
| `POST_NOTIFICATIONS` (Android 13+) | Booking notifications | At runtime, on the home screen |

No storage permission is needed. The gallery uses `ACTION_GET_CONTENT`, and exports use app storage, `FileProvider` and MediaStore Downloads.

---

## 12. How each course topic is demonstrated

| Course topic | Where in SkillConnect |
|---|---|
| **Android UI / multiple screens** | 19 activities and 9 fragments with XML layouts (`res/layout`), Material 3 theme, styles, colours, dimens |
| **ListView + custom adapter** | `ProviderListActivity` and `SearchFragment` use a `ListView` with `ProviderListAdapter` (a `BaseAdapter` using the ViewHolder pattern: image, name, skill, rating, price, distance). `CategoriesActivity` uses a `ListView` with `CategoryAdapter` (2 view types). |
| **RecyclerView adapters** | `ProviderAdapter`, `BookingAdapter`, `ReviewAdapter`, `PortfolioAdapter`, `CategoryCardAdapter`, `SavedProviderAdapter`, `AdminAdapter` |
| **DatePicker** | `BookingActivity` → `DatePickerDialog` (no past dates, max 90 days ahead) |
| **TimePicker** | `BookingActivity` (booking time) and `EditProviderProfileActivity` (working hours) → `TimePickerDialog` |
| **Camera (implicit intent)** | `ImagePickerHelper` → `MediaStore.ACTION_IMAGE_CAPTURE` + `FileProvider`, with the runtime CAMERA permission |
| **Gallery (implicit intent)** | `ImagePickerHelper` → `Intent.ACTION_GET_CONTENT` (`image/*`) |
| **Image handling** | `ImageUtils` (EXIF rotation, down-scaling, JPEG compression), Glide loading, preview dialog before upload |
| **File handling (.txt / .csv)** | `FileUtils` writes `booking_history.csv`, `job_history.csv` and `provider_profile.txt`, reads them back for a preview, then shares (`ACTION_SEND`) or saves them to Downloads |
| **Google Maps** | `MapActivity` (Nearby Providers, markers, info card), `LocationPickerActivity` (choose service location) |
| **Location** | `LocationUtils`: runtime permissions, `FusedLocationProviderClient.getCurrentLocation`, distance calculation, reverse geocoding with `Geocoder` |
| **SQLite** | `database/DatabaseHelper` (`SQLiteOpenHelper`) with tables `favorite_providers`, `recently_viewed` and `recent_searches`. **INSERT** (save provider, recently viewed, new search), **SELECT** (Saved tab, recommendations, recent searches), **UPDATE** (refresh the saved snapshot, bump search time), **DELETE** (unsave, clear history). |
| **Firebase** | Authentication (`FirebaseAuthHelper`), Firestore (`FirestoreHelper`: CRUD, real-time listener, transaction, batch), Storage (`StorageHelper`), FCM (`SkillConnectMessagingService`) |
| **Client–server** | The Android client talks to the Firebase backend. The data rules are enforced on the server (`firebase/firestore.rules`). The optional Cloud Functions send pushes from the server. |
| **CRUD** | Providers (create at signup, read in lists, update in edit profile, admin deactivate), bookings (create, read, status updates), reviews (create, read), portfolio (create, read, delete), users (create, read, update) |
| **Explicit intents** | All screen navigation with extras, e.g. `ProviderDetailsActivity.EXTRA_PROVIDER_ID` |
| **Implicit intents** | Camera, gallery, Google Maps (`geo:` URI), dial (`tel:`), SMS (`smsto:`), WhatsApp link, share text/files (`ACTION_SEND`) |
| **Toasts & dialogs** | Toasts such as "Booking request sent" and "Provider added to favorites". AlertDialogs for "Send this service request?", "Are you sure you want to cancel this booking?", "Delete portfolio image?", logout and accept/decline. |
| **Spinner, RadioButton, CheckBox** | Filters bottom sheet (category/distance/sort spinners, price/rating radio groups, availability checkboxes), role radio buttons on Register, category and price-unit spinners on Edit Profile |
| **SharedPreferences** | `PrefsManager`: onboarding done, last location, notification setting |
| **Runtime permissions** | Location, camera and notifications via the Activity Result API |

---

## 13. Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/skillconnect/app/
│   ├── SkillConnectApp.java            Application class (init backend + notification channel)
│   ├── activities/                     Splash, Onboarding, Login, Register, CustomerHome, ProviderHome,
│   │                                   Categories, ProviderList, ProviderDetails, Map, LocationPicker,
│   │                                   Booking, BookingDetails, Review, EditProfile, EditProviderProfile,
│   │                                   Portfolio, Settings, Admin (+ BaseHomeActivity, BookingsHost)
│   ├── fragments/                      CustomerHome, Search, CustomerBookings, Saved, Profile,
│   │                                   ProviderDashboard, ProviderRequests, ProviderJobs, FilterBottomSheet
│   ├── adapters/                       ProviderAdapter, ProviderListAdapter (ListView), CategoryAdapter (ListView),
│   │                                   BookingAdapter, ReviewAdapter, PortfolioAdapter, ...
│   ├── models/                         User, Provider, Booking, Review, Category, PortfolioItem,
│   │                                   FavoriteProvider, FilterOptions
│   ├── data/                           AppRepository (interface), RepositoryProvider, DemoRepository,
│   │                                   DemoData, CategoryData, Callback, Subscription
│   ├── firebase/                       FirebaseRepository, FirebaseAuthHelper, FirestoreHelper,
│   │                                   StorageHelper, FirebaseErrors
│   ├── database/                       DatabaseHelper (SQLite)
│   ├── notifications/                  NotificationHelper, SkillConnectMessagingService (FCM)
│   └── utils/                          ValidationUtils, FileUtils, LocationUtils, DateTimeUtils, ImageUtils,
│                                       ImagePickerHelper, ProviderSearch, UiUtils, StateView, ...
└── res/  layout/ drawable/ menu/ values/ xml/ mipmap-anydpi-v26/
firebase/
├── firestore.rules      Firestore security rules
├── storage.rules        Storage security rules
└── functions/           Optional Cloud Functions for push notifications
```

**How data flows (useful for the viva):** Activity/Fragment → `RepositoryProvider.get()` → `AppRepository` method with a `Callback` → `FirebaseRepository` (Firestore/Storage/Auth) or `DemoRepository` → result on the main thread → the adapter updates the list. Home screens keep **one** real-time bookings listener (`BaseHomeActivity`) and share results with their tabs through the `BookingsHost` interface.

---

## 14. User flows

**Customer:** Splash → Onboarding → Login/Register (choose *Hire a skill*) → Home → Search / Categories → Provider List → Provider Profile → Map / Contact / Save → Book Service (date + time) → Booking Details → (provider completes) → Leave a Review → Bookings / Saved / Profile → Export CSV → Logout

**Provider:** Splash → Login/Register (choose *Offer my skill*) → Profile setup (photo, skills, price, location on map, availability) → Dashboard → Requests → Accept/Decline → Jobs → Mark completed → Profile → Edit Profile / Manage Portfolio (camera/gallery) / Service Location / Export Profile TXT → Logout

**Admin:** Login → Admin Panel → Providers / Users / Bookings / Categories → deactivate, verify, seed data → Logout

Every screen has a back button or bottom navigation, so there are no dead ends.

---

## 15. Known limitations

- **Demo mode data resets** when the app process restarts. SQLite data (favorites, history, searches) persists.
- **In-app chat is not included in V1.** *Contact* opens the phone dialer, SMS or WhatsApp.
- **Push notifications while the app is closed** need the optional Cloud Functions (Blaze plan). In-app/local notifications work without them.
- **Rating updates are client-side** (in a Firestore transaction, restricted by the rules to the two rating fields). Production should move this to a Cloud Function.
- **Verification is manual:** an admin toggles the badge. There is no document or KYC upload yet.
- **Categories are bundled in the app** (`CategoryData`) and mirrored to Firestore by the seeder. The admin can view them but not edit them.
- **Search is on the device**, which works for hundreds of providers. At scale, use a search service (Algolia/Typesense) and geo-queries (geohashes).
- **Payments are not included.** The price is a starting price agreed offline.
- **Firestore writes while offline** are queued by Firestore. The app warns the user and blocks important writes when there is no connection.
- **Light theme only.**

---

## 16. Future roadmap

**V1 (this MVP):** profiles, search, location, booking, reviews

**V2:** in-app chat, full push notifications, provider verification (ID/document upload), payments (UPI), availability calendar with time slots, better location matching (geohash radius queries)

**V3:** AI-based provider matching, smart recommendations, provider ranking, personalised and natural-language search

**V4:** escrow payments, dispute resolution, business accounts, provider subscriptions, commission system

**V5:** multi-city expansion, professional and enterprise services, API integrations

**AI ideas for later (not required in V1):** smart provider matching, natural-language service search ("someone to fix my AC tomorrow evening"), AI-written provider descriptions and service requests, personalised recommendations, review summarisation.

---

*All people, businesses, phone numbers and reviews in the sample data are fictional.*
