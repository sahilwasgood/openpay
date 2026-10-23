package openpay.app.rails

import android.net.Uri

/**
 * Rail 2: NPCI's *99# over the SIM signaling channel. The carrier owns the
 * menu, so the user finishes it there. OpenPay cannot inject text into the
 * USSD session and never asks for an accessibility service.
 */
object UssdRail {

    const val USSD_SEND_MONEY_VPA = "*99*1*3#"
    const val USSD_BALANCE = "*99*3#"

    /** Dialable URI. # must be encoded, * survives as-is on every dialer tested. */
    fun dialUri(ussd: String = USSD_SEND_MONEY_VPA): Uri =
        Uri.parse("tel:" + ussd.replace("#", "%23"))

    /** What the carrier menu will ask for, in order, after *99*1*3#. */
    fun guideSteps(vpa: String?, amountRupees: String): List<String> = buildList {
        if (vpa != null) add("Type the UPI ID when asked: $vpa")
        add("Type the amount: Rs $amountRupees")
        add("Type a remark if the menu asks, or send 1 to skip")
        add("Type your UPI PIN. It stays on the carrier menu, OpenPay never sees it")
        add("Confirm. Your bank's SMS will close this payment here automatically")
    }
}
