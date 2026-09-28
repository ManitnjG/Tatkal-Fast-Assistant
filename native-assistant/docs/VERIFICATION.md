# Verification — v0.3.0 preview, 2026-09-28

- 74 JVM unit tests passed (zero failures/errors); original XML reports retained below.
- Preview APK and Android instrumentation APK compiled successfully.
- Debug lint passed: zero errors, three cosmetic UseKtx warnings.
- The preview APK signature was verified with Android SDK apksigner on 2026-09-27. The retained APK checksum still matches.
- v0.3 device tests and final minified release checks were interrupted by the execution usage limit. No successful result was recovered. GitHub Actions is configured to rerun unit tests, debug/release lint, debug APK, unsigned release AAB, and device tests on API 26/29/35.

## Downloaded preview APK

Version: 0.3.0-preview (3)  
Application ID: in.tatkalfast.assistant.preview  
Bytes: 20,114,924  
SHA-256: e52ee456e63d79200a1a8778bf86c209aa857cd2b8708f6e2a0bd3ccff3de1dc

The preview installs alongside older packages without removing their data. It cannot automatically read another package's private profiles. Future upgrades require the same signing key, or an encrypted backup/restore after a deliberate reinstall. Stable signing secrets have not been supplied in this task.

## Verification boundaries

No live IRCTC login, CAPTCHA, OTP, booking or payment was performed. Browser/form compatibility and the end-to-end document-picker backup flow still require real-phone testing. Local PNR extraction is not an authoritative booking or payment-status API. Duplicate-attempt protection applies inside this assistant; independent browser transactions remain under the user's control.

The earlier v0.2 test run passed 9 emulator tests. That result is not a substitute for running the changed v0.3 suite. Production signing, authorized IRCTC APIs and an independent security review remain external prerequisites.

## Reproduce

```sh
./gradlew :domain:test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:bundleRelease :app:lintRelease
./gradlew connectedDebugAndroidTest
```
