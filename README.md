# OpenPay

UPI payments that work with zero signal.

OpenPay is one installable PWA for India. Install it from the browser, then it works offline. Set your carrier once, and the app hands each payment to the strongest rail that still works.

| Rail | Needs | Limit | Notes |
|---|---|---|---|
| UPI Intent | Data | Your bank's limit | Hands off to any installed UPI app |
| USSD *99# | Signal | Rs 5,000 | Airtel, Vi, BSNL. Jio carries no USSD |
| IVR · UPI 123Pay | Signal | Rs 10,000 | The Jio path, keyed in with dial tones |

UPI Intent is a `upi://pay` deep link into any installed UPI app. USSD dials `*99*1*3#` and you finish the carrier menu. IVR keys the 123Pay line (080 4516 3666) with the dialer. The app composes the instruction and hands it over. Your bank app, the carrier menu or the IVR does the rest.

## Recharge, composed in the browser

A recharge is not a payment. With no data, OpenPay builds the exact bank SMS or opens the dialer for a USSD or IVR path, and you send or confirm it on the handset. The bank or operator moves the money from your account or prepaid balance to the operator. OpenPay never holds it. Codes, operators, limits and sources are in [docs/RECHARGE.md](docs/RECHARGE.md). A browser cannot send SMS or run a USSD session, so the app hands off with `sms:` and `tel:` links and you finish on the phone.

## Offline, local, no server

- The app makes no network calls. It builds `upi://`, `tel:` and `sms:` links. No fetch, no API, no telemetry.
- After install it works offline. The service worker caches the app shell and all state lives in your browser.
- No server, no accounts, no analytics, no tracking.
- No money is held. Every path moves money directly from your bank account or prepaid balance to the payee or operator.

There is no backend server. The rules that pick a path and build the handoff are pure code in the app, what a conventional app would put in a backend. A server would break the promise above, and a collection and settlement flow would make OpenPay an unlicensed payment aggregator. OpenPay only composes the instruction you send.

## Fees

Person to person UPI is free, and every OpenPay payment is person to person: zero MDR, any amount. The 0.4% MDR from Oct 15, 2026 applies to large merchants on merchant transactions above Rs 2,000, on their side. Splitting one purchase to dodge it is prohibited by NPCI and saves nobody anything. The [site](https://sahilwasgood.github.io/openpay/#mdr) has the calculator.

## Install

Open [sahilwasgood.github.io/openpay/app.html](https://sahilwasgood.github.io/openpay/app.html) in a browser and use the browser's install option (Add to Home screen or Install app). Launch it like any other app. There is no store listing and no file to sideload. After the first load it works offline.

Pay by scanning a UPI QR, typing a VPA or phone number, or picking a contact where the browser allows it. The Pay tab picks the rail, Recharge builds the bank SMS or dial handoff, History keeps a local record you mark paid or pending yourself (browsers cannot read bank SMS), and Settings holds English and Hindi, elder mode and backup and restore. All state is in your browser.

## Fork it

Plain HTML, CSS and JavaScript, one service worker, no framework and no build step. Pages deploys the site on every change under `site/`. [CONTRIBUTING.md](CONTRIBUTING.md) is the whole map, `docs/ARCHITECTURE.md` is the system, `CLAUDE.md` makes any coding agent productive in this repo in minutes. Fork it and ship your first change in an evening.

## Repo layout

- `site/` the PWA and the site
- `docs/ARCHITECTURE.md` the system map
- `docs/RECHARGE.md` every recharge path, code and limit
- `RESEARCH.md` the UPI research this is built on
- `PRODUCT.md` the product spec
- `AUTONOMOUS/PROJECT_STATUS.md` task board and decisions

## License

Apache 2.0. *99# and UPI 123Pay are NPCI services. OpenPay is not a bank and not a PSP. Payments settle on UPI rails through your bank.
