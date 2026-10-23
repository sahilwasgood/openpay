package openpay.app.rails

import openpay.app.data.PayState
import openpay.app.data.Payment
import openpay.app.data.Rail

/**
 * Pure confirmation matching so the SMS window rules are unit testable:
 * newest pending non intent payment inside its window, sender must belong
 * to the user's bank, body must read as a debit of exactly that amount.
 */
object SmsConfirmations {

    fun findCandidate(payments: List<Payment>, nowEpoch: Long, windowMs: Long): Payment? =
        payments
            .filter {
                it.state == PayState.PENDING &&
                    it.rail != Rail.INTENT &&
                    nowEpoch - it.createdAtEpoch < windowMs
            }
            .maxByOrNull { it.createdAtEpoch }

    /** The confirmed payment, or null when the SMS does not close it. */
    fun confirm(
        candidate: Payment,
        sender: String?,
        bankSenders: List<String>,
        body: String,
        nowEpoch: Long
    ): Payment? {
        if (bankSenders.isNotEmpty() && !SmsParser.senderMatchesBank(sender, bankSenders)) return null
        if (!SmsParser.isDebitConfirmation(body, candidate.amountPaise)) return null
        val ref = SmsParser.extractRef(body) ?: candidate.bankRef
        return candidate.copy(
            state = PayState.SUCCESS,
            smsConfirmed = true,
            bankRef = ref,
            updatedAtEpoch = nowEpoch,
            settledAtEpoch = nowEpoch
        )
    }
}
