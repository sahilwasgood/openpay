[x] the app is not already listed at *IzzyOnDroid*

### Package name:
app.openpay

### Link to app in another app store:
None, this is the initial listing request. Source and releases: https://github.com/sahilwasgood/openpay (tagged vX.Y.Z, one APK per release named OpenPay-vX.Y.Z.apk, SHA-256 in the release notes, CI-signed)

### Category and group:
* Money

Summary: UPI payments that work with zero signal.

OpenPay hands every payment to the strongest rail that still works: UPI intent when data exists, USSD *99# when only signal does, the UPI 123Pay IVR line for Jio, and signed promissory notes (Ed25519, verified fully offline) when nothing does. Notes settle over normal UPI later, like udhaar in a digital khata. No internet permission: the app cannot reach any server, verifiable in the manifest. Encrypted local ledger, bank SMS confirmation inside a strict window, private on-device spending log, bill splitting into fair-share notes, double debit guard, QR scan autofill, contact payments, balance check, English and Hindi, elder mode. Apache-2.0, 48 unit tests.

Fastlane metadata (title, descriptions, icon, changelogs) is in the repo at fastlane/metadata/android/en-US/. No trackers, no ads, no analytics.

### Useful additional links:
* Website / web app: https://sahilwasgood.github.io/openpay/
* Architecture map: https://github.com/sahilwasgood/openpay/blob/main/docs/ARCHITECTURE.md
* Icon (512): https://raw.githubusercontent.com/sahilwasgood/openpay/main/fastlane/metadata/android/en-US/icon.png
