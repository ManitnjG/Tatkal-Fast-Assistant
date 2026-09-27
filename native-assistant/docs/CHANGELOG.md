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
