# Recharge handoff

OpenPay does not perform a recharge. The domain/rules layer builds the exact bank SMS, or opens the dialer for a USSD or IVR path, and the user sends or confirms it on the handset. The bank or operator moves the money from the payer's account or prepaid balance straight to the operator. OpenPay never receives, holds or routes it. Facts below were checked against official bank and operator pages on 2026-11-22.

This page is the reference for codes, operators, limits and sources. The table records live and dead paths, plus paths the handoff may use. It is not a claim that every row is wired into a screen.

## Every path, one table

| Path | Code | Operators | Payer source | Limits | Source | Status |
|---|---|---|---|---|---|---|
| SBI Quick SMS | `REG <account>` to 07208933148; `MOBRC <AIR\|BSN\|MTD\|MTM\|JIO\|VI> <amount>` to 917208933145; `MOBACC <OTP>` to 917208933145 within 10 min | AIR Airtel, BSN BSNL, MTD MTNL Delhi, MTM MTNL Mumbai, JIO Jio, VI Vi | SBI account, same registered mobile only | Rs 700/day, Rs 1000/month | [SBI][sbi] | Verified 2026-11-22, live |
| Axis SMS | `MOBILE <number> <operator> <amount> <last6>` to 9951860002 or 56161600 | Any operator named in the SMS | Axis account, last 6 digits; any target prepaid number | Not stated on the official page | [Axis][axis] | Verified 2026-11-22, live, no internet or GPRS needed |
| UPI 123Pay IVR | Dial 08045163666, Mobile Recharge menu via biller, bank -> UPI -> operator | Biller list varies by bank | Bank account via UPI, no wallet | UPI 123Pay rail cap, Rs 10,000 per payment (see RESEARCH.md) | [NPCI][npci], [Digital India][digitalindia] | IVR number confirmed by NPCI; menu detail not re-verified from a primary page in this pass, live |
| Bharat Connect | In a UPI app biller or Bharat Connect, no USSD code | Postpaid: all major operators. Prepaid data packs: BSNL and MTNL only | Bank account via UPI or BBPS, no wallet | Not stated on the page checked | [Bharat Connect][bharat] | Coverage not fully re-verified from a primary page in this pass, live category |
| Operator self service | Vi `*199#`; Airtel `*121#` or `*567*3#`; BSNL `*123#` with STV and RC; MTNL `*444#`; Jio app or IVR only | Vi, Airtel, BSNL, MTNL; Jio has no USSD self service | Prepaid balance, no bank money | Operator balance rules, no bank cap | [Vi][vi], [Airtel][airtel], [Airtel data loan][airteldata], [MTNL][mtnl], [BSNL][bsnl] | Vi, Airtel and MTNL codes verified on operator pages; BSNL `*123#` and the Jio app or IVR only claim not fully re-verified, live |
| Vi plus Paytm `@paytm` VPA | `@paytm` VPA | Vi | Was a Paytm wallet or `@paytm` VPA | Not applicable | [RBI][rbi] | Dead. Paytm retired handles 1 Aug 2024, exact date not re-verified from a primary page in this pass |
| ICICI SMS recharge | SMS transactional recharge, MTOPUP and related keywords | Not applicable | Was an ICICI account | Not applicable | [ICICI][icici] | Dead. Discontinued 9 Mar 2023, verified on the official ICICI page |

Do not invent a token or a menu number that is not in this table. The SBI tokens are the six above, exactly as SBI lists them. The bank SMS paths use the destination numbers above, nothing else.

[sbi]: https://sbi.bank.in/web/personal-banking/information-services/kyc-guidelines/sbi-quick-missed-call-banking
[axis]: https://www.axis.bank.in/bank-smart/sms-banking/recharge-through-sms
[npci]: https://x.com/NPCI_NPCI/status/1503699668341256195
[digitalindia]: https://www.digitalindia.gov.in/initiative/upi-123pay/
[bharat]: https://bharat-connect.com
[vi]: https://myvi.in/help-support/self-service
[airtel]: https://airtel.in/blog/prepaid/quick-read-on-what-is-a-ussd-code-list-of-ussd-codes
[airteldata]: https://airtel.in/mobile/terms-conditions/data-loan
[mtnl]: https://mtnldelhi.in/mobile/online-mobile-recharge.htm
[bsnl]: http://rajasthan.bsnl.co.in/mobile/shortcodes.php
[rbi]: https://rbidocs.rbi.org.in/rdocs/PressRelease/PDFs/PR1926ACTIONAGAINSTPAYTMPAYMENTSBANK08B8389D22B849E0A0E10AA2C0606E76.PDF
[icici]: https://icici.bank.in/personal-banking/ways-to-bank/mobile-banking/sms-banking

## What OpenPay can and cannot do

Can:

- Build the SMS body and destination number for a bank path, or open a `tel:` link for a USSD or IVR path
- Show the exact steps for a path, including SBI's two step OTP flow and the daily and monthly caps
- Run all of this with no network call from OpenPay, in the domain/rules layer

Cannot:

- Send an SMS or execute a USSD session from a website. A browser can only open `sms:` and `tel:` links. The user sends the message or completes the menu on the handset, and OpenPay never touches the network
- Read bank SMS in the browser. The Android app can confirm a payment from a bank SMS inside a window, because SMS is a platform API there. The web app cannot, so its confirmation is manual
- Debit an account, hold a balance, top up a wallet, or act as a bank, a PSP or a payment aggregator
- Make a bank send from a number it does not allow. SBI Quick only recharges the same registered mobile, whatever a handoff composes

Every path ends with a human action on the handset. That is the boundary.

## Why no backend server, why we are not a payment aggregator

There is no server backend. This is a decision, not a missing feature.

- A server would break the privacy promise. The manifest carries no INTERNET permission, and there are no accounts, no analytics and no tracking. A backend would make those claims false and would need the permission back.
- A collection and settlement flow would make OpenPay an unlicensed payment aggregator. If OpenPay took money from a payer and settled with the operator, it would be collecting and settling payments. It does neither. Every path in the table moves money directly from the payer's bank account or prepaid balance to the operator, with the user sending the instruction from their own handset.

What a conventional app would call a backend is, here, the domain/rules layer: pure, unit tested, on-device code that picks a path, validates amounts and formats the handoff. It stays under `rails/` and any recharge path code, with the same rule as payments, no money logic in composables and no network code in the app.

## Verification gaps

Flagged for a later pass. These are the facts in this handoff that I could not confirm end to end from a primary page while writing this file:

- The UPI 123Pay Mobile Recharge menu sequence, bank -> UPI -> operator, and the note that the menu varies by bank. NPCI's official post at [NPCI][npci] and the [Digital India][digitalindia] page confirm the IVR number 08045163666. The NPCI 123Pay FAQ URL returned 404 during this pass. No primary page used here spells out the recharge menu.
- Bharat Connect operator coverage. [bharat-connect.com][bharat] is the official site and its press room records BSNL as the first telecom in mobile prepaid recharges, but no primary page checked here confirms the full statement that postpaid covers all major operators and prepaid data packs only BSNL and MTNL.
- BSNL `*123#`, and Jio app or IVR only. BSNL regional pages such as [BSNL Rajasthan][bsnl] document self care on 123 and STV activation, but not the exact `*123#` USSD string. No primary Jio page was checked for the app or IVR only claim.
- The 1 Aug 2024 Paytm handle retirement date. The [RBI press release][rbi] confirms the @paytm handle migration but gives 15 Mar 2024 for the Payments Bank restrictions, not 1 Aug 2024. The date is kept as handed off and needs a primary NPCI circular to close.
