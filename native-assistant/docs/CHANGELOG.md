# Unreleased — train-search compatibility
- Open the user-supplied official `/eticket/train-search` URL.
- Recognize exact short From, To, Mobile and Mobile No. labels and inspect accessibility descriptions/HTML titles.
- Preserve authenticated review, exact-origin checks and security-field exclusions.
- Live IRCTC validation remains pending: the inspection browser returned Access Denied. Station suggestions still require the user to select and verify the station; typing a code alone does not establish a selected station.

# v0.3.0
- Added authenticated encrypted portable backup/restore and atomic conservative merge.
- Added grouped sibling autofill with partition/origin/ambiguity checks.
- Added local setup diagnostics and preferred verified-Chrome handoff.
- Added explicit confirmation-paste PNR extraction without automatic payment resolution.
- Separated preview package so old installations can be retained.
- Added optional stable preview signing and release certificate checks; API 26/29/35 CI matrix.
- Added backup, grouping, parsing and Android storage tests.

# 0.2.0

- Added Android AutofillService, device-authenticated review and exact-origin/signer checks.
- Added Profile setup controls and English/Tamil autofill/reconciliation text.
- Added separate booking-history and payment verification controls; inconsistent/pending outcomes remain locked.
- Persisted payment evidence and verification time with backward-compatible serialization defaults.
- Added pure policy tests for prohibited fields/origins, selected passengers and reconciliation, plus device tests for encrypted recovery and one-use request tokens.
- Retained all previous profiles, Room schema and booking state semantics.
