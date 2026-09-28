# Integration requirements and honest boundaries

Implemented through v0.3.0: Android-native, user-approved grouped autofill; encrypted portable backup/restore; local setup checks; explicit confirmation paste; Chrome signer and HTTPS origin checks; protected field rejection; per-request device authentication; conservative payment reconciliation with recorded local evidence; existing preparation/history/countdown and localization.

Still external dependencies:
- Live transaction, PNR and payment verification requires authorized IRCTC/provider API access and actual API documentation. We cannot create authorization or invent an endpoint. No booking API credentials or provider integration were supplied in this task. CAPTCHA/OTP/payments remain on official IRCTC.
- Production signed publishing requires a stable private release keystore and the GitHub signing secrets listed in README. A repository was not selected for publication. The workflows are included; no secrets or signing keys are in source.
- Live form compatibility and device/browser behavior require validation on the intended phone. Automated fixtures and software-emulator tests do not establish live IRCTC compatibility.

Sources checked 2026-09-27:
- Android Autofill framework/service documentation: https://developer.android.com/identity/autofill/autofill-services
- Native Chrome third-party autofill: https://developers.googleblog.com/en/chrome-3p-autofill-services/
- User configuration: https://support.google.com/chrome/answer/142893?co=GENIE.Platform%3DAndroid&hl=en
- Google signing certificate publication: https://www.google.com/.well-known/assetlinks.json
- IRCTC provider integration documentation is released after onboarding formalities: https://contents.irctc.co.in/en/NormsforB2CMobile.pdf

IRCTC/provider terms and requirements must be confirmed directly before a commercial integration. This task did not apply for provider status, incur onboarding fees or contact third parties.

## Limits that an app change cannot remove

Confirmed seats depend on actual availability and IRCTC allocation. A countdown does not reserve seats or bypass a queue. CAPTCHA/OTP/payment authorization remain required by the official flow. Broad autofill is not enabled by trusting arbitrary browsers or guessing unknown controls. Live payment verification/history synchronization still needs authorized APIs and documentation; a pasted PNR is not authoritative verification. Real-device/live-form testing needs an actual device and user-controlled session. Signing automation is ready but requires stable private secrets under the owner's control. None of these are reported as completed merely because tests pass.
