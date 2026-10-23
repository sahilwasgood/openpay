package openpay.app

import openpay.app.data.RupeeFormat
import openpay.app.rails.IntentRail
import openpay.app.rails.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAndLedgerTest {

    @Test
    fun senderMatching() {
        assertTrue(SmsParser.senderMatchesBank("AD-HDFCBK", listOf("HDFCBK")))
        assertTrue(SmsParser.senderMatchesBank("VM-SBIBNK", listOf("SBI", "SBIBNK")))
        assertFalse(SmsParser.senderMatchesBank("AD-JIOFIB", listOf("HDFCBK")))
        assertFalse(SmsParser.senderMatchesBank(null, listOf("HDFCBK")))
        assertFalse(SmsParser.senderMatchesBank("AD-HDFCBK", emptyList()))
    }

    @Test
    fun debitConfirmationMatchesAmount() {
        val body = "Rs 45.00 debited from a/c XX1234 on 24-10-26 ref 447190211234 UPI Meera Chai"
        assertTrue(SmsParser.isDebitConfirmation(body, 4500))
        assertFalse(SmsParser.isDebitConfirmation(body, 4600))
    }

    @Test
    fun debitConfirmationPlainRupees() {
        assertTrue(SmsParser.isDebitConfirmation("INR 500 debited towards UPI", 50_000))
        assertTrue(SmsParser.isDebitConfirmation("Rs.1,250 debited", 125_000))
    }

    @Test
    fun creditSmsIsNotAConfirmation() {
        assertFalse(SmsParser.isDebitConfirmation("Rs 45.00 credited to your a/c", 4500))
    }

    @Test
    fun refExtraction() {
        assertEquals(
            "447190211234",
            SmsParser.extractRef("Rs 45 debited ref no 447190211234 on UPI")
        )
        assertEquals("UTN8832190A", SmsParser.extractRef("debited, UTR UTN8832190A towards"))
        assertNull(SmsParser.extractRef("Rs 45 debited, no reference here at all"))
    }

    @Test
    fun intentResponseNormalization() {
        assertEquals("SUCCESS", IntentRail.normalizeResponse("success"))
        assertEquals("FAILURE", IntentRail.normalizeResponse(" FAILED "))
        assertEquals("SUBMITTED", IntentRail.normalizeResponse("Submitted"))
        assertNull(IntentRail.normalizeResponse(null))
        assertNull(IntentRail.normalizeResponse("whatever"))
    }

    @Test
    fun rupeeFormatting() {
        assertEquals("45", RupeeFormat.format(4500))
        assertEquals("45.50", RupeeFormat.format(4550))
        assertEquals("0.05", RupeeFormat.format(5))
    }
}
