# CLAUDE.md

Instructions for coding agents working in this repository. Human forkers: `CONTRIBUTING.md`. System map: `docs/ARCHITECTURE.md`.

## Build and test

```
cd android
./gradlew test assembleDebug        # everything, JDK 17+, SDK platform 34
./gradlew assembleRelease           # signed if keystore.properties exists (gitignored)
```

Local toolchain on this machine: Gradle 8.7 at `~/tools/gradle-8.7`, Android SDK at `~/android-sdk`. CI does the same on ubuntu-latest.

## Layout

- `android/app/src/main/java/openpay/app/rails/` payment rail logic, pure Kotlin, unit tested. RailDecider, IntentRail, UssdRail, IvrRail, SmsParser, SmsConfirmations, Guards, Mdr, ExpenseParser.
- `android/app/src/main/java/openpay/app/notes/` the promissory note instrument. Notes (Ed25519, canonical string), NotesStore, PayerLimits, Split, NoteQr.
- `android/app/src/main/java/openpay/app/data/` persistence. Ledger and Vault (EncryptedFile), Banks, ExpenseStore.
- `android/app/src/main/java/openpay/app/ui/` Compose screens. Lang.kt is the string table.
- `android/app/src/test/` the tests. Run them before pushing anything.
- `site/` the site and PWA, no build step.
- `.github/workflows/` CI: tests on push, signed release on v* tags, Pages on site/ changes.

## Rules

1. The app ships without the internet permission. Nothing that requires the network inside the app is acceptable, ever.
2. Money rules go in pure Kotlin under `rails/` or `notes/` with unit tests. Screens call them. No payment logic in composables.
3. Strings: English literals are the keys in `ui/Lang.kt`. Use `s()` in composables, `ts()` inside callbacks and TTS. Add the Hindi entry when you add a string, missing entries fall back to English safely.
4. Copy style, everywhere including commit messages and docs: no em or en dashes, use the middle dot · or a comma. No filler phrases, no claims the code does not make.
5. Kotlin namespace and applicationId are both `openpay.app`. Never use the package prefix `in.` anywhere, `in` is a Kotlin keyword.
6. Ed25519 canonical string for notes is frozen: pipe joined `openpay-note|v|from|payerKey|to|vpa|phone|amountPaise|serial|nonce|expiresEpoch|memo`. The browser verifier in `site/verify.html` must stay byte compatible.
7. Release signing: keystore in `android/keystore/` (gitignored) or gradle properties `storeFile/storePassword/keyAlias/keyPassword`. Store and key password must be identical (Android rejects split PKCS12 passwords).
8. Version bumps: `versionCode` and `versionName` in `android/app/build.gradle.kts`, tag `vX.Y.Z` pushes a signed release automatically. The release carries the release APK only.
9. Service worker cache id in `site/sw.js` must be bumped on every site change so installed PWAs swap caches atomically after one online visit. Bump the `?v=` suffix on `styles.css` and `app.js` in both HTML files in the same change, HTTP caches can otherwise serve the old assets for up to ten minutes.

## Useful facts

- Jio phones have no USSD, the decider sends them to the IVR rail.
- Bank SMS confirmation only counts inside a 6 minute window with matching sender, debit wording and exact amount.
- Notes cap at Rs 2,000 each, Rs 10,000 open, first notes from unknown keys cap at Rs 500 with explicit consent.
- Network flakiness on this machine is real: retry loops with sleeps for anything hitting GitHub or Maven.
