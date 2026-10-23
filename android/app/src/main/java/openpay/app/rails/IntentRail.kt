package openpay.app.rails

import android.content.Intent
import android.net.Uri

/**
 * Rail 1: hand the payment to any installed UPI app. OpenPay never does
 * networking itself, the chosen app talks to the bank.
 */
object IntentRail {

    fun uri(vpa: String, name: String, amountRupees: String, note: String): Uri =
        Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("pn", name.ifBlank { "Payee" })
            .apply {
                if (amountRupees.isNotBlank()) appendQueryParameter("am", amountRupees)
                if (note.isNotBlank()) appendQueryParameter("tn", note)
                appendQueryParameter("cu", "INR")
            }
            .build()

    fun launchIntent(vpa: String, name: String, amountRupees: String, note: String): Intent =
        Intent(Intent.ACTION_VIEW, uri(vpa, name, amountRupees, note))

    /** Normalize the UPI deep link response to SUCCESS / FAILURE / SUBMITTED / null. */
    fun normalizeResponse(raw: String?): String? = when (raw?.trim()?.uppercase()) {
        "SUCCESS" -> "SUCCESS"
        "FAILURE", "FAILED" -> "FAILURE"
        "SUBMITTED" -> "SUBMITTED"
        else -> null
    }
}
