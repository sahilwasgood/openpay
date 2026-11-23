# OpenPay handoff

Stable state as of 2026-11-22. This is a PWA-only project. There is no Android app, no APK, no F-Droid metadata and no promissory note feature. Those were deliberately removed; do not restore them.

## What works

- Installable offline PWA at `site/app.html`, serviced by `site/sw.js`.
- Pay tab: UPI intent, USSD `*99#` handoff, UPI 123Pay IVR handoff.
- Recharge tab: handoff builders for SBI Quick SMS, Axis SMS, UPI 123Pay voice and operator self-service menus.
- Local history, recent payees, QR paste autofill, share-target autofill, English and Hindi, elder mode, backup, restore and wipe.
- No videos ship for now. The Watch section and rendered films were removed; the four portrait HyperFrames source projects remain in `brand/videos/` for a later pass.

## Run and verify

```
node --test tests/*.test.js
python3 tests/browser-smoke.py
cd site && python3 -m http.server 8000
```

The Node suite has 23 tests over the marked pure blocks in `site/webapp.js`: `@recharge-core-start`, `@parse-upi-start`, `@rail-start`. Keep those markers. The browser smoke test uses system Chrome when present and skips otherwise.

## Verified

- 22 Node tests pass locally and in CI (`.github/workflows/test.yml`).
- Browser smoke test passes with system Chrome over `file://`.
- True offline reload verified over local HTTP with a controlled service worker.
- No external asset requests; fonts are vendored in `site/fonts/` and cached by the service worker.
- Pages deploys succeed; the recurring "Found 0 artifacts" failure was an API metadata race, fixed with a 20-second wait in `deploy-site.yml`.
- Live service worker was `openpay-v33` at the last check.

## Remaining blockers

- Real-handset SMS, USSD and IVR flows cannot be tested here; they need a SIM and bank account.
- iOS Safari and iPadOS SMS URI behaviour is inferred from detection logic, not device-tested.
- Bharat Connect operator-level biller coverage could not be confirmed because their live directory returned HTTP 500; those rows are marked unverified in `docs/RECHARGE.md`.
- Browser smoke is not in CI; it would need Playwright and Chrome setup on runners.

## Repository

`sahilwasgood/openpay` on GitHub, public, branch `main`, active CLI account `sahilwasgood`. Never switch the global GitHub account while other sessions are working.
