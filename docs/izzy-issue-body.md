* [x] The app complies with the inclusion criteria as listed [in the README](https://gitlab.com/IzzyOnDroid/repo/blob/master/README.md#what-are-the-requirements-an-app-must-meet-to-be-included-with-the-repo)
* [x] The app is not already listed in the repo or issue tracker

---------------------

### Link to the source code:
https://github.com/sahilwasgood/openpay

### Link to app in another app store:
None yet, this is the first listing request.

### License used:
Apache-2.0

### Category:
Money

### Summary:
UPI payments that work with zero signal. OpenPay hands every payment to the strongest rail that still works: UPI intent when data exists, USSD *99# when only signal does, the UPI 123Pay IVR line for Jio, and Ed25519 signed promissory notes verified fully offline when nothing does. Notes settle over normal UPI later.

Notes for review:
- APKs are attached to tagged GitHub releases (vX.Y.Z) as OpenPay-vX.Y.Z.apk, signed with the project's release key (no debuggable/testOnly), SHA-256 printed in each release's notes: https://github.com/sahilwasgood/openpay/releases
- The app ships WITHOUT the INTERNET permission: it cannot reach any server, verifiable in the manifest. No trackers, no ads, no ATS elements (it processes payment records, so we hold it to the strictest ATS reading).
- No cleartext traffic flag, nothing downloads executables, unique packageName `openpay.app` and displayName `OpenPay`.
- Fastlane metadata (title, short/full description, icon, changelogs) lives in the repo at `fastlane/metadata/android/en-US/`.
- APK size ~3.6 MB.
