# Release acceptance

- Run JVM, lint and device tests. Exercise Android 8, 10, 13 and 15, low-memory device, Tamil fonts, dark mode, 200% font scale, TalkBack and small screens.
- Verify screen lock setup, cancellation, returning from browser, process death and encrypted database corruption behavior. Confirm no automatic reset and no sensitive task-switcher screenshot.
- With your own test account, verify browser handoff and manual copy; enter CAPTCHA/OTP only on IRCTC. Never perform a payment merely for an automated test.
- Simulate network interruption before/after payment and verify UNKNOWN/new-attempt lock. Resolve only after independently checking official history and payment state.
- Verify PNR user-entry, RAC/waitlist, official cancellation vs local mark, and clipboard behavior across API levels.
- Verify reminder permission denial, rescheduling after edits, reboot/Doze delay, and timezone/date rollover. Device clock must be correct.
- Review IRCTC rules and terms and current Play target SDK requirements. The pinned target SDK is 35; update it as required for actual store submission.
- Configure private signing secrets; verify signed APK identity, install/update and checksums. Test minified release on a device before distribution.
- Commission security/privacy review and publish the privacy notice with your developer contact before public production release.
