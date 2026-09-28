# Tatkal Fast Assistant

Native Android booking preparation assistant. Kotlin, Compose Material 3, Android 8+.

**Integration boundary:** actual booking happens on official IRCTC in your browser. This app does not have an authorized IRCTC transaction API. Optional Android Autofill offers recognized prepared fields on verified Google Chrome HTTPS IRCTC pages, after device authentication and explicit per-field confirmation. It does not inject browser scripts, inspect CAPTCHA, read OTPs, observe transaction state, query availability, submit bookings, or detect payments/PNRs. Prepared values can be copied individually. The checklist and history are explicitly user-reported. Use IRCTC's own Master List for its supported passenger reuse.

## Build

Install Android Studio with JDK 17 and Android SDK 35 / build tools 35.0.0. Open this directory, let Gradle sync, and run `app`.

```sh
chmod +x gradlew
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Run device tests on a connected emulator or phone:

```sh
./gradlew connectedDebugAndroidTest
```

Project dependencies are pinned. Gradle wrapper distribution is checksum-verified. No API keys, backend, paid service, or IRCTC credentials are required.

## Use

1. Enable a device PIN/password/biometric. Unlock the app. Optional app locking can be changed after authentication in Profile.
2. Add passengers and groups. Enter age for the journey date. The app stores nationality and child/senior preferences but IRCTC determines actual eligibility, fares and available options.
3. Add a journey, including **train-origin departure date** and your boarding date. They may differ for overnight journeys. Select passengers, class, quota and boarding station.
4. Select payment category (default IRCTC eWallet). No payment is performed here.
5. Review all details. Start prepares a durable attempt; **Open official IRCTC** commits the handoff lock before opening the browser.
6. Complete login, train selection, passenger entry, CAPTCHA, OTP, final review and payment on IRCTC. Switch back for copyable details if needed. Biometric/device authentication is required again when the app returns from background if enabled.
7. Use the optional local checklist for progress. The app cannot observe browser steps. After any uncertain outcome, verify official booking history and the payment provider before recording a result. New attempts remain locked until resolved.
8. Enter PNR, status and optional coach/berth after verification. Sharing includes route, date, train, PNR and allocation; no passenger names or payment details.

## Implemented

- Multiple persisted journey profiles and passenger groups; editable selections and preferences.
- Local validation, review and immutable attempt snapshots.
- IST countdown based on origin date; AC 10:00 and SL/2S 11:00; no countdown for General/1A. Device clock is not server-synchronized.
- Optional generic WorkManager reminder about two minutes before opening. Delivery is approximate and may be delayed by Android. Editing/deleting a profile cancels its old reminder. No remote polling.
- AES-256/GCM encrypted Room payloads with Android Keystore; atomic writes; versioned migration; no destructive migration fallback.
- Persisted booking state and conservative UNKNOWN recovery after process death.
- Global unresolved-attempt lock, including across journeys. Manual failure resolution requires explicit history/payment verification. This cannot prevent a user booking separately in a browser or another app.
- User-recorded booking history with upcoming/completed/cancelled categories and PNR copy/share.
- English/Tamil interface, system/light/dark themes, device authentication and screenshot protection.
- GitHub debug/test and signed release workflows.

## Repository / APK without a computer

Upload this project to a GitHub repository (preserve `.github` and `gradle` folders). The **Android checks and debug APK** workflow runs on push or manually from Actions. Download `TatkalFastAssistant-debug` from the completed run's Artifacts section and extract the APK on your phone. Debug artifacts are not GitHub Releases. Debug signing keys can differ between build machines, so one debug APK may not update another. Use a stable private release key for durable updates; resolve pending payments before uninstalling or clearing app data.

No repository was selected or modified by this source delivery. Local build results are in `docs/VERIFICATION.md`.

## Android autofill (v0.3.0)

1. In Profile, choose **Set up Android autofill** and select this app in Android settings. Android has one active provider; selecting it replaces the previous provider.
2. In Chrome > Settings > Autofill services, choose **Autofill using another service**, then restart Chrome if prompted.
3. Prepare and review a journey, start it, and open official IRCTC. Focus a recognized field and select the Tatkal suggestion.
4. Unlock with device authentication, select the correct passenger when required, review the displayed value and tap **Fill this field**.

Scope: Android 9+ for web origin verification; Android 8 keeps manual copying. Only the Google-signed `com.android.chrome` browser and exact `https://www.irctc.co.in` / `https://irctc.co.in` origins are accepted. Unknown signatures/origins/fields receive no data. Other browsers are not supported yet. Browser/page field metadata and third-party autofill support are required; this does not guarantee live IRCTC field compatibility. Passenger name, age, mobile, station codes and train number have explicit aliases. Generic names, conflicting identifiers and security/payment fields are rejected. Fill can include unambiguous sibling fields in the same data partition after one review; choose station dropdown suggestions manually. CAPTCHA, OTP, login and payment authorization always remain manual. No SaveInfo or form-value collection is provided.

