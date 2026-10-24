package openpay.app.rails

/**
 * Turns arbitrary bank debit SMS into private expense entries, entirely on
 * device. This is the offline expense tracker people keep building for
 * themselves because UPI apps do not ship one.
 */
object ExpenseParser {

    data class ParsedDebit(
        val amountPaise: Long,
        val payeeHint: String?,
        val ref: String?
    )

    private val AMOUNT = Regex("""(?:rs\.?|inr)\.?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val VPA = Regex("""([a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,})""")
    private val TO_HINT = Regex("""(?:to|towards|at)\s+([A-Za-z0-9 ._@&-]{3,32}?)(?:\s+on\s+|\s+ref\s+|\.|,|$)""", RegexOption.IGNORE_CASE)

    fun parseDebit(body: String): ParsedDebit? {
        val lower = body.lowercase()
        val hasDebit = listOf("debited", "debit", "paid using upi", "spent", "purchase").any { lower.contains(it) }
        if (!hasDebit) return null
        // credit only messages are not spending
        if (lower.contains("credited") && !lower.contains("debited")) return null
        val amountMatch = AMOUNT.find(body) ?: return null
        val rupees = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val paise = Math.round(rupees * 100)
        if (paise <= 0) return null
        val payee = VPA.find(body)?.groupValues?.get(1)
            ?: TO_HINT.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        return ParsedDebit(
            amountPaise = paise,
            payeeHint = payee,
            ref = SmsParser.extractRef(body)
        )
    }
}
