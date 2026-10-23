package openpay.app.rails

import android.net.Uri

/**
 * Rail 3: UPI 123Pay over the IVR phone line. This is the Jio path (no USSD
 * on an all IP network) and the phone-number-only path. The call string keys
 * the menu with DTMF tones, each comma a 2 second pause.
 */
object IvrRail {

    const val NUMBER = "08045163666"
    const val MAX_RUPEES: Long = 10_000L

    /**
     * DTMF suffix after the number: menu 1 (send money), recipient phone,
     * amount, confirm 1. Returns null when the input cannot be keyed in.
     */
    fun dtmfSuffix(phone: String, amountRupees: Long): String? {
        val digits = phone.filter { it.isDigit() }
        val p = if (digits.length == 12 && digits.startsWith("91")) digits.drop(2) else digits
        if (p.length != 10) return null
        if (!p.startsWith("6") && !p.startsWith("7") && !p.startsWith("8") && !p.startsWith("9")) return null
        if (amountRupees <= 0 || amountRupees > MAX_RUPEES) return null
        return "1,,$p,,$amountRupees,,1"
    }

    fun dialUri(phone: String, amountRupees: Long): Uri? =
        dtmfSuffix(phone, amountRupees)?.let { suffix ->
            Uri.parse("tel:$NUMBER,,$suffix")
        }

    /** Guide templates with {placeholders}, localized by the screen. */
    fun guideSteps(): List<String> = listOf(
        "The call starts itself and keys in the menu, recipient and Rs {amt}",
        "Listen for the UPI PIN prompt and dial your PIN on the call keypad",
        "Hang on the line until the IVR reads out the reference number",
        "Your bank's SMS will close this payment here automatically"
    )
}
