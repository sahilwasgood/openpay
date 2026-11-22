package openpay.app.rails

/**
 * Recharge is a handoff, not a payment OpenPay processes. A website or app
 * without INTERNET permission can only compose the bank SMS or dial the USSD
 * and IVR numbers. The user always confirms on the handset, and the operator
 * is the payee, so OpenPay never holds money and is not a payment aggregator.
 */
enum class RechargeOperator(val label: String, val sbiToken: String?) {
    JIO("Jio", "JIO"),
    AIRTEL("Airtel", "AIR"),
    VI("Vi", "VI"),
    BSNL("BSNL", "BSN"),
    MTNL_DELHI("MTNL Delhi", "MTD"),
    MTNL_MUMBAI("MTNL Mumbai", "MTM")
}

sealed interface Handoff {
    data class Sms(
        val target: String,
        val body: String,
        val step: Int,
        val steps: Int
    ) : Handoff

    data class Dial(val number: String) : Handoff
}

object RechargeHandoff {
    const val SBI_NUMBER = "917208933145"
    const val SBI_REG_NUMBER = "07208933148"
    const val SBI_MAX_RUPEES = 700
    const val AXIS_NUMBER = "9951860002"
    const val UPI_123PAY_NUMBER = "08045163666"

    /** SBI Quick, step one: MOBRC <operator> <amount>. Bank official, daily cap Rs 700. */
    fun sbiStep1(operator: RechargeOperator, amountRupees: Int): Handoff.Sms? {
        if (amountRupees <= 0 || amountRupees > SBI_MAX_RUPEES) return null
        val token = operator.sbiToken ?: return null
        return Handoff.Sms(SBI_NUMBER, "MOBRC $token $amountRupees", 1, 2)
    }

    /** SBI Quick, step two: MOBACC <OTP>, valid for ten minutes. */
    fun sbiStep2(otp: String): Handoff.Sms? {
        val code = otp.filter(Char::isDigit)
        if (code.length != 6) return null
        return Handoff.Sms(SBI_NUMBER, "MOBACC $code", 2, 2)
    }

    /** Axis SMS banking. Any number, no GPRS. Operator token is editable in the UI. */
    fun axis(phone: String, operator: String, amountRupees: Int, accountLast6: String): Handoff.Sms? {
        val digits = phone.filter(Char::isDigit)
        val p = if (digits.length == 12 && digits.startsWith("91")) digits.drop(2) else digits
        if (p.length != 10 || p.first() !in '6'..'9') return null
        if (amountRupees <= 0 || amountRupees > 10_000) return null
        val last6 = accountLast6.filter(Char::isDigit)
        if (last6.length != 6) return null
        val op = operator.trim().ifBlank { return null }
        return Handoff.Sms(AXIS_NUMBER, "MOBILE $p $op $amountRupees $last6", 1, 1)
    }

    /** UPI 123Pay IVR. Mobile Recharge is a menu; wording depends on the bank. */
    fun upi123Pay(): Handoff.Dial = Handoff.Dial(UPI_123PAY_NUMBER)

    /**
     * Operator self-service menus. These spend existing balance or a voucher,
     * never bank money. Jio has no USSD, so there is no self-service dial here.
     */
    fun selfService(operator: RechargeOperator): Handoff.Dial? = when (operator) {
        RechargeOperator.JIO -> null
        RechargeOperator.AIRTEL -> Handoff.Dial("*121#")
        RechargeOperator.VI -> Handoff.Dial("*199#")
        RechargeOperator.BSNL -> Handoff.Dial("*123#")
        RechargeOperator.MTNL_DELHI, RechargeOperator.MTNL_MUMBAI -> Handoff.Dial("*444#")
    }
}
