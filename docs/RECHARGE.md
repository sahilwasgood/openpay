# Recharge handoff

OpenPay does not perform a recharge. The rules layer in the PWA builds the exact bank SMS, or opens the dialer for a USSD or IVR path, and the user sends or confirms it on the handset. The bank or operator moves the money from the payer's account or prepaid balance straight to the operator. OpenPay never receives, holds or routes it. Facts below were checked against official bank and operator pages on 2026-11-22.

This page is the reference for codes, operators, limits and sources. The table records live and dead paths, plus paths the handoff may use. It is not a claim that every row is wired into a screen.

## Every path, one table

| Path | Code | Operators | Payer source | Limits | Source | Status |
|---|---|---|---|---|---|---|
| SBI Quick SMS | `REG <account>` to 07208933148; `MOBRC <AIR\|BSN\|MTD\|MTM\|JIO\|VI> <amount>` to 917208933145; `MOBACC <OTP>` to 917208933145 within 10 min | AIR Airtel, BSN BSNL, MTD MTNL Delhi, MTM MTNL Mumbai, JIO Jio, VI Vi | SBI account, same registered mobile only | Rs 700/day, Rs 1000/month | [SBI][sbi] | Verified 2026-11-22, live |
| Axis SMS | `MOBILE <number> <operator> <amount> <last6>` to 9951860002 or 56161600 | Any operator named in the SMS | Axis account, last 6 digits; any target prepaid number | Not stated on the official page | [Axis][axis] | Verified 2026-11-22, live, no internet or GPRS needed |
| UPI 123Pay IVR | Dial 08045163666, say the category and biller, enter mobile number or customer ID, verify name and amount, enter UPI PIN, get confirmation on the call and by SMS | Biller named in the IVR prompt, no primary page lists a fixed Mobile Recharge menu | Bank account via UPI, no wallet | UPI 123Pay rail cap, Rs 10,000 per payment (see RESEARCH.md) | [NPCI][npci], [Digital India][digitalindia], [Bharat Connect UPI 123Pay][bc123] | IVR number from NPCI, payment flow from Bharat Connect, no primary page spells out a Mobile Recharge menu or bank -> UPI -> operator settlement, live |
| Bharat Connect | In a UPI app biller or Bharat Connect, no USSD code | Categories live for postpaid mobile bills and prepaid mobile recharges, operator list not confirmed from a primary page | Bank account via UPI or BBPS, no wallet | Not stated on the page checked | [Bharat Connect][bharat], [categories][bccat], [press room][bharatpress] | BSNL named as first telecom in prepaid recharges on 20 Sep 2021. Claims that postpaid covers all major operators and that prepaid data packs are BSNL and MTNL only are unverified, live category |
| Operator self service | Vi `*199#`; Airtel `*121#` or `*567*3#`; BSNL `*123#` menu based self care with balance, scratch card recharge and STV enquiry subcodes; MTNL `*444#`; Jio MyJio app or missed call 1299 for an SMS with number and plan details | Vi, Airtel, BSNL, MTNL and Jio. No USSD code found for Jio on official pages, unverified either way | Prepaid balance, no bank money | Operator balance rules, no bank cap | [Vi][vi], [Airtel][airtel], [Airtel data loan][airteldata], [MTNL][mtnl], [BSNL Calcutta][bsnlcal], [Jio MyJio][jiomy], [Jio 1299][jio1299] | Vi, Airtel, MTNL and BSNL codes confirmed on operator pages. Jio app and missed call confirmed on jio.com. Jio no USSD claim unverified, live |
| Vi plus Paytm `@paytm` VPA | `@paytm` VPA | Vi | Was a Paytm wallet or `@paytm` VPA | Not applicable | [Paytm][paytm], [RBI][rbi] | Dead. Paytm moved users from @paytm to @pthdfc, @ptaxis, @ptsbi or @ptyes from 1 Aug 2024, per Paytm notice published 31 Jul 2024 |
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
[bc123]: https://www.bharat-connect.com/solutions/upi-123pay/
[bccat]: https://www.bharat-connect.com/categories/
[bharatpress]: https://www.bharat-connect.com/media-room/press-releases/
[cub]: https://cityunionbank.bank.in/upi-123pay
[bob]: https://bankofbaroda.bank.in/digital-products/instant-banking/bob-e-pay-123pay
[bsnlcal]: https://calcutta.bsnl.co.in/Mobile_Prepaid_STV.shtml
[jiomy]: https://www.jio.com/apps/myjio/
[jio1299]: https://www.jio.com/help/faq/mobile/services/hd-voice/how-to-know-my-jio-number/
[paytm]: https://paytm.com/blog/payments/upi/enhancing-your-upi-payment-experience-with-new-upi-ids/

