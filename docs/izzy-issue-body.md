### App name
OpenPay

### Package ID
app.openpay

### Category
Money

### Summary
UPI payments that work with zero signal.

### Description
OpenPay hands every payment to the strongest rail that still works: UPI intent when data exists, USSD *99# when only signal does, the UPI 123Pay IVR line for Jio, and signed promissory notes (Ed25519, verified fully offline) when nothing does. Notes settle over normal UPI later, like udhaar in a digital khata.

No internet permission: the app cannot reach any server, verifiable in the manifest. Encrypted local ledger (Android Keystore). Bank SMS confirmation inside a strict window matched by sender, debit wording and exact amount. Private spending log from the user's own bank debit SMS, on device only. Bill splitting into fair share notes. Double debit guard before same looking retries. QR scan autofill, contact payments, balance check. English and Hindi, elder mode with large text, CSV export. 48 unit tests, CI signs every release.

### Website
https://sahilwasgood.github.io/openpay/

### Source code
https://github.com/sahilwasgood/openpay

### License
Apache-2.0

### Releases
Tagged vX.Y.Z, one APK per release named OpenPay-vX.Y.Z.apk, SHA-256 printed in the release notes. https://github.com/sahilwasgood/openpay/releases

### App logo
https://raw.githubusercontent.com/sahilwasgood/openpay/main/fastlane/metadata/android/en-US/icon.png

### Trackers / ads / analytics
None. The app ships without the INTERNET permission.

### Fastlane metadata
Included in the repo at fastlane/metadata/android/en-US/ (title, short and full description, icon, changelogs).
