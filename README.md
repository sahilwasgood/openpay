# OpenPay

UPI payments that work with zero signal.

OpenPay is an open source Android app and web app for India. You never pick a rail: the app reads your network and carrier and hands the payment to the strongest option that still works.

| Rail | Needs | Limit | Notes |
|---|---|---|---|
| UPI Intent | Data | Your bank's limit | Hands off to any installed UPI app |
| USSD *99# | Signal | Rs 5,000 | Airtel, Vi, BSNL. Jio carries no USSD |
| IVR · UPI 123Pay | Signal | Rs 10,000 | The Jio path, keyed in with dial tones |
| OpenPay Note | Nothing | Rs 2,000 each | A signed promissory note, settled later |

## A Note is a promissory note

The kirana shop already solved this: udhaar goes in the khata book, both sides trust the record, it is settled later. A Note is the same slip, digital. The payer's phone signs the amount, payee, serial and expiry with an Ed25519 key that never leaves the phone. The payee verifies the signature, expiry, cap and replay state fully offline, by app or in [any browser](https://sahilwasgood.github.io/openpay/verify.html). With signal back, one tap settles over normal UPI. No money is held, ever. Strangers are capped at Rs 500 with explicit accept, blocked keys are refused on sight.

## Trust, in the manifest

- No internet permission. The app cannot reach any server. Check the manifest, not the marketing.
- Ledger encrypted with an Android Keystore key. Bank SMS close a payment only inside a six minute window, matched by sender, debit wording and exact amount.
- Open source, Apache 2.0. One release APK per version, signed by CI.

## Fees

Person to person UPI is free, and every OpenPay payment is person to person: zero MDR, any amount. The 0.4% MDR from Oct 15, 2026 applies to large merchants on merchant transactions above Rs 2,000, on their side. Splitting one purchase to dodge it is prohibited by NPCI and saves nobody anything. The [site](https://sahilwasgood.github.io/openpay/#mdr) has the calculator.

## Install

- Android, the product: F-Droid via [IzzyOnDroid](https://apt.izzysoft.de/fdroid/) is the distribution path being submitted (submission file: `docs/izzyondroid-submission.md`), it installs cleanly on phones where Play Protect blocks sideloads. Until it is listed, the signed APK from [Releases](https://github.com/sahilwasgood/openpay/releases) works where sideloading is allowed: one file per version named `OpenPay-vX.Y.Z.apk`, SHA-256 in the notes. Play Protect either offers More details, Install anyway, or blocks outright depending on device. If an OpenPay from before v0.4.0 is installed, uninstall it first, the signing key changed.
- Web, the product with no download: [sahilwasgood.github.io/openpay/app.html](https://sahilwasgood.github.io/openpay/app.html) pays through the same rails (UPI deep links, `tel:` USSD and 123Pay), QR scan autofill, contacts, recent payees, writes and verifies Notes with a WebCrypto key, settles notes by UPI link, takes shared note text straight into Verify (Android share menu), English and Hindi, elder mode, backup and restore, and installs as an app that works offline. Browsers cannot read bank SMS, so payment confirmation there is manual. The Android app adds automatic SMS confirmation and the encrypted keystore.

## Fork it

Kotlin and Compose, one framework free site, 48 tests, CI that signs and releases on every tag. [CONTRIBUTING.md](CONTRIBUTING.md) is the whole map, `docs/ARCHITECTURE.md` is the system, `CLAUDE.md` makes any coding agent productive in this repo in minutes. Fork it and ship your first change in an evening.

## Repo layout

- `android/` the app
- `site/` the site and PWA
- `docs/ARCHITECTURE.md` the system map
- `RESEARCH.md` the UPI research this is built on
- `PRODUCT.md` the product spec

## License

Apache 2.0. *99# and UPI 123Pay are NPCI services. OpenPay is not a bank and not a PSP. Payments settle on UPI rails through your bank.
