package openpay.app.rails

/** What the phone has right now. */
enum class NetKind { ONLINE, SIGNAL, NONE }

data class RailContext(
    val net: NetKind,
    val carrierName: String?,
    val payeeHasVpa: Boolean,
    val payeeHasPhone: Boolean
)

sealed interface RailPick {
    data object Intent : RailPick
    data object Ussd : RailPick
    data object Ivr : RailPick
    data class None(val reason: Reason) : RailPick

    enum class Reason { NO_PAYEE, OFFLINE_NO_PHONE, NO_SIGNAL, NO_TELEPHONY }
}

/**
 * The heart of OpenPay: one decision, from full data to nothing.
 *
 * ONLINE        -> UPI intent handoff
 * SIGNAL, Jio   -> IVR (Jio's all IP network carries no USSD)
 * SIGNAL, other -> USSD when we have a VPA, IVR when we only have a phone
 * NONE          -> nothing works yet (OpenPay Notes arrive in the next release)
 */
object RailDecider {

    fun isJio(carrierName: String?): Boolean {
        if (carrierName.isNullOrBlank()) return false
        val c = carrierName.uppercase()
        return c.contains("JIO") || c.contains("RELIANCE")
    }

    fun pick(ctx: RailContext): RailPick = when (ctx.net) {
        NetKind.ONLINE -> RailPick.Intent
        NetKind.SIGNAL -> when {
            !ctx.payeeHasVpa && !ctx.payeeHasPhone -> RailPick.None(RailPick.Reason.NO_PAYEE)
            isJio(ctx.carrierName) ->
                if (ctx.payeeHasPhone) RailPick.Ivr
                else RailPick.None(RailPick.Reason.OFFLINE_NO_PHONE)
            ctx.payeeHasVpa -> RailPick.Ussd
            ctx.payeeHasPhone -> RailPick.Ivr
            else -> RailPick.None(RailPick.Reason.NO_PAYEE)
        }
        NetKind.NONE -> RailPick.None(RailPick.Reason.NO_SIGNAL)
    }
}
