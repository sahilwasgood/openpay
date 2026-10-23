# OpenPay: Product Spec v0

Pay when the network cannot. An open source Android app and site for UPI payments that degrade gracefully from full data to zero signal, with deferred settlement that clears itself.

One line: **scan or type, OpenPay picks the best rail that still works, and every payment either confirms or tells you exactly what happens next.**

## Positioning

Not another USSD wrapper. The two reference apps (Flowpay, OffPayApp) each implement one slice and stop. OpenPay is the full ladder plus the payee side, built to be the app people actually install before a trip, a concert, or life in a low signal town.

Non-goals: we are not a TPAP, not a wallet, never hold funds, no accounts on a server, no analytics, ads, or tracking. Open source, reproducible builds, APK on GitHub Releases and the site.

## The core mechanic: the rail ladder

Every payment runs the same decision:

1. **Intent (data available)**. Hand off to any installed UPI app via `upi://pay` with prefilled VPA, amount, note. We orchestrate; the user's bank app does the network.
2. **USSD (no data, GSM signal, not Jio)**. Dial `*99#` flows. QR payload fills the amount; the carrier session completes with PIN.
3. **IVR (Jio, or USSD failed)**. 123Pay IVR `080 4516 3666` with sequenced DTMF payload (recipient phone, amount, confirm). Fixes the gap both repos leave: Jio is India's largest carrier.
4. **OpenPay Note (zero signal, or every rail failed)**. A signed offline voucher: amount, payee, nonce, serial, expiry, created in under 3 seconds. The payee verifies it offline against the payer's public key. When either phone regains connectivity, the payer's app fires the UPI intent to settle and both ledgers reconcile automatically.

Confirmation truth, inherited from Flowpay and hardened: pending row written before dial, bank SMS captured only inside a tight operation window, sender ID checked against the known bank shortcode (both reference apps skip this), dedup by amount and window, "no confirmation, no record" honesty.

## Feature set

v1 (the wedge)
- One box: scan QR (ZXing + CameraX, offline), type UPI ID or phone, or pick a contact
- Rail ladder 1-3 with live network and carrier detection; Jio detection up front
- Signed OpenPay Notes: create, verify, settle; keys exchanged by QR or NFC when parties meet; Ed25519 signatures, serial + nonce replay protection, per Note cap ₹2,000 and ₹10,000 outstanding (RBI small value offline comfort zone)
- Merchant mode: accept Notes offline, offline verify, TTS voice confirmation (soundbox alternative), pending settlement queue that clears automatically
- Encrypted local ledger (SQLCipher + Keystore), CSV and PDF export
- Pending tracker: every unconfirmed outbound payment shows the NPCI clock (T+1 auto reversal, T+5 refund deadline, ₹100/day penalty) with one tap escalation templates (bank email, upihelp.npci.org.in link)
- No INTERNET permission at all. Settlement goes through the UPI intent handoff, which runs in the user's installed UPI app. Verifiable claim, not a promise
- English + Hindi at minimum; architecture ready for 8 more languages (both repos failed here)

v2
- Split a bill into Notes; group settle
- NFC tap for Note exchange and merchant accept (HCE), UPI Lite X style
- UPI Lite awareness and topup assist
- Feature phone companion path (123Pay shortcuts, missed call pay)
- Elder mode: giant type, single screen, voice prompts

Deliberately out of scope: holding money, private tokens as currency, mesh networking of payments, credit, crypto. Legal note: Notes are deferred payment promises between the two parties, settled on NPCI rails; caps stay small; get a legal review before marketing at scale.

## Trust model

- No server, no accounts, no INTERNET permission, no analytics: open source and reproducible builds make this checkable
- Notes are signed, so a merchant holding a verified Note holds proof, not a screenshot; the fake screenshot economy dies at the counter
- Every state is explicit: confirmed, pending with clock, failed with reason. No silent retries that cause double debits (the number one community trauma)

## Design principles

- Minimal, to the point, zero slop. One primary action per screen
- Three taps maximum from launch to rail handoff
- Type first: strong numerals for amounts, generous spacing, no decorative anything
- Motion only where it explains state (rail switching, settlement clearing); reference real fintech onboarding and payment confirmation patterns from Mobbin (Revolut, Cash App, CRED, GPay, PhonePe) once the Mobbin MCP is connected
- Dark and light, system fonts plus one grotesk for display; palette: near black, paper white, one signal green, one alert amber

## The site

Single page, static, fast. Sections: hero (one line + download APK), the ladder (how it works, honest about which rail needs what), merchant, trust (open source, no INTERNET permission, reproducible builds), limits and FAQ (caps, Jio, settlement timing), GitHub. No blog, no newsletter popup, no fake testimonials.

## Build order

1. Brand + static site
2. Android MVP: ladder steps 1-3, carrier and Jio detection, encrypted ledger, pending tracker
3. OpenPay Notes (create, verify, settle) + merchant mode with TTS
4. Hindi + languages, exports, elder mode
5. Reproducible APK CI, GitHub Releases

## Naming note

"OpenPay" collides with a defunct Australian BNPL and a Mexican charge processor. For an Indian open source consumer payment tool the collision is low risk; if it ever bothers us, the Note name is the brand asset worth protecting. Domain hunting after v1 works.
