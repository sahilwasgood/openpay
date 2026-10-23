package openpay.app.rails

/**
 * Confirmation truth. The rails give OpenPay no callback, so the only honest
 * success signal is the user's own bank SMS: matched by sender, by debit
 * wording, and by the exact amount, inside a tight time window.
 */
object SmsParser {

    private val AMOUNT = Regex("""(?:rs\.?|inr)\.?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val REF = Regex(
        """(?i:ref|utr|txn|transaction)\s*(?i:no\.?|number|id)?[\s:#.\-]{0,4}([A-Z0-9]{6,22})"""
    )

    /** True when the sender short code belongs to the user's bank. */
    fun senderMatchesBank(sender: String?, bankSenders: List<String>): Boolean {
        if (sender.isNullOrBlank()) return false
        if (bankSenders.isEmpty()) return false
        val s = sender.uppercase()
        return bankSenders.any { s.contains(it.uppercase()) }
    }

    /**
     * True when the body reads as a debit of exactly this amount. "Other bank"
     * users get sender matching disabled, so they pay with a stricter amount
     * plus debit word match instead.
     */
    fun isDebitConfirmation(body: String, expectedPaise: Long): Boolean {
        val lower = body.lowercase()
        val debitWord = listOf("debited", "debit", "paid using upi", "sent to")
            .any { lower.contains(it) }
        if (!debitWord) return false
        val match = AMOUNT.find(body) ?: return false
        val rupeesText = match.groupValues[1].replace(",", "")
        val rupees = rupeesText.toDoubleOrNull() ?: return false
        return Math.round(rupees * 100).toLong() == expectedPaise
    }

    fun extractRef(body: String): String? =
        REF.find(body)?.groupValues?.get(1)?.uppercase()
}
