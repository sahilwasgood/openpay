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

## Architecture decision

No server backend, by design. A server would break the no network calls and no-accounts promise. Collecting customer money and settling to an operator would make OpenPay an unlicensed payment aggregator. The "backend" is a pure rules layer in `site/webapp.js`: rail choice, recharge builders and validation. All state stays in the browser.

## Pages deploy

An earlier Pages deploy failed because two rapid pushes hit the `pages` concurrency group with `cancel-in-progress: true`; one run cancelled the other's artifact upload. Fixed by setting `cancel-in-progress: false`, then pushing once (commit 5870c05). Push deploy 45057467658 and manual deploy 45057489709 both succeeded. Cache-busted fetches of `sw.js` and `app.html` from the live domain return the new v16 PWA-only build; uncached edge responses can still show the previous version for a short TTL.

## Resource note

Machine measured at 4 CPUs, load 8-9, 3.8 GB free, 2.3 GB swap used, with other root sessions active. Concurrency reduced to 2 implementation subagents plus the root; builds serialized.
