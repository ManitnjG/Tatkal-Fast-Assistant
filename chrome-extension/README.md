# Tatkal Fast Assistant — AI Chrome extension (0.1 preview)

Desktop Chrome Manifest V3 extension. Real Gemini API integration extracts a journey draft from English or Tamil instructions. The AI is not simulated or replaced by regex. A separate, deterministic local adapter fills only reviewed, recognized, empty IRCTC fields. It never sends page content to AI or runs AI-generated code.

## Install (no build tools needed)
1. Extract `Tatkal-AI-Chrome-Extension.zip`.
2. In desktop Chrome open `chrome://extensions` and enable Developer mode.
3. Click **Load unpacked**, then choose the extracted `Tatkal-AI-Chrome-Extension` folder containing `manifest.json`.
4. Pin the extension from Chrome's Extensions menu.
5. Open the extension, expand **Gemini connection settings**, enter your own API key and a supported Gemini model ID (default `gemini-2.5-flash`), and click **Enable AI connection**. Approve access to Google's API when Chrome asks. Obtain a key at https://aistudio.google.com/apikey . Never share the key in chat or commit it.
6. Enter only journey instructions, tick the disclosure consenting to send that text to Google, and click **Create draft with AI**. Review the returned draft. Passenger data is entered separately and is not included in the AI request.
7. Click **Open official IRCTC**, then reopen the extension on that tab. Click **Preview this page**, review the checked values, and click **Fill selected fields**.
8. Select station suggestions and verify every value on IRCTC. Choose train, custom date/class/quota widgets, gender and berth manually when unsupported. Complete CAPTCHA, OTP, booking submission and payment yourself.

Example instruction: `TJ to MS on 30 September 2026, train 16866, SL, Tatkal`.
Tamil instructions are sent to the same model; correct interpretation depends on the provider and must be reviewed. No live Tamil inference was verified in this build.

## Actual AI use
`core.js` makes an HTTPS `models.generateContent` request to Google's Gemini API with a JSON response schema, timeout and explicit errors for invalid keys, unavailable models and quotas. The model extracts from/to/date/train/class/quota; missing values must remain empty. Only validated fields reach the editor. The AI cannot click buttons, select a payment method or submit anything. API availability, quotas and pricing depend on your Google account; no unlimited/free AI promise is made.

You can use the manual form and reviewed autofill without a key. There is no fake AI fallback. This package contains no key, remote JavaScript, server or subscription.

## Current autofill coverage
- Recognized empty From/To, train-number, mobile and passenger name/age inputs.
- Native date inputs and exact native select options when empty.
- Unrecognized or ambiguous labels are skipped. Repeated passenger rows are skipped entirely, rather than guessing a passenger-to-row match.
- Existing values are preserved. Text-based date widgets and custom dropdowns need manual entry.
- Station text is only typed: IRCTC's station suggestion must be selected and verified by the user. No hidden application state or private IRCTC endpoint is changed.
- Preview binds actual DOM nodes, their labels, the tab URL and a one-use token for two minutes. Navigation, changed labels, new content or expired previews require a new scan.
- No CAPTCHA/OTP/password/card/PIN/payment fields, form submission, payment automation, high-frequency polling or booking retries.

## Privacy and storage
- The AI receives only the text in the journey instruction box, after explicit consent, plus today's date in India. Any personal information the user includes in that box is also sent to Google, so keep it out. Provider data policies apply.
- API key and working draft use `chrome.storage.session`, retained during the browser session, not saved permanently. Close/reopen of the popup retains the draft, but browser restart clears it. Popup closure can interrupt an in-progress AI request; reopen and retry manually.
- **Save on this device** optionally stores one draft, including entered passenger details, in local extension storage. It is not encrypted or synced. Use a personal Chrome profile. **Delete saved & clear** removes it and clears the working draft.
- **Forget key** removes the session key and optional Google API permission. It does not revoke the key at Google; manage that in Google AI Studio.
- Page access uses `activeTab` only after the extension is opened. The only optional host permission is Google's API. No analytics, telemetry, cookies permission or background scraping.

## Verification and limits
- Run `npm test` in this folder (Node 20+). No dependency installation is required.
- 15 automated tests passed: validation, exact-origin checks, provider request contract/error handling with mocked responses, and DOM-fixture scan/apply protections.
- These are mocked provider and DOM-fixture tests, not an installed-Chrome or live-site end-to-end test.
- Live Gemini inference was not tested because no authorized API key was supplied.
- Live IRCTC compatibility was not verified: the inspection browser returned Access Denied. Test on the actual desktop before relying on this preview. Site markup changes may require adapter updates.
- Not published to the Chrome Web Store. Load unpacked for this preview; production distribution needs store review and ongoing compatibility testing.
- Independent software, not affiliated with IRCTC. No guarantee of booking, availability or confirmation.

Official implementation references:
- https://ai.google.dev/api/generate-content
- https://developer.chrome.com/docs/extensions/develop/concepts/activeTab
- https://developer.chrome.com/docs/extensions/reference/api/storage
