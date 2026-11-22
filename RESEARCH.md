# OpenPay Research: Offline UPI Payments in India

Compiled 2026-10-23. Sources inline. Covers the two reference repos, the official UPI framework, community sentiment, and the competitive landscape. Recharge addendum checked 2026-11-22.

## TL;DR

1. Both reference repos are USSD/IVR wrappers: real payer-side apps, no payee side, no offline value transfer. Flowpay is alive (Oct 2026), OffPayApp is dead (June 2026). Neither has shipped an APK.
2. The official rails for no-data payments exist and work (USSD *99#, UPI 123Pay IVR, UPI Lite, Lite X NFC) but adoption is near zero because the UX is clunky and awareness is low.
3. The top user pain is not "no internet" in isolation; it is pending and double debited payments with no recourse visibility, plus merchant side trust (fake screenshots, dead soundboxes).
4. Nobody ships a clean consumer PWA that just orchestrates the existing rails, composes the recharge handoff, and is honest about what is pending. That is the opening.
5. An indie team cannot become a UPI app (TPAP requires PSP bank sponsorship, 5 to 9 months, NPCI certification). The pragmatic route is an installable PWA that builds UPI intents and dial or SMS handoffs, with no server and no money held.

---

## 1. Reference repo A: Flowpayup/Payments-Without-Internet

172 stars, 116 forks, Kotlin 2.1 / Jetpack Compose / M3, minSdk 29, Apache-2.0. Created 2026-06-30, last push 2026-10-14. Single anonymous maintainer (org "Flowpayup", commits authored "Flowpay"). Source only, no released APK (maintainer cites "installable attack surface from public CI").

Mechanism ([ARCHITECTURE.md](https://github.com/Flowpayup/Payments-Without-Internet/blob/main/docs/ARCHITECTURE.md)):
- **USSD rail (QR path)**: dials `*99*1*3#` via `ACTION_CALL`. The user completes the interactive carrier USSD menu by hand; the app cannot inject VPA or amount into the session. No accessibility service is used.
- **123Pay IVR rail (manual path)**: `Upi123CallStringBuilder.kt` builds `tel:08045163666,,1,<phone>,,<amount>,,1`, a DTMF payload with fixed 2s pauses. Works on Jio where USSD is dead (VoLTE carries no USSD). Whole rupees only, ₹4,999 cap. UPI PIN dialed manually by the user.
- **Confirmation**: the rails give no callback to a third party app. A `RECEIVE_SMS` receiver plus an "operation window" plus a permissive bank SMS parser (14 banks) is the only success signal. 10 minute deadline, then the pending row is deleted. "No confirmation, no record."
- **Security posture**: no INTERNET permission, no analytics, no server. SQLCipher ledger keyed via Android Keystore.

Weaknesses: QR and USSD are disconnected (user re-enters everything after scanning); SMS confirmation is spoofable (no sender authentication); DTMF blind menu walking desyncs with variable IVR prompts; open bugs (SQLCipher migration wipes history #28, junk bank refs #23-25, landline passes validation #35); English only; no tests against a real bank; no merchant or payee side.

Forks: of 116, only ~10 diverge, all trivially (APK build CI workflows or doc typos). No feature forks. Example: [Raju-yeager/Flowpay-apk-compiler](https://github.com/Raju-yeager/Flowpay-apk-compiler).

## 2. Reference repo B: laksh-ya/OffPayApp

170 stars, 16 forks, Kotlin / Compose, minSdk 26, MIT. Created 2026-05-26 by students (laksh-ya, harshtripathi272), last push 2026-06-16. Dead after a 3 week sprint. Beta APK released 2026-06-11, site on Vercel. Predates Flowpay by a month; independent codebase, same problem.

Mechanism: USSD `*99*1*3#` only, automated by an `AccessibilityService` that watches carrier USSD dialogs, classifies frames with a regex state machine (`ActionRunner`), types replies, and hides the raw dialog behind an overlay. Manual mode prefills the dialer; a PWA covers iOS. SQLCipher history, PIN held in memory and wiped within 500ms, zero outbound network.

Weaknesses: **Jio unsupported** (largest carrier, no USSD over VoLTE); regex automation is fragile against per bank and per carrier menu drift; sideloaded app demanding accessibility + overlay + programmatic PIN entry is a trust nightmare on Android 13+; ₹5,000 rail cap; dead project; promised languages and per-bank handling never landed.

Forks: 16, all trivial. devendermahto is 43 commits ahead but all "chore: sync editor configuration".

## 3. The wider repo ecosystem

GitHub search finds a whole long tail of USSD wrapper attempts, none above ~11 stars: [NoInUPI](https://github.com/jagarlamudijanakirammaneesh/NoInUPI) (11★, active Oct 2026), [TurantPay](https://github.com/rajureddi/TurantPay), [wazpay](https://github.com/f4w4z/wazpay), [AirPay](https://github.com/bindpratapsingh/AirPay) (USSD + IVR), [Offline-UPI](https://github.com/tomxposed/Offline-UPI) (QR scan to USSD call), [zeppay](https://github.com/mananbharti/zeppay) (Flutter, *99#/123Pay), [NexPay](https://github.com/Ordinarybits/NexPay) (USSD + 123Pay + bank SMS), plus several Bluetooth mesh simulation backends (Spring Boot demos at 1★). Adjacent and useful: [UPIPaymentAlert](https://github.com/official-Arvind/UPIPaymentAlert) (5★, offline voice announcements of UPI SMS), [xxwarwolfxx/UPI-Expense-Tracker](https://github.com/xxwarwolfxx/UPI-Expense-Tracker) (9★, offline expense capture). Global OSS energy for true offline value transfer is in Bitcoin land: Meshtastic LoRa + Cashu ecash ([blackbox_node](https://github.com/wadadawadada/blackbox_node), 173★; [btcmesh](https://github.com/eddieoz/btcmesh)) and event NFC wristband systems ([stustapay](https://github.com/stustapay/stustapay), 159★, active Oct 2026).

Read: the USSD wrapper idea keeps being reinvented every few months and dying. The differentiated version has not been built.

## 4. Official UPI framework (Oct 2026)

**Architecture.** NPCI runs the central switch; banks connect as PSP members; TPAPs (PhonePe ~47%, Google Pay ~33-37%, Paytm) are frontends riding PSP banks. Users are addressed by VPA (`name@psp`), 2FA is MPIN. Android integration is intent based: `upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>&tr=<ref>` per the NPCI deep linking spec; UPI QR encodes exactly this URI. States are success / failure / **pending** (debit posted, beneficiary CBS timed out); auto reversal by T+1; merchant refunds must land T+5 working days or the bank pays ₹100/day penalty.

**Access rules for builders.** TPAP status needs a PSP bank sponsor and NPCI certification (5-9 months); effectively closed to small teams. The 30% volume cap on TPAPs was deferred again to Dec 31, 2026. Indie route is merchant side or orchestration: intent flows, static QR (free, no integration), dynamic QR + gateway webhooks. New cost: **MDR 0.4% on P2M transactions above ₹2,000 (cap ₹300)** effective Oct 15, 2026, consumer side still free ([DFS FAQ](https://financialservices.gov.in/sites/default/files/2026-09/FAQs---Merchant-Discount-Rate--MDR--on-Select-UPI--P2M--Transactions_0.pdf)).

**Offline rails.**

| Rail | How | Limits | Status Oct 2026 |
|---|---|---|---|
| USSD *99# | SIM signaling channel, menu driven | ₹5,000/txn (raised from ₹10,000 proposal line; see [RBI](https://www.rbi.org.in/Scripts/BS_PressReleaseDisplay.aspx?prid=53385)) | Alive, flat/declining use, dead on Jio VoLTE |
| UPI 123Pay | 4 channels: IVR 080 4516 3666, app on feature phone, sound based (ToneTag), missed call pay | ₹10,000/txn, raised by RBI in Oct 2024 | Alive technically, commercially dead; PhonePe launched it only Sep 2026 to "revive" it |
| UPI Lite | On device wallet | ₹1,000/txn no PIN, ₹5,000 wallet | ~50M users, ~5-10% of volume, buried in bank app UIs |
| UPI Lite X | NFC debit from Lite wallet, no internet, no PIN | small per txn (~₹500) | Shipped 2023, near zero adoption |
| UPI Tap & Pay | Phone taps PoS over NFC, unlock only | no PIN to ₹2,000-5,000 | Launched Sep 2026 at GFF; PoS still needs internet |
| e₹ offline | CBDC NFC offline | ₹10,000/txn, ₹1 lakh wallet | Pilot; circulation fell ₹10.16B to ₹7.71B YoY |
| UPI One World | PPI for foreign tourists | no Indian account needed | Live, low awareness |

**Scale.** Sep 2026: 24.07B transactions, ₹29.37 lakh crore, 756 banks, ~55.5 crore users, 6.5 crore merchants, ~84% of digital payments.

## 5. Community signal

**Pains.** Villages and terrain shadows where UPI dies ([r/IndiaTech](https://www.reddit.com/r/IndiaTech/comments/1qapptm/internet_at_my_village_in_sindhudurg_dist_is)); mall basements; hill stations where ATMs are dry too; dense crowds (concerts, Kumbh 2025 where merchants pulled down digital boards); Mumbai Metro 3 with no network at all. Top recurring complaint overall: debited but failed payments, retries causing double debits, T+1 to T+3 waits, unresponsive banks ([r/UPI](https://www.reddit.com/r/UPI/comments/1wyj7t0/mobikwik_upi_payment_pending_money_debited_did), [r/LegalAdviceIndia](https://www.reddit.com/r/LegalAdviceIndia/comments/1sgeqm4/35000_debited_but_not_received_bank_not_helping)). Merchant side: soundbox lock-in and failures; merchants depend on voice because they cannot watch a screen.

**Sentiment on existing offline options.** "Technically alive, commercially dead." UPI 123Pay serves a rounding error of volume; NDTV lists offline payments among "12 UPI features most users still don't use". UPI Lite has real users but poor visibility.

**Wishes.** Hide full name/phone from payees; native split and receipts/export (people build offline SMS expense trackers because UPI lacks them); offline resilience itself. Elderly users are "afraid to open payment apps".

**Fraud and trust.** Fake payment screenshots (now AI generated) and QR overlays are the merchant killer; NPCI killed the "request money" feature entirely on Oct 1, 2025 over collect scams. RBI Ombudsman FY25: 13.34 lakh complaints, digital payments the largest category at 42%.

**Notable events.** Apr 12, 2025: longest UPI outage ever (Check Transaction API flooding). Oct 2-3, 2026: SBI/intermittent outage, 2,750+ Downdetector reports. Total UPI downtime 2022-2024 was only 282 minutes; the long tail is bank side and last mile.

## 6. Competitive landscape

- **ToneTag**: encrypted ultrasonic + NFC proximity payments; powers 123Pay's sound channel; ~50M users, 380k merchants, $78M raised Feb 2026. Business to business distribution; no consumer mindshare.
- **Soundboxes** (Paytm/PhonePe/BharatPe): voice confirmation of online UPI; payer still needs data. Proof that merchants adopt single purpose, dead simple devices.
- **PayNearby/AePS**: Aadhaar biometric agent cash network; ₹10,000/txn; RBI tightened agent KYC Jan 2026.
- **Digital Rupee**: rail without a product; shrinking circulation.
- **Global analogues**: Square/Shopify POS offline mode (queued offline card payments with merchant caps and auto clearing on reconnect), London OMNY deferred risk based transit settlement, Lightning custodial vouchers and Cashu ecash for offline tokens, and M-Pesa for USSD native UX at national scale.

## 7. White space (what nobody ships)

1. **Graceful degradation.** UPI today is binary: works or errors. No app walks a fallback ladder (intent to USSD to IVR) and tells you what each rail needs.
2. **Pending payment clarity.** Every stuck transaction app shows a spinner; none explains T+1/T+5 rights or gives the user a plain local record of what they handed off.
3. **Recharge without data.** Recharge needs no VPA and no app on the operator side. The bank SMS and USSD or IVR paths exist but are buried in bank menus. Nothing composes the exact instruction for the user to send from their own handset.
4. **Trust posture as a feature.** Both repos proved the pitch dies without distribution and polish. A PWA removes the install barrier: no store, no APK, no sideloading. No server, no accounts and no network calls are checkable properties, not marketing.
5. **Local first history.** No cloud profile, no analytics, all state in the browser, an export the user controls.

The conclusion: OpenPay should not be "another USSD wrapper". It should be the full rail ladder and the recharge handoff in an installable PWA, honest about confirmation and simple enough for a shopkeeper and a parent.

## 8. Recharge without data (checked 2026-11-22)

Recharge is not a UPI payment. The bank SMS paths need only signal. SBI Quick registers with `REG <account>` to 07208933148, then `MOBRC <AIR|BSN|MTD|MTM|JIO|VI> <amount>` to 917208933145, then `MOBACC <OTP>` within 10 minutes, capped at Rs 700 a day and Rs 1000 a month, same registered mobile only ([SBI](https://sbi.bank.in/web/personal-banking/information-services/kyc-guidelines/sbi-quick-missed-call-banking)). Axis sends `MOBILE <number> <operator> <amount> <last6>` to 9951860002 or 56161600, no internet or GPRS, any target prepaid number ([Axis](https://www.axis.bank.in/bank-smart/sms-banking/recharge-through-sms)).

The other live paths: UPI 123Pay IVR 08045163666 has a Mobile Recharge menu through a biller, bank -> UPI -> operator, no wallet, and the menu varies by bank. Bharat Connect postpaid covers all major operators, but prepaid data packs only BSNL and MTNL. Operator self service needs no bank money: Vi `*199#`, Airtel `*121#` or `*567*3#`, BSNL `*123#` with STV and RC, MTNL `*444#`, Jio app or IVR only. Two old paths are dead: the Vi plus Paytm `@paytm` VPA, with Paytm retiring handles on 1 Aug 2024, and ICICI SMS recharge, stopped 9 Mar 2023. Full table, sources, liveness and verification gaps in [docs/RECHARGE.md](docs/RECHARGE.md).

The architecture stays local. What a conventional app would call a backend is here the rules layer, pure on-device code that picks the path and formats the handoff. A server would break the no accounts and no network calls promise, and a collection and settlement flow would make OpenPay an unlicensed payment aggregator. OpenPay composes the instruction, the user sends it, and the money moves bank or prepaid balance to operator directly.
