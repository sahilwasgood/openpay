# OpenPay: Product Spec

Pay when the network cannot. An open source, installable PWA for India that degrades gracefully from full data to signal only, with an honest record of what happened after every handoff.

One line: **scan or type, OpenPay picks the best rail that still works, and every payment either confirms or tells you exactly what happens next.**

## Positioning

Not another USSD wrapper. The two reference apps (Flowpay, OffPayApp) each implement one slice and stop. OpenPay is the full rail ladder, a recharge handoff and a local record, built to be the app people install before a trip, a concert, or life in a low signal town.

Non-goals: we are not a TPAP, not a wallet, never hold funds, no accounts on a server, no analytics, ads, or tracking. No server backend at all: the rules are pure code in the app, what a conventional app would call a backend. We are not a payment aggregator, so there is no collection or settlement flow. A server would break the privacy promise, and collecting from a payer to settle with an operator would make OpenPay an unlicensed payment aggregator. Open source, static site, installable from the browser.

## The core mechanic: the rail ladder

Every payment runs the same decision:

1. **Intent (data available)**. Hand off to any installed UPI app via `upi://pay` with prefilled VPA, amount and note. OpenPay composes the link, the user's UPI app does the network.
2. **USSD (no data, GSM signal, not Jio)**. Dial `*99*1*3#`. QR payload fills the amount; the carrier session completes with PIN. Cap Rs 5,000. Airtel, Vi, BSNL. Jio carries no USSD.
3. **IVR (Jio, or USSD failed)**. 123Pay IVR `080 4516 3666` with a sequenced DTMF payload (recipient phone, amount, confirm) keyed in through the dialer. Cap Rs 10,000. This covers Jio, India's largest carrier.

Carrier is a setting the user controls, because browsers do not expose it. Connection state comes from the browser. Confirmation is honest: the app records a pending entry before it hands off, and the user marks it paid or pending in History after checking their bank. Browsers cannot read bank SMS.

## Recharge handoff

A recharge is a different job from a payment: the payee is an operator and the bank SMS paths need no data or VPA. OpenPay does not send money. The rules layer builds the exact SMS body and destination, or opens the dialer for a USSD or IVR path, and the user sends or confirms it on the handset. The bank debits the payer's account or the prepaid balance and credits the operator. OpenPay holds nothing.

The paths, codes, limits and sources are in [docs/RECHARGE.md](docs/RECHARGE.md), checked against official pages on 2026-11-22. They cover SBI Quick and Axis SMS recharge, the Mobile Recharge menu on the UPI 123Pay IVR, operator self service menus that need no bank money, and two dead paths: the Vi plus Paytm `@paytm` VPA and ICICI SMS recharge. The app builds `sms:` and `tel:` handoffs and the user finishes on the handset.

## Feature set

v1 (the wedge)

- One box: scan a UPI QR (camera where the browser allows it, paste everywhere else), type a UPI ID or phone, or pick a contact where the browser exposes contacts
- Rail ladder 1 to 3 with a carrier setting and a connection state
- Recharge handoff with SBI Quick, Axis SMS, UPI 123Pay IVR and operator codes, built as `sms:` and `tel:` links
- Local History: payments you mark paid or pending, all in browser storage
- The app makes no network calls. It builds `upi://`, `tel:` and `sms:` links. No fetch, no API, no telemetry
- Installable from the browser, works offline after install, state stays in the browser
- English and Hindi, elder mode, backup and restore of browser state

v2

- UPI Lite awareness and topup assist
- Feature phone shortcuts through 123Pay, IVR and missed call pay
- More languages

Deliberately out of scope: holding money, private tokens as currency, mesh networking of payments, credit, crypto.

## Trust model

- No server, no accounts, no network calls, no analytics. Open source and static, so this is checkable
- Every state is explicit: pending after the handoff, paid or pending after you check. No silent retries that cause double debits
- No money custody. The bank, carrier or operator moves it, the user sends the instruction
- Backup is a file you export yourself. Browser storage can be cleared by the browser

## Design principles

- Minimal, to the point, zero slop. One primary action per screen
- Three taps maximum from launch to rail handoff
- Type first: strong numerals for amounts, generous spacing, no decorative anything
- Motion only where it explains state, for example a rail switch
- Dark and light, system fonts plus one grotesk for display; palette: near black, paper white, one signal green, one alert amber

## The site

Single page, static, fast. Sections: hero (install the PWA, no download), the ladder (how it works, honest about which rail needs what), recharge, trust (open source, no network calls, no accounts), limits and FAQ, GitHub. No blog, no newsletter popup, no fake testimonials.

## Build order

1. Brand + static site
2. PWA: rail ladder, QR autofill, carrier setting, recharge handoff, local History
3. Hindi and languages, elder mode, backup and restore
4. Installability, service worker cache, Pages deploy

## Naming note

"OpenPay" collides with a defunct Australian BNPL and a Mexican charge processor. For an Indian open source consumer payment tool the collision is low risk. Domain hunting after v1 works.
