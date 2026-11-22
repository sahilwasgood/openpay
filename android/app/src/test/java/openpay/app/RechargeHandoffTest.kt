package openpay.app

import openpay.app.rails.Handoff
import openpay.app.rails.RechargeHandoff
import openpay.app.rails.RechargeOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RechargeHandoffTest {

    @Test
    fun sbiStep1UsesOperatorTokenAndBankShortCode() {
        val sms = RechargeHandoff.sbiStep1(RechargeOperator.JIO, 199)
        assertEquals(Handoff.Sms("917208933145", "MOBRC JIO 199", 1, 2), sms)
    }

    @Test
    fun sbiStep1CoversAllSixOperators() {
        assertEquals("MOBRC AIR 100", RechargeHandoff.sbiStep1(RechargeOperator.AIRTEL, 100)?.body)
        assertEquals("MOBRC BSN 100", RechargeHandoff.sbiStep1(RechargeOperator.BSNL, 100)?.body)
        assertEquals("MOBRC MTD 100", RechargeHandoff.sbiStep1(RechargeOperator.MTNL_DELHI, 100)?.body)
        assertEquals("MOBRC MTM 100", RechargeHandoff.sbiStep1(RechargeOperator.MTNL_MUMBAI, 100)?.body)
        assertEquals("MOBRC VI 100", RechargeHandoff.sbiStep1(RechargeOperator.VI, 100)?.body)
    }

    @Test
    fun sbiStep1RejectsAmountsAboveDailyCap() {
        assertNull(RechargeHandoff.sbiStep1(RechargeOperator.JIO, 701))
        assertNull(RechargeHandoff.sbiStep1(RechargeOperator.JIO, 0))
    }

    @Test
    fun sbiStep2IsTheOtpConfirmation() {
        val sms = RechargeHandoff.sbiStep2("123456")
        assertEquals(Handoff.Sms("917208933145", "MOBACC 123456", 2, 2), sms)
    }

    @Test
    fun sbiStep2RejectsAnIncompleteOtp() {
        assertNull(RechargeHandoff.sbiStep2("12 34"))
    }

    @Test
    fun axisSmsNormalisesThePhoneNumber() {
        val sms = RechargeHandoff.axis("+91 98765 43210", "JIO", 199, "123456")
        assertEquals(Handoff.Sms("9951860002", "MOBILE 9876543210 JIO 199 123456", 1, 1), sms)
    }

    @Test
    fun axisSmsRejectsInvalidInput() {
        assertNull(RechargeHandoff.axis("1234567890", "JIO", 199, "123456"))
        assertNull(RechargeHandoff.axis("9876543210", "JIO", 199, "12345"))
        assertNull(RechargeHandoff.axis("9876543210", "", 199, "123456"))
    }

    @Test
    fun upi123PayDialsTheIvrNumber() {
        assertEquals(Handoff.Dial("08045163666"), RechargeHandoff.upi123Pay())
    }

    @Test
    fun operatorMenusExistWhereUssdIsSupported() {
        assertEquals("*199#", RechargeHandoff.selfService(RechargeOperator.VI)?.number)
        assertEquals("*121#", RechargeHandoff.selfService(RechargeOperator.AIRTEL)?.number)
        assertEquals("*444#", RechargeHandoff.selfService(RechargeOperator.MTNL_DELHI)?.number)
        assertNull(RechargeHandoff.selfService(RechargeOperator.JIO))
    }
}
