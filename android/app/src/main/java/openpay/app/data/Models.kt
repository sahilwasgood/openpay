package openpay.app.data

import kotlinx.serialization.Serializable

enum class Rail { INTENT, USSD, IVR }

enum class PayState { PENDING, SUCCESS, FAILED }

@Serializable
data class Payee(
    val displayName: String = "",
    val vpa: String? = null,
    val phone: String? = null
)

@Serializable
data class Payment(
    val id: String,
    val createdAtEpoch: Long,
    val payee: Payee,
    val amountPaise: Long,
    val note: String = "",
    val rail: Rail,
    val state: PayState = PayState.PENDING,
    val bankRef: String? = null,
    val smsConfirmed: Boolean = false,
    val intentResult: String? = null,
    val updatedAtEpoch: Long = createdAtEpoch,
    val settledAtEpoch: Long? = null
) {
    val amountRupeesString: String
        get() = RupeeFormat.format(amountPaise)
}

object RupeeFormat {
    fun format(paise: Long): String {
        val rupees = paise / 100
        val rest = paise % 100
        return if (rest == 0L) rupees.toString() else "$rupees.${rest.toString().padStart(2, '0')}"
    }
}
