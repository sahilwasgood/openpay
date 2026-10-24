# IzzyOnDroid submission

IzzyOnDroid is the standard route for GitHub open source Android apps: it serves the project's own release APKs through the F-Droid client, so users install from a known store and Play Protect does not stand in the way. Submission is one issue on their repo. The fields below are filled in and ready to paste into the template at:

https://github.com/IzzyOnDroid/repo/issues/new?template=app_submission.yml

They pick up future releases automatically from the GitHub releases this repo already publishes, `OpenPay-vX.Y.Z.apk`, signed with the project key.

- Repo: `sahilwasgood/openpay`
- App ID: `app.openpay` (reverse of the applicationId `openpay.app`)
- Category: Money
- License: Apache-2.0
- Summary: `UPI payments that work with zero signal`
- Description:

```
OpenPay hands every payment to the strongest rail that still works: UPI intent
when data exists, USSD *99# when only signal does, the UPI 123Pay IVR line for
Jio, and signed promissory notes (Ed25519, verified fully offline) when nothing
does. Notes settle over normal UPI later. No internet permission: the app cannot
reach any server. Encrypted local ledger, bank SMS confirmation inside a strict
window, private spending log, bill splitting, double debit guard, English and
Hindi, elder mode.
```

- Source code: https://github.com/sahilwasgood/openpay/tree/main/android
- Releases track the tag `vX.Y.Z`, one APK per release named `OpenPay-vX.Y.Z.apk`, SHA-256 in the release notes
- App logo: `site/icons/icon-512.png`
- Author: sahilwasgood
- Web: https://sahilwasgood.github.io/openpay/
- Trackers, ads, analytics: none. The app ships without the INTERNET permission, verifiable in the manifest.

F-Droid proper (the main repo, building from source with their own key) can follow later: it needs reproducible builds, a roadmap item. IzzyOnDroid works with today's release pipeline.

Play Store note for later: RECEIVE_SMS is a restricted permission on Play and needs a declaration plus review for the confirmation feature. If Play rejects it, the store build ships without SMS confirmation and leans on the pending tracker, while F-Droid keeps the full build.