The generic suggestion contains no personal data. Device authentication is always required for autofill, even when the optional main app lock is off. Ephemeral request tokens expire after two minutes and are single-use. Payment/unknown/finished attempts cannot fill. Process death invalidates tokens.

## Payment reconciliation (v0.3.0)

The outcome screen requires separate confirmation of official booking history and bank/UPI/eWallet status. A pending/unknown payment or a successful payment without a booking remains locked. Failure can be recorded only when no payment started, failure is verified, or refund is confirmed. Success requires a paid status and a ten-digit PNR. The evidence category and local verification time are stored. All outcomes remain explicitly user-recorded.

Live payment/PNR verification cannot be implemented without authorized API documentation and access. IRCTC publishes provider onboarding requirements; see docs/INTEGRATION.md. No fabricated endpoint or simulated payment response exists.

## Signed release

Set repository Actions secrets:

- `SIGNING_KEYSTORE_BASE64`: base64-encoded private signing keystore
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Keep the signing key backed up privately. Never commit it. Then push a semantic version tag such as `v0.3.0`. The release job validates secrets, runs tests/lint, builds signed APK/AAB, calculates SHA-256 checksums and publishes a GitHub Release. Missing secrets fail the job; it does not silently publish an unsigned APK. Version code uses the release workflow run number; preserve monotonicity if migrating repositories. Release credentials and Play publishing are not configured in this delivery.

## Architecture

- `domain`: platform-independent serializable models, validation, timetable calculations and state machine with JVM tests.
- `app/data`: Hilt-injected Room encrypted vault and non-sensitive DataStore settings.
- `app/BookingViewModel`: serialized asynchronous mutations and observable state.
- `app/ui`: Compose screens and bilingual text catalog.
- `app/ReminderWorker`: no-data local notification scheduling.

Single app module plus isolated domain keeps build overhead low. Feature boundaries are packages, not empty Gradle modules. No network module is needed because the assistant makes no server requests. No WebView, JavaScript bridge, accessibility service, SMS permissions or credential vault are present.

See `docs/SECURITY.md`, `docs/PRIVACY.md`, `docs/RULES.md` and `docs/RELEASE-CHECKLIST.md`.


## v0.3: safer upgrades and fewer repeated steps

- **Profile → Encrypted backup / restore**: device authentication, a 12–128 character passphrase, Android document picker, AES-256-GCM and PBKDF2-HMAC-SHA256 (210,000 iterations). Preview counts before a merge. Existing profile IDs win collisions. An unresolved attempt is recovered as UNKNOWN; a restore cannot clear its lock. Conflicting unresolved attempts reject the entire import. Settings and reminders are not included.
- **Grouped autofill**: one authentication and explicit review can fill uniquely identified sibling fields. Passenger details are never mixed with the journey/contact partition. Different rows/origins and ambiguous siblings are excluded. Unrecognized fields still need manual entry. No browser DOM or form values are scraped.
- **Profile setup check**: Android version, Google Chrome signature, chosen Autofill Service, device lock, automatic clock and notification permission. Refreshes on return from Settings. It does not assert live page compatibility or server clock accuracy.
- **Confirmation paste**: explicitly paste official text in Resolve. Only a single labelled 10-digit PNR is extracted; you must still check the journey, booking history and payment status. No SMS or clipboard monitoring, and no inferred payment success.
- The official flow prefers verified Chrome when available. Other browsers retain manual handoff.

### Installation and existing data

The provided APK uses application ID `in.tatkalfast.assistant.preview`, labelled **Tatkal Fast Assistant Preview**, so it installs alongside the older APK without requiring you to delete its data. Preview storage is separate. Android prevents this app from reading another package's private profiles. Earlier v0.1/v0.2 builds have no portable export, so their records must be retained in that installation or entered manually here. Lost signing keys cannot be reconstructed from an APK.

From v0.3 onward, make an encrypted backup and test restoring it before changing devices or replacing a differently signed installation. Keep the backup passphrase separately. If it is forgotten, neither the app nor developer can recover it. Backups intentionally leave app-private storage at a destination you select; protect that file.

### Stable signing in GitHub Actions

The normal build accepts `DEBUG_KEYSTORE_BASE64`, `DEBUG_STORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`, and `DEBUG_CERT_SHA256` secrets for a stable preview signing key. The SHA-256 value is the certificate digest (hex, optional colons). The workflow checks the key's certificate before building and fails on a mismatch. Pull-request builds intentionally do not receive this key. Without these secrets, builds use an ephemeral debug key and emit a warning: updates are not guaranteed.

Signed releases additionally require `SIGNING_CERT_SHA256` alongside the existing release signing secrets. Use a separate private release key; never commit keys or passwords. Android's package and signing identity are immutable across compatible updates. The preview package is deliberately distinct from production and the legacy GitHub app (`com.tatkal.fastassistant`).

CI device coverage is configured for API 26, 29 and 35. Local execution results are recorded in docs/VERIFICATION.md; a configured matrix is not evidence that every device has passed.
