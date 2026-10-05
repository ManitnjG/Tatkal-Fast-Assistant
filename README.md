# Tatkal Fast Assistant 2.0

Native Android **manual preparation and recovery companion**. Updates the existing application ID. This is not an authorised IRCTC booking integration and cannot book, autofill, inspect the official browser, query transactions, retrieve a PNR or independently confirm a ticket.

## Compliance boundary
IRCTC's current Terms and Conditions prohibit automation/scripting software on its website and apps. The previous WebView JavaScript injections have been removed. All login, search, train selection, passenger entry, CAPTCHA, OTP and payment actions stay with the user on official IRCTC. Use IRCTC's own Master List for faster permitted passenger entry. No scraping, polling, security bypass, automatic refresh, hidden API calls or payment resubmission occurs.

Official references checked 2026-10-05:
- https://contents.irctc.co.in/en/Terms%20and%20conditions.pdf (automation restriction; Tatkal availability)
- https://contents.irctc.co.in/en/tatkal_faq.pdf (10:00 AC / 11:00 non-AC IST, day before origin departure)
- https://www.pib.gov.in/PressReleasePage.aspx?PRID=2227511&lang=1&reg=3 (Aadhaar OTP)

## Included
- Android Keystore AES-GCM storage in app-private no-backup files, atomic writes and fail-closed storage errors.
- Journey, boarding, origin date, passenger profiles, berth/meal preferences, maximum total fare and ordered train/class alternatives.
- Basic format/date validation, 1–4 passengers and duplicate-choice detection. Train schedules, station routes, extra passenger requirements and availability must be verified on IRCTC.
- Local countdown for the **first** train/class; origin dates and opening times of alternatives must be checked separately. Device time is not a server-synchronised clock.
- Explicit persistent workflow states; user-reported progress visibly distinguished from live official status.
- Summary and maximum-fare check before payment handoff. Durable PENDING marker before opening browser. PENDING/UNKNOWN/SUCCESS reports block another payment handoff, even after restart, errors or session expiration.
- User checks both official histories before recording payment outcome. A manual FAILED report permits a new reviewed attempt; it is not machine verification. SUCCESS never becomes BOOKED.
- Delete passenger details without deleting the payment lock. Screen captures disabled; no sensitive logs, clipboard copying, passwords, OTPs, PINs, CVVs or token storage.
- Browser manages its own IRCTC login session. The companion does not copy or persist it.
- Pure core retry policy: 1/2/4/8 seconds, maximum 4 attempts, honours longer Retry-After; excludes side effects, queues, security controls and unresolved payment. **No network retry runner is enabled.**

## Limitations / follow-up gates
This is a manual companion, not all 19 requested automated capabilities. Live automatic fallback, page/error detection, automatic resume, PNR retrieval and ticket export require an explicitly authorised integration and are not implemented. No generic DOM-text heuristic is accepted as booking/payment proof. Print/download the real ticket in IRCTC. The app cannot prevent a user paying twice independently in their browser.

Payment protection depends on recording handoff before paying. Clearing Android app data or uninstalling removes local protection. A SUCCESS report stays locked; use IRCTC for subsequent bookings until an authorised reconciliation/archive path exists. Infants/special passenger requirements are entered directly on IRCTC. Legacy unstructured passenger text is intentionally not imported; enter validated profiles again. Existing WebView session remnants are cleared on first launch of v2.

## Build and verification
Java 17, Gradle 8.9, Android SDK 35. GitHub Actions runs core safety checks, unit tests and assembles the debug APK. Debug APK is for testing; production distribution needs a managed release signing key and on-device verification.

```
gradle :app:testDebugUnitTest :app:assembleDebug
javac -d /tmp/tatkal-checks app/src/main/java/com/tatkal/fastassistant/core/BookingFlow.java tests/CoreChecks.java
java -cp /tmp/tatkal-checks CoreChecks
```

Device acceptance: save/restart, corrupt ciphertext fail-closed, review rotation/process death, unknown-payment restart lock, browser return, passenger deletion, offline browser handoff and accessibility/font scaling. No live booking/payment tests were performed.

## Verification in this change
The pure Java booking core compiled with Eclipse ECJ 3.39.0 (Java 17 target) and passed 55 executable safety checks on 2026-10-05. `git diff --check` passed. Android compilation, UI rendering, instrumentation and live IRCTC flows have not been tested in this environment; Android SDK/Gradle are not installed. GitHub publication was blocked pending explicit approval, so CI has not run for this change.
