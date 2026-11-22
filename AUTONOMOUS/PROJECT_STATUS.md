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
| OP-10 | Close primary-source gaps listed in docs/RECHARGE.md | BACKLOG | NPCI 123Pay menu, Paytm handle retirement date, BSNL exact code |
| OP-11 | Rewrite all owned docs as PWA-only with no Android or note feature | COMPLETED | 8 owned docs files rewritten in this task |

## Architecture decision

No server backend, by design. A server would break the no network calls and no-accounts promise. Collecting customer money and settling to an operator would make OpenPay an unlicensed payment aggregator. The "backend" is a pure rules layer in `site/webapp.js`: rail choice, recharge builders and validation. All state stays in the browser.

## Pages deploy

The last Pages deploy failed because two rapid pushes to main hit the same `pages` concurrency group with `cancel-in-progress: true`. One push cancelled the other's artifact before deployment, so the published site did not update. Root will re-run the deploy with a single push. The workflow itself is unchanged.

## Resource note

Machine measured at 4 CPUs, load 8-9, 3.8 GB free, 2.3 GB swap used, with other root sessions active. Concurrency reduced to 2 implementation subagents plus the root; builds serialized.
