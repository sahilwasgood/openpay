# OpenPay autonomous status

Date: 2026-11-22. Owner: sahilwasgood. Repo: github.com/sahilwasgood/openpay (public, preserved).

## Task board

| ID | Task | Status | Evidence |
|---|---|---|---|
| OP-01 | Fix docs vs code claims (auto settle, browser replay, 123Pay cap, SQLCipher, PDF, version drift) | COMPLETED | 13 doc/site files changed, stale-claim grep clean |
| OP-02 | Enforce Rs 5,000 USSD cap in rail decider | COMPLETED | TDD red/green, 3 new RailLogicTest cases, 60 tests 0 failures |
| OP-03 | All-operator offline recharge research | COMPLETED | Bank-official SBI Quick and Axis SMS pages verified; docs/RECHARGE.md |
| OP-04 | Recharge domain/rules layer (no server backend) | COMPLETED | RechargeHandoff.kt, 9 tests green |
| OP-05 | Liquid glass recharge screen (Android) | COMPLETED | RechargeScreen.kt, wired into MainActivity and PayScreen |
| OP-06 | Liquid glass recharge pane (web PWA) | COMPLETED | site/app.html, webapp.js, styles.css, 30 builder checks by subagent |
| OP-07 | Docs for handoff and no-backend decision | COMPLETED | README, PRODUCT, RESEARCH, ARCHITECTURE, new docs/RECHARGE.md |
| OP-08 | Full build + assembleDebug verification | COMPLETED | `./gradlew test` 60 tests 0 failures, `./gradlew assembleDebug` BUILD SUCCESSFUL, commit 74f3fa5 pushed |
| OP-09 | Handset verification of SMS/IVR flows | BACKLOG | Needs a real SIM and bank account |
| OP-10 | Close primary-source gaps listed in docs/RECHARGE.md | BACKLOG | NPCI 123Pay menu, Paytm handle retirement date, BSNL exact code |

## Architecture decision

No server backend, by design. A server would break the no INTERNET permission and no-accounts promise. Collecting customer money and settling to an operator would make OpenPay an unlicensed payment aggregator. The "backend" is a pure Kotlin rules layer under `android/.../rails/`, mirrored by pure builders in `site/webapp.js`.

## Resource note

Machine measured at 4 CPUs, load 8-9, 3.8 GB free, 2.3 GB swap used, with other root sessions active. Concurrency reduced to 2 implementation subagents plus the root; builds serialized.
