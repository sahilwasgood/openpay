# CLAUDE.md

Instructions for coding agents working in this repository. Human forkers: `CONTRIBUTING.md`. System map: `docs/ARCHITECTURE.md`.

OpenPay is a single installable PWA. There is no build step and no test runner in this repo.

## Preview and verify

```
cd site
python3 -m http.server 8000
```

Open `http://localhost:8000/app.html`. Verify a change by exercising Pay, Recharge, History and Settings in a browser, then check install and offline with the service worker. Use a desktop browser and a phone. There is no SDK, no JDK and no package install.

## Layout

- `site/index.html` the marketing site. `site/app.js` drives it.
- `site/app.html` plus `site/webapp.js` the product: Pay, Recharge, History and Settings. State lives in `localStorage` under `op_` keys.
- `site/webapp.js` also holds the pure rail and recharge builders. The DOM layer renders their output.
- `site/styles.css` the single stylesheet.
- `site/manifest.webmanifest` installability, `start_url` is `app.html`.
- `site/sw.js` the service worker, network first when online and cached when offline.
- `.github/workflows/deploy-site.yml` Pages deploy on site changes.
- `docs/ARCHITECTURE.md`, `docs/RECHARGE.md`, `RESEARCH.md`, `PRODUCT.md` the system and the reasoning.

## Rules

1. The app makes no network calls. It builds `upi://`, `tel:` and `sms:` links. No fetch, no API, no telemetry, ever.
2. Path and money rules stay as pure functions in `site/webapp.js`, under the rail and recharge builders. Event handlers call them, they do not re-implement them.
3. Strings: English literals are the keys, Hindi lives in the `HI` map, `data-i` attributes are the hooks. Add the Hindi entry when you add a string, missing entries fall back to English safely.
4. Copy style, everywhere including commit messages and docs: no em or en dashes, use the middle dot · or a comma. No filler phrases, no claims the code does not make.
5. No money custody, no server, no accounts, no analytics. Browser storage can be cleared by the browser, so keep the export path honest.
6. Service worker cache id in `site/sw.js` must be bumped on every site change so installed PWAs swap caches atomically after one online visit. Bump the `?v=` suffix on `styles.css` and on `app.js` or `webapp.js` in both `index.html` and `app.html` in the same change, HTTP caches can otherwise serve the old assets for a while.
7. The product is the rail ladder and the recharge handoff. Do not add a deferred payment or signature verification feature.

## Useful facts

- Jio phones have no USSD. The rail builder sends them to the 123Pay IVR string.
- The 123Pay IVR line is 080 4516 3666. The USSD dial string is `tel:*99*1*3%23`.
- SBI Quick caps at Rs 700 a day and Rs 1000 a month, and only works from the mobile registered with the account.
- Browsers cannot read bank SMS. Confirmation in History is manual, the user marks a row paid or pending.
