## Native assistant upgrade (v0.3 preview)

The complete Kotlin/Compose assistant is in [native-assistant](native-assistant/README.md). Open that folder as the Android Studio project. It includes encrypted profiles and backups, guarded autofill, conservative payment recovery, bilingual UI, local history and tests. The original root app remains available for comparison; it has a different package ID and no automatic private-data migration.

Use **Native assistant checks and preview APK** in Actions. Signed native releases use tags `native-vX.Y.Z` and the signing secrets documented in the new project's README. Preview APKs install alongside older apps. No live IRCTC transaction integration or ticket-success guarantee is claimed.

---

# Tatkal Fast Assistant

Fast native Android preparation companion for the official IRCTC booking flow.

## V1
- Persistent journey preset: From, To, journey date and train
- Passenger preset field
- Class selector: 1A, 2A, 3A and SL
- AC / Non-AC Tatkal opening-time reference
- Readiness percentage
- IRCTC/Aadhaar readiness checklist
- IRCTC eWallet as preferred payment with balance-ready check
- Automatic system light/dark theme
- One-tap Quick Book handoff to official IRCTC
- GitHub Actions debug APK build

The app prepares and saves information locally. CAPTCHA, OTP, payment authorization and final booking remain manual on IRCTC. It does not bypass queues, rate limits, CAPTCHA, or other IRCTC protections.

## Android installation
Open Actions > Build Android APK > latest successful run > Artifacts. Download Tatkal-Fast-Assistant-debug, extract it, and install app-debug.apk.

## Next
V1.1 can add multiple named passenger/journey profiles, date picker, countdown notifications, biometric protection and improved encrypted local storage.