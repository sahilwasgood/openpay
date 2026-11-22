# OpenPay architecture

One sentence: an installable PWA that hands every payment to the strongest rail that still works, composes the recharge handoff in the browser, and keeps all state local.

## The three rails

The decision lives in `site/webapp.js` as pure functions that pick the rail and build the link. The DOM layer only renders.

1. **Intent** (online): builds a `upi://pay` deep link and the user opens any installed UPI app. OpenPay makes no network call.
2. **USSD** (signal, no data, not Jio): builds `tel:*99*1*3%23`. The user finishes the carrier menu. Works on Airtel, Vi, BSNL. Jio carries no USSD.
3. **IVR** (signal, Jio or phone only payee): builds the DTMF string for the UPI 123Pay line (080 4516 3666), comma pauses keyed in by the dialer. Cap Rs 10,000.

Connection state comes from the browser. Carrier is a user setting because browsers do not expose it. The pair selects the rail.

## Confirmation truth

The carrier rails give no callback to a web page. The only honest signal is the user's own bank, read by the user:

- A payment row is written to browser storage as pending before the handoff goes out.
- The user checks their bank and marks the row paid or pending in History.
- Browsers cannot read bank SMS. There is no SMS capture and no operation window in the PWA.

## The recharge handoff

Recharge is not one of the rails. It is a separate handoff: pure builders in `site/webapp.js` return an SMS body with its destination number, or a `tel:` dial string for USSD and IVR, and the DOM layer turns that into `sms:` and `tel:` links. The user sends it or completes the menu on the handset. The bank or operator moves the money. OpenPay never does. The path table, codes, operators, limits, sources and liveness are in [RECHARGE.md](RECHARGE.md).

No recharge path needs a network call from OpenPay.

## Decision: no server backend

This is deliberate, not a missing feature.

- A server would break the privacy promise. The app makes no network calls, there are no accounts, no analytics and no tracking. A backend would make those claims false.
- A collection and settlement flow would make OpenPay an unlicensed payment aggregator. If OpenPay took money from a payer and settled with the operator, it would be collecting and settling payments. It does neither. Every path in [RECHARGE.md](RECHARGE.md) moves money directly from the payer's bank account or prepaid balance to the operator, with the user sending the instruction from their own handset.

What a conventional app would call a backend is here the rules layer: pure, on-device code that picks a path, validates amounts and formats the handoff. It lives in `site/webapp.js`, with no money logic in event handlers and no network code in the app.

## The PWA

`site/` is deliberately framework free.

- `app.html` is the product: Pay, Recharge, History and Settings tabs.
- `webapp.js` holds state in `localStorage` under `op_` keys, the pure rail and recharge builders, QR autofill (BarcodeDetector where supported, paste elsewhere), contacts where the browser exposes them, and English and Hindi strings in the `HI` map with `data-i` hooks and English captured from the DOM at boot.
- `styles.css` is the single stylesheet. `app.js` drives the marketing site.
- `manifest.webmanifest` makes it installable. `start_url` is `app.html`, so installing the PWA installs the product. Shortcuts point into the app tabs.
- `sw.js` is the service worker: network first for same-origin GETs when online, cache when offline. The cache id is bumped every deploy so installed PWAs swap caches atomically after one online visit.

There is no build step. `.github/workflows/deploy-site.yml` deploys `site/` to GitHub Pages on every change under `site/`.

## Invariants worth defending

1. No network calls from the app code. It builds `upi://`, `tel:` and `sms:` links.
2. No money custody, no server, no accounts, no collection or settlement.
3. Path and money rules stay pure in `webapp.js`, not in event handlers.
4. All state stays in the browser, with a user controlled export.