## What OpenPay can and cannot do

Can:

- Build the SMS body and destination number for a bank path, or open a `tel:` link for a USSD or IVR path
- Show the exact steps for a path, including SBI's two step OTP flow and the daily and monthly caps
- Run all of this with no network call from OpenPay, in the rules layer in `site/webapp.js`

Cannot:

- Send an SMS or execute a USSD session from a website. A browser can only open `sms:` and `tel:` links. The user sends the message or completes the menu on the handset, and OpenPay never touches the network
- Read bank SMS. Browsers cannot, so the PWA leaves confirmation to the user in History
- Debit an account, hold a balance, top up a wallet, or act as a bank, a PSP or a payment aggregator
- Make a bank send from a number it does not allow. SBI Quick only recharges the same registered mobile, whatever a handoff composes

Every path ends with a human action on the handset. That is the boundary.

## Why no backend server, why we are not a payment aggregator

There is no server backend. This is a decision, not a missing feature.

- A server would break the privacy promise. The app makes no network calls, and there are no accounts, no analytics and no tracking. A backend would make those claims false.
- A collection and settlement flow would make OpenPay an unlicensed payment aggregator. If OpenPay took money from a payer and settled with the operator, it would be collecting and settling payments. It does neither. Every path in the table moves money directly from the payer's bank account or prepaid balance to the operator, with the user sending the instruction from their own handset.

What a conventional app would call a backend is, here, the rules layer: pure, on-device code in `site/webapp.js` that picks a path, validates amounts and formats the handoff. There is no money logic in event handlers and no network code in the app.

## Verification gaps

Checked against primary pages on 2026-11-23. The BSNL code and the Paytm date are now confirmed. A separate Mobile Recharge menu, the bank -> UPI -> operator chain, menu wording by bank, the Bharat Connect operator list and the Jio no USSD claim remain unverified.

- UPI 123Pay menu and settlement. The [Bharat Connect UPI 123Pay page][bc123] (undated) gives the voice payment flow: call the UPI 123Pay number, say the category and biller, enter your mobile number or customer ID, verify your name and the amount, enter your UPI PIN and get confirmation on the call and by SMS. The IVR number 08045163666 is from [NPCI's post][npci] and the [Digital India page][digitalindia] published 27 Feb 2024. No primary page spells out a separate Mobile Recharge menu, the chain bank -> UPI -> operator, or menu wording that varies by bank. Those three details are unverified. Bank pages do list their own IVR numbers, for example [City Union Bank][cub] 08045163581 and [Bank of Baroda][bob] 08045228222, but describe money transfer and balance enquiry rather than a recharge menu.
- Bharat Connect operator coverage. The [Bharat Connect categories page][bccat] confirms live categories for postpaid mobile bills and prepaid mobile recharges. The [press room][bharatpress] records BSNL as the first telecom operator in mobile prepaid recharges on 20 Sep 2021. The official live billers directory returned an error during this pass. Neither the claim that postpaid covers all major operators nor the claim that prepaid data packs are BSNL and MTNL only is confirmed by a primary page. Both are unverified.
- BSNL and Jio self service. The [BSNL Calcutta circle page][bsnlcal] (undated) lists the exact string "SELFCARE - New Menu based shortcode: *123#" with subcodes *123*1# for balance, *123*(ScratchCardPin)# for scratch card recharge, *123*3# for tariff and *123*5# for STV enquiry. The [BSNL Rajasthan page][bsnl] documents self care by SMS to 123. This closes the `*123#` gap. On Jio the [MyJio page][jiomy] says the app can be used with no data balance on a Jio data network and that Jio data is not charged for using it, and the [Jio 1299 FAQ][jio1299] confirms a missed call to 1299 returns an SMS with the Jio number and plan details. Jio therefore has no-data self service beyond the app. No official Jio page states that Jio has no USSD self service, so that claim is unverified.
- Paytm `@paytm` handle retirement. The [Paytm notice][paytm] published 31 Jul 2024 says the UPI ID changes from @paytm to @pthdfc, @ptaxis, @ptsbi or @ptyes and that from 1 Aug 2024 these new UPI IDs are used to send and receive payments on the Paytm app. This confirms the 1 Aug 2024 date from a primary Paytm source. The [RBI press release][rbi] separately gives 15 Mar 2024 for the Payments Bank restrictions.
