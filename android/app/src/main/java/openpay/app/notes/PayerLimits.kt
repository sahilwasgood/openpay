package openpay.app.notes

/**
 * Risk limits for Notes. OpenPay cannot see anyone's balance offline, so the
 * defence is bounded exposure: a first note from an unknown key is capped
 * small, pinned payers earn room.
 */
object PayerLimits {

    /** Max for a note from a payer key that was never pinned. Rs 500. */
    const val FIRST_NOTE_MAX_PAISE: Long = 50_000L

    fun needsConsent(amountPaise: Long, knownPayer: Boolean): Boolean =
        !knownPayer && amountPaise > FIRST_NOTE_MAX_PAISE
}
