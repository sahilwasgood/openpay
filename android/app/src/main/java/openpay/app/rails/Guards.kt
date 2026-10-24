package openpay.app.rails

import openpay.app.data.PayState
import openpay.app.data.Payment

/**
 * The double debit guard. The single most repeated trauma on Indian payment
 * forums: a payment looks stuck, the user retries, the first one actually
 * went through, both debit. OpenPay warns before a same-looking retry.
 */
object Guards {

    fun findDuplicate(
        payments: List<Payment>,
        amountPaise: Long,
        vpa: String?,
        phone: String?,
        nowEpoch: Long,
        windowMs: Long
    ): Payment? = payments
        .filter {
            it.state == PayState.PENDING &&
                it.amountPaise == amountPaise &&
                nowEpoch - it.createdAtEpoch < windowMs &&
                ((vpa != null && it.payee.vpa == vpa) || (phone != null && it.payee.phone == phone))
        }
        .maxByOrNull { it.createdAtEpoch }
}
