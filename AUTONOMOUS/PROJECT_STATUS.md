# OpenPay autonomous status

Date: 2026-11-22. Owner: sahilwasgood. Repo: github.com/sahilwasgood/openpay (public, preserved).

## Product decision

OpenPay is a single installable PWA. The Android app, APK, F-Droid and Play Protect work, the Kotlin rules layer, Gradle, the 60 Kotlin tests and any iOS companion are rejected and removed from the product. The promissory note instrument (offline IOU, khata, serial and Ed25519 note, replay tracking, browser verifier) is also removed. No task below builds on those features.

## Task board

| ID | Task | Status | Evidence |
|---|---|---|---|
| OP-01 | Fix docs vs code claims (auto settle, browser replay, 123Pay cap, SQLCipher, PDF, version drift) | REJECTED | Android-era claim list, superseded by the PWA-only docs rewrite |
| OP-02 | Enforce Rs 5,000 USSD cap in the Android rail decider | REJECTED | Kotlin test work, removed per the PWA-only decision |
| OP-03 | All-operator offline recharge research | COMPLETED | Bank-official SBI Quick and Axis SMS pages verified, docs/RECHARGE.md |
| OP-04 | Android recharge domain/rules layer | REJECTED | Kotlin work replaced by pure builders in the PWA |
| OP-05 | Liquid glass recharge screen (Android) | REJECTED | Android UI work removed |
| OP-06 | Liquid glass recharge pane (web PWA) | COMPLETED | site/app.html, webapp.js, styles.css, recharge handoff in the PWA |
| OP-07 | Docs for handoff and no-backend decision | COMPLETED | README, PRODUCT, RESEARCH, ARCHITECTURE, docs/RECHARGE.md rewritten PWA-only |
| OP-08 | Full Android build and assembleDebug verification | REJECTED | No Gradle, no APK and no 60 Kotlin tests in a PWA-only product |
| OP-09 | Handset verification of SMS/IVR flows | BACKLOG | Needs a real SIM and bank account |
| OP-10 | Close primary-source gaps listed in docs/RECHARGE.md | COMPLETED | Superseded by OP-15, three of four gaps closed with primary sources and the rest flagged unverified |
| OP-11 | Rewrite all owned docs as PWA-only with no Android or note feature | COMPLETED | 8 owned docs files rewritten in this task |
| OP-12 | Strip notes and APK references from the PWA, site and brand docs | COMPLETED | webapp.js 999 to 668 lines, app.html 290 to 236, styles.css 804 to 730, note verifier and screenshots removed |
| OP-13 | Replace four brand videos with four portrait liquid glass films | COMPLETED | reel-pay, reel-recharge, reel-ivr, reel-trust; all four check clean and render 1080 by 1920, copied to site/videos |
| OP-14 | Commit regression tests for the recharge rules | COMPLETED | tests/recharge.test.js, 7 assertions over the marked pure block in webapp.js, all passing |
| OP-15 | Close primary-source gaps in docs/RECHARGE.md | COMPLETED | Paytm 1 Aug 2024, BSNL *123#, Jio self service and UPI 123Pay voice flow now sourced; operator coverage explicitly flagged unverified |
| OP-16 | Browser smoke test of the PWA | COMPLETED | Playwright with system Chrome over file://: four tabs present, no note or verify tab, SBI handoff links correct, over-cap warning works, zero console errors, four tabs fit one phone row |
| OP-17 | Remove third-party assets and add offline regression tests | COMPLETED | tests/offline.test.js, local fonts in site/fonts, Google Fonts links removed, fonts added to SW shell, browser confirms zero external requests, 14 tests pass |
| OP-18 | Accessibility pass on the PWA | COMPLETED | No unlabelled visible inputs or buttons, aria-label on the collapsed dial link, global keyboard focus outline verified in the browser |
| OP-19 | Functional browser pass over Pay, Settings and the landing demo | COMPLETED | UPI link built correctly, Hindi and elder toggles work, payment recorded in history, signal and Jio rail demo copy switches correctly, toast made click-through |
| OP-20 | Data flow browser pass | COMPLETED | QR paste autofills VPA, name and amount; export blob contains all op_ keys; wipe clears storage; restore from JSON brings the payment back |
| OP-21 | Harden QR paste against malformed input | COMPLETED | tests/parse-upi.test.js, 3 new tests, malformed percent-encoding now returns null and shows the no-UPI toast, 17 tests pass |
| OP-22 | Verify true offline reload | COMPLETED | Local HTTP server, service worker controls the page, cache openpay-v22, offline reload renders all four tabs |
| OP-23 | Reject fractional rupee amounts | COMPLETED | All three bank handoff builders now require whole rupees, one regression test added, 18 tests pass |
| OP-24 | iPadOS SMS handoff detection | COMPLETED | Macintosh user agents with multi-touch now use the iOS &body= separator, verified by test, 19 tests pass |
| OP-25 | Share target with embedded UPI link | COMPLETED | URL anywhere in shared text now fills VPA, name and amount, verified in browser, 20 tests pass |
| OP-26 | IVR rail decimal guard | COMPLETED | 123Pay DTMF links now require whole rupees with a clear hint, UPI intent keeps paise, verified offline in browser, 22 tests pass |
| OP-27 | Commit a browser smoke test | COMPLETED | tests/browser-smoke.py covers tabs, QR autofill, history, recharge cap, share target and landing demo; passes with system Chrome |
| OP-28 | Enforce tests in CI | COMPLETED | .github/workflows/test.yml runs node --test on every push and PR, first run 45072404675 succeeded |
| OP-29 | Language accessibility attribute | COMPLETED | Switching to Hindi now sets html lang to hi, smoke test asserts both directions, 22 tests pass |
| OP-30 | Dynamic text follows language switch | COMPLETED | Rail copy, recent payees and history re-render on language change, Hindi rail strings added, smoke test asserts it |
| OP-31 | History labels translated | COMPLETED | Rail, pending/paid state and action labels use t(), empty state translated, smoke test asserts Hindi status in history |
| OP-32 | Locale-aware dates | COMPLETED | History and export dates use hi-IN or en-IN according to language; Chrome renders Latin digits for hi-IN, so no digit assertion is possible |
| OP-33 | Translate remaining toasts | COMPLETED | Five hardcoded English toasts now use t() with Hindi strings, verified in browser |
| OP-34 | Translate Pay rail labels | COMPLETED | Rail action texts, empty button label and net/carrier line follow language, one unit test added, 23 tests pass |
| OP-35 | Final string audit | COMPLETED | Carrier toggle and history Rs prefix now translated, carrier label follows language switch, smoke test asserts both languages |
| OP-36 | Token-first UI redesign | COMPLETED | Material 3 colour roles, one rem type scale, sentence-case labels, bottom tab bar, real glass only on the control layer, focus and reduced-motion/transparency fallbacks |
| OP-37 | Remove videos for now | COMPLETED | Watch section and rendered MP4s removed, HyperFrames sources kept for later |

## Architecture decision

No server backend, by design. A server would break the no network calls and no-accounts promise. Collecting customer money and settling to an operator would make OpenPay an unlicensed payment aggregator. The "backend" is a pure rules layer in `site/webapp.js`: rail choice, recharge builders and validation. All state stays in the browser.

## Pages deploy

The "Found 0 artifacts" failure was traced: `upload-pages-artifact` succeeds, then `deploy-pages` queries artifact metadata before GitHub's API has caught up. A 20 second pause between those steps fixes it. Deploy 45067654947 then succeeded, and every later push deploy has passed. The latest live `sw.js` confirms `openpay-v33`. This replaces the earlier cancellation-race theory.

## Final verification

No videos ship in the site. `node --test tests/*.test.js` reports 23 pass and 0 fail, and `python3 tests/browser-smoke.py` passes. Manifest and every service-worker shell entry resolve on disk. Live service worker was v33 before the redesign.

## Resource note

Machine measured at 4 CPUs, load 8-9, 3.8 GB free, 2.3 GB swap used, with other root sessions active. Concurrency reduced to 2 implementation subagents plus the root; builds serialized.
