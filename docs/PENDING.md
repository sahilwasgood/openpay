# Pending UPI payments

OpenPay cannot read bank SMS in a browser, so every row starts as `pending` until the user marks it paid. The history row deliberately says not to pay again and explains the recovery timeline.

The two timelines are different cases:

- **Payee not credited:** the beneficiary bank auto-reverses the debit by **T+1** day. ([NPCI consolidated circular, 18 Sep 2026](https://www.npci.org.in/uploads/UPI_Consolidated_Circular_f9c023a0ef.pdf), [ET](https://m.economictimes.com/wealth/spend/upi-payment-failed-or-stuck-when-banks-must-refund-your-money-and-pay-100-per-day-in-compensation/articleshow/135116763.cms))
- **Merchant confirmation failure:** refunds are due by **T+5** days, with ₹100 per day compensation for delays. ([Business Standard](https://business-standard.com/finance/personal-finance/wrong-upi-payment-here-s-what-you-can-recover-and-what-you-can-t-126012900939_1.html), [Moneycontrol](https://moneycontrol.com/news/business/personal-finance/upi-payment-failed-but-money-was-debited-check-this-before-paying-again-14046572.html))

The NPCI PDF link was unreachable from this environment, so those two secondary sources back the merchant case. The app wording names both cases instead of claiming a single deadline for all failures.
