# Booking rules and integration sources

Checked during implementation on 2026-09-27. Rules can change; review before each production release. No remote rules updater is present.

- IRCTC Tatkal guide: https://contents.irctc.co.in/en/TatkalBooking.html — published guide states one day before train-origin departure date, excluding that departure day. Direct retrieval returned HTTP 403 in this environment; the indexed official result was available. No claim of a full live-page audit.
- Ministry of Railways / PIB announcement (11 June 2025): https://www.pib.gov.in/PressReleasePage.aspx?PRID=2135694 — Aadhaar-authenticated users for online Tatkal from 1 July 2025; Aadhaar OTP from 15 July 2025; AC/non-AC windows at 10:00/11:00 IST. Full page retrieved.
- IRCTC eWallet guide: https://contents.irctc.co.in/en/EwalletUserGuide.html — preference only, actual method availability remains on IRCTC.
- Official app listing identifies the website: https://play.google.com/store/apps/details?id=cris.org.in.prs.ima
- Compose BOM: https://developer.android.com/develop/ui/compose/bom

The app estimates the standard Tatkal opening; it cannot determine whether a specific train/class has Tatkal/Premium Tatkal seats or whether booking is suspended. 1A is excluded from the Tatkal countdown. General is accepted for preparation with no Tatkal countdown. Premium Tatkal uses the standard class opening estimate and requires live official confirmation. No guarantees of booking success or waitlist confirmation.

Preparation limits are conservative: 4 selected passengers for Tatkal/Premium, 6 for General; special infant/child cases are verified on IRCTC. ISO country code is a preparation value, not a promise of matching IRCTC form options. Senior preference does not assert a concession. Stored preferences never override official eligibility.
