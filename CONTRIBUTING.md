# Forking OpenPay

OpenPay is built to be forked. The whole map, start to finish:

## What you are forking

- `android/` the app. Kotlin 2.0, Jetpack Compose, minSdk 29, no internet permission, everything local.
- `site/` the site and PWA. Plain HTML, one CSS file, one JS file, one service worker. No framework, no build step.
- `.github/workflows/` CI. Tests and a debug APK on every push, a signed release APK published on every `v*` tag, the site deploys to Pages on every change under `site/`.
- `docs/ARCHITECTURE.md` the system map. Read it first.
- `RESEARCH.md` and `PRODUCT.md` the reasoning behind every feature.

## Your first evening

1. Fork, clone, open the folder in your editor with its AI agent. The agent should read `CLAUDE.md` and `docs/ARCHITECTURE.md` first. That is the entire onboarding.
2. Build and test:
   ```
   cd android
   ./gradlew test assembleDebug
   ```
   JDK 17 or newer, Android SDK with platform 34.
3. Install `android/app/build/outputs/apk/debug/app-debug.apk` on a phone with an Indian SIM.
4. Change one small thing, push, watch CI go green. That is the whole loop.

## Signing your fork

CI signs release builds from repo secrets. Set four secrets in your fork (Settings, Secrets and variables, Actions):

- `OPENPAY_KEYSTORE_BASE64`: `base64 -w0 your-release.keystore`
- `OPENPAY_STORE_PASSWORD`, `OPENPAY_KEY_PASSWORD`: make them the same value, Android packaging rejects PKCS12 keystores with split passwords
- `OPENPAY_KEY_ALIAS`

Generate a keystore once:

```
keytool -genkeypair -keystore release.keystore -alias openpay -keyalg RSA -keysize 2048 -validity 10000
```

Keep the keystore and passwords out of git, forever. Lose them and you can never update your published app.

## Publishing your fork's site

Pages runs from the `site` workflow on every push that touches `site/`. In your fork, enable Pages (Settings, Pages, Source: GitHub Actions). Done.

## House rules that keep the project small

- No internet permission in the app. This is the product's core claim. Nothing that needs the network in the app itself is ever acceptable.
- Every money rule that can be pure Kotlin lives in `android/.../rails/` or `android/.../notes/` and gets unit tests. UI files call them, they do not re-implement them.
- English literals are the translation keys in `ui/Lang.kt`. Missing Hindi falls back to English, it can never break.
- Copy rules everywhere (app, site, docs): no em dashes, no filler, no claims the code does not make. Use the middle dot · as a separator.
- One build step only (`gradle`), one deploy path only (Actions). If a change adds a tool, it needs a better reason than taste.

## Where things are

See `docs/ARCHITECTURE.md` for the full map: the rail ladder, Notes, confirmation truth, the spending log, the guards, and every file that matters.
