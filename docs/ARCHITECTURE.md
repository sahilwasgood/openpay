# OpenPay architecture

One sentence: a payer side Android app that hands every payment to the strongest rail that still works, keeps an encrypted local ledger as the source of truth, and extends trust offline through signed promissory notes.

## The four rails

`android/app/src/main/java/openpay/app/rails/RailDecider.kt` is the whole decision, pure and unit tested:

1. **Intent** (online): `IntentRail` builds a `upi://pay` deep link and launches any installed UPI app. OpenPay never does networking.
2. **USSD** (signal, no data, not Jio): `UssdRail` dials `*99*1*3#` encoded for the dialer. The user finishes the carrier menu. Works on Airtel, Vi, BSNL. Jio carries no USSD.
3. **IVR** (signal, Jio or phone only payee): `IvrRail` builds the DTMF string for the UPI 123Pay line (080 4516 3666), comma pauses keyed in automatically. Cap Rs 10,000.
4. **Notes** (nothing): signed promissory notes, see below.

`util/Net.kt` classifies connectivity as ONLINE (validated), SIGNAL (connected, nothing validated), NONE. The decider reads network plus carrier plus what the payee has (VPA or phone) and picks.

## Confirmation truth

The carrier rails give a third party app no callback. The only honest success signal is the user's own bank SMS:

- `data/Ledger.kt`: encrypted JSON payment list (EncryptedFile, AES256 GCM, Android Keystore). Every payment is written PENDING before the dial goes out.
- `sms/SmsReceiver.kt` receives SMS, `rails/SmsConfirmations.kt` (pure, tested) decides: newest pending non intent payment, inside the 6 minute window, sender matches the user's bank (`data/Banks.kt` short codes), body reads as a debit of the exact amount (`rails/SmsParser.kt`).
- `rails/Guards.kt` blocks same looking retries inside the window: the double debit guard, in both the pay flow and pay again.

## Notes: promissory notes

`notes/Notes.kt` is the instrument. A payload (payer name, payer public key, payee, amount, serial, nonce, expiry, memo) is signed with Ed25519 over a canonical pipe joined string. `NotesStore.kt` keeps the keypair (encrypted vault), issued notes, receivables, pinned payer keys, blocked payer keys and seen serials.

- Issue: payer signs, shows QR (`NoteQr.kt` renders with zxing).
- Verify: payee scans, `Notes.verifyNote` checks signature, expiry, the Rs 2,000 cap, replay by serial, all offline. First notes above Rs 500 from unknown keys need explicit merchant consent (`notes/PayerLimits.kt`). Blocked keys are refused loudly.
- Settle: with signal, one tap per note or the settle all queue opens the UPI app prefilled. `rails/Mdr.kt` warns when a split looks like prohibited structuring.
- Cross platform: `site/verify.html` verifies the same signature in the browser with WebCrypto. The canonical string must stay byte identical across Kotlin, browser JS and anything else.

Limits: Rs 2,000 per note, Rs 10,000 open at once, notes expire. No money is ever held.

## Around the rails

- `data/ExpenseStore.kt` plus `rails/ExpenseParser.kt`: bank debit SMS become a private spending log, on device, toggle in settings.
- `notes/Split.kt`: paise fair bill splitting into one note per person.
- `ui/Lang.kt`: English literals are the keys, Hindi the map, `s()` composable, `ts()` for callbacks.
- `ui/theme/Theme.kt`: brand tokens and the elder mode 1.28x type scale.

## The recharge handoff

Recharge is not one of the four payment rails. It is a separate handoff: the domain/rules layer builds a bank SMS body with its destination number, or a `tel:` dial string for USSD and IVR, and the user sends it or completes the menu on the handset. The bank or operator moves the money. OpenPay never does. The path table, codes, operators, limits, sources and liveness are in [RECHARGE.md](RECHARGE.md).

Domain/rules layer is the name for what a conventional app would call a backend: pure code that picks a path, validates amounts and formats the handoff. It runs on the device. The Android app can read bank SMS to confirm a payment inside a window, because SMS is a platform API there. A browser has no such API, so the web app builds `sms:` and `tel:` links and confirmation there is manual. No recharge path needs a network call from OpenPay.

## Decision: no server backend

This is deliberate, not a missing feature.

- A server would break the privacy promise. The manifest has no INTERNET permission, and there are no accounts, no analytics and no tracking. A backend would make those claims false and would need the permission back.
- A collection and settlement flow would make OpenPay an unlicensed payment aggregator. If OpenPay took money from a payer and settled with the operator, it would be collecting and settling payments. It does neither. Every path in [RECHARGE.md](RECHARGE.md) moves money directly from the payer's bank account or prepaid balance to the operator, with the user sending the instruction from their own handset.

The domain/rules layer stays local, pure and unit tested, under `rails/` and any recharge path code, with the payment rule intact: no money logic in composables, no network code in the app.

## The site

`site/` is deliberately framework free. `index.html` (the ladder, notes explainer, MDR calculator, FAQ), `verify.html` (browser verifier), `sw.js` (network first when online, cache when offline, cache id bumped every deploy), `manifest.webmanifest` (installable, screenshots in a deliberate order). Deploys through `.github/workflows/deploy-site.yml` to GitHub Pages.

## CI

`.github/workflows/ci.yml`: `./gradlew test` on every push, signed `assembleRelease` on `v*` tags, release published with the release APK only. Secrets hold the keystore.

## Invariants worth defending

1. No internet permission in the app manifest, ever.
2. No money custody, no server, no accounts, no collection or settlement.
3. Money rules live in pure tested Kotlin, not in composables.
4. The canonical note string never changes without a version bump.

## The web app (v0.8 site)

`site/app.html` plus `site/webapp.js` is the product in the browser, no download needed:

- Pay tab: the same rail ladder as links. Online builds a `upi://pay` deep link into any installed UPI app. Signal only builds `tel:*99*1*3%23` (USSD) or the 123Pay DTMF `tel:` string for Jio and phone only payees. Carrier is a manual toggle, browsers do not expose it.
- Notes tab: Ed25519 keypair generated by WebCrypto on first use, private key JWK in localStorage (less hardened than the Android Keystore, the honest tradeoff of a browser). Signs the same canonical string, renders QR (vendored `vendor/qrcode.js`, MIT), copy and share.
- Verify tab: camera scan via BarcodeDetector where supported, paste everywhere else, signature and replay checks identical to the app, payer pinning and blocking in localStorage.
- History tab: payments and notes in localStorage. Browsers cannot receive SMS, so confirmation is manual marking.

`manifest.webmanifest` start_url points at `app.html`: installing the PWA installs the product. The marketing site stays the manual. The canonical note string and its rules are shared byte for byte across Kotlin, webapp.js and verify.html, that invariant is what makes all three interoperate.

### Web app additions (current site)

Settings tab (name persistence, English and Hindi, elder mode zoom 1.22, backup export and restore of all `op_` localStorage keys including the signing key, wipe, key fingerprint), manifest share_target (shared note text lands straight in Verify, shared `upi://` text autofills Pay), home screen shortcuts via `app.html#pay|#notes|#verify`, per open note a settle link built from the note's own VPA with the serial as the transaction note, and recent payee chips. Hindi strings live in `webapp.js` in the `HI` map, English originals are captured from the DOM at boot, `data-i` attributes are the hooks.
