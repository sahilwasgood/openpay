# OpenPay

UPI payments that work with zero signal.

OpenPay is an open source Android app for India that hands every payment to the strongest rail that still works. You never pick a rail. The app reads your network and your carrier, then starts the payment on the best option available.

| Rail | Needs | Limit | Notes |
|---|---|---|---|
| UPI Intent | Data | Your bank's limit | Hands off to any installed UPI app |
| USSD *99# | Signal | ₹5,000 | Airtel, Vi, BSNL. Jio carries no USSD |
| IVR · UPI 123Pay | Signal | ₹10,000 | Works on Jio. Details keyed in with dial tones |
| OpenPay Note | Nothing | ₹2,000 each | Signed offline voucher. Ships after v0.1 |

Every payment is written to an encrypted local ledger before the call goes out, and a bank SMS inside a tight confirmation window marks it done. No confirmation, no record of success. Stuck payments show the NPCI clocks (T+1 auto reversal, T+5 refund deadline, ₹100 per day penalty) with escalation actions built in.

## Trust

- No `android.permission.INTERNET`. The app cannot reach a server. Check the manifest.
- No accounts, no analytics, no ads, no telemetry.
- Ledger encrypted with an Android Keystore key (AES256 GCM).
- Open source, Apache 2.0.

## Status

v0.5.0 is out: the three network rails (intent, USSD, IVR with DTMF), OpenPay Notes (Ed25519 signed offline vouchers, merchant verify with voice readback), QR scan, encrypted ledger, bank SMS confirmation, pending tracker with NPCI clocks, English and Hindi, elder mode, CSV export, properly signed release builds from CI, and a browser based Note verifier: [sahilwasgood.github.io/openpay/verify.html](https://sahilwasgood.github.io/openpay/verify.html) checks a Note's signature with WebCrypto, no Android app needed. 29 unit tests cover the rails, the parsers, and note signing.

CI runs on every push (tests, debug APK, artifacts) and every tag builds a signed release APK from the encrypted keystore in repo secrets. The site deploys to Pages on every change to site/.

v0.5.0 also shipped: a private spending log (the app reads your own bank debit SMS on device and builds a monthly expense view, the offline expense tracker people keep asking for), bill splitting into fair-share Notes, a double debit guard that warns before a same-looking retry, Pay again from any payment record, and QRs that can be re-shown.

v0.6.0: first time payer consent above Rs 500 (bounded exposure for unknown keys, pinned payers earn room), balance check via *99*3# before issuing, a settle all queue that walks every open note through UPI when you are back online, a stale notes banner after 24 hours, and honest MDR awareness: the split flow warns when a would be single payment above Rs 2,000 is being cut into sub threshold pieces, which NPCI prohibits and which saves nobody anything. The [site](https://sahilwasgood.github.io/openpay/) has a merchant fee calculator and the Rs 2,000 line explained, including why splitting saves no tax.

v0.7.0: payer key blacklist (a merchant blocks a key once and every future note from it is refused loudly, with voice), mobile site fixes (no horizontal scroll down to 360px), one click install for the web app with a fullscreen mobile sheet and seven screenshots, and the fee story completed: person to person payments, which is what OpenPay makes, carry zero MDR at any amount.

Next: more languages, NFC tap for Note exchange, UPI Autopay for recurring settlements.

## Repo layout

- `android/` the app. Kotlin, Jetpack Compose, minSdk 29.
- `site/` the static site. Plain HTML, CSS, and one small JS file.
- `RESEARCH.md` how we got here: the UPI framework, the offline rails, what people complain about.
- `PRODUCT.md` the product spec.

## Install

Grab the APK from [Releases](https://github.com/sahilwasgood/openpay/releases). Android will ask you to allow installs from unknown sources. The app requests three permissions, nothing else:

- `CALL_PHONE` to dial the USSD and IVR numbers for you
- `RECEIVE_SMS` to hear your bank's confirmation inside the payment window
- `CAMERA` to scan UPI QR codes

Your UPI PIN is never seen by OpenPay. It is typed into your bank's USSD menu or the 123Pay call, on the dialer, not in this app.

## What is a Note

A Note is a signed promise, not money. With zero signal, the payer's phone signs an amount, payee, serial and expiry with an Ed25519 key that never leaves the phone, and shows a QR. The payee verifies it fully offline: signature, expiry, the Rs 2,000 cap, and replay by serial. When signal returns, one tap settles it over normal UPI. No money is ever held by OpenPay.

If the payee does not have the Android app, the [web verifier](https://sahilwasgood.github.io/openpay/verify.html) does the same check in any browser. Install the web app once and it verifies offline too. And with even 2G signal, the USSD and IVR rails pay any UPI user with no app on their side at all.

## Build

Android Studio, or from the command line:

```
cd android
./gradlew test assembleDebug
```

Requires JDK 17 or newer and an Android SDK with platform 34.

## License

Apache 2.0. See [LICENSE](LICENSE).

*99# and UPI 123Pay are services of NPCI. OpenPay is not a bank and not a PSP. Payments settle on UPI rails through your bank.
