# Forking OpenPay

OpenPay is built to be forked. The whole map, start to finish.

## What you are forking

- `site/` the PWA and the site. Plain HTML, one CSS file, one JS file, one service worker. No framework, no build step.
- `.github/workflows/deploy-site.yml` Pages deploy on every push that touches `site/`. No other CI.
- `docs/ARCHITECTURE.md` the system map. Read it first.
- `RESEARCH.md` and `PRODUCT.md` the reasoning behind every feature.

## Your first evening

1. Fork, clone, open the folder in your editor with its AI agent. The agent should read `CLAUDE.md` and `docs/ARCHITECTURE.md` first. That is the entire onboarding.
2. Preview locally:
   ```
   cd site
   python3 -m http.server 8000
   ```
   Open `http://localhost:8000/app.html`. There is no compiler and no package install.
3. Try Pay with a test VPA and a carrier setting, then Recharge with the SBI Quick path. Nothing is sent from the app, so no SIM or bank account is needed to see the handoffs.
4. Change one small thing, push, watch the Pages deploy go green. That is the whole loop.

## Publishing your fork's site

Pages runs from `deploy-site.yml` on every push that touches `site/`. In your fork, enable Pages (Settings, Pages, Source: GitHub Actions). Done. Two rapid pushes can cancel each other in the `pages` concurrency group; if the site did not update, re-run the deploy with one push.

## House rules that keep the project small

- The app makes no network calls. It builds `upi://`, `tel:` and `sms:` links. Nothing that needs a fetch, an API or a backend in the app is ever acceptable.
- Rail and recharge rules live as pure functions in `site/webapp.js`, under the path builders. Event handlers call them, they do not re-implement them.
- English literals are the translation keys, Hindi lives in the `HI` map, and `data-i` attributes are the hooks. Missing Hindi falls back to English, it can never break.
- Copy rules everywhere (app, site, docs): no em dashes, no en dashes, no filler, no claims the code does not make. Use the middle dot · as a separator.
- One deploy path only (Actions to Pages). If a change adds a tool or a build step, it needs a better reason than taste.
- All state stays in the browser. No cloud profiles, no analytics, no accounts.

## Where things are

See `docs/ARCHITECTURE.md` for the full map: the rail ladder, confirmation truth, the recharge handoff, the local record, and every file that matters.
