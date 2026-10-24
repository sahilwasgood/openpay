package openpay.app

import openpay.app.notes.PayerLimits
import openpay.app.rails.Mdr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MdrAndLimitsTest {

    // ---------- Mdr ----------

    @Test
    fun noFeeAtOrBelowThreshold() {
        assertEquals(0L, Mdr.merchantFeePaise(0L))
        assertEquals(0L, Mdr.merchantFeePaise(200_000L))
    }

    @Test
    fun fortyRupeesOnTenThousand() {
        assertEquals(4_000L, Mdr.merchantFeePaise(1_000_000L)) // Rs 40
    }

    @Test
    fun capApplies() {
        assertEquals(30_000L, Mdr.merchantFeePaise(75_000_00L))   // Rs 300 on 75k
        assertEquals(30_000L, Mdr.merchantFeePaise(10_000_00_00L)) // Rs 300 on 10 lakh
    }

    @Test
    fun structuringDetected() {
        assertTrue(Mdr.looksLikeStructuring(1_000_000L, listOf(166_700L, 166_700L, 166_700L, 166_700L, 166_600L, 166_600L)))
        assertTrue(Mdr.looksLikeStructuring(600_000L, listOf(200_000L, 200_000L, 200_000L)))
    }

    @Test
    fun genuineSplitsAreNotFlagged() {
        // shares above the threshold are normal large payments
        assertFalse(Mdr.looksLikeStructuring(1_000_000L, listOf(500_000L, 500_000L)))
        // total below threshold: nothing to dodge
        assertFalse(Mdr.looksLikeStructuring(150_000L, listOf(75_000L, 75_000L)))
        // mixed shares: not a clean dodge pattern
        assertFalse(Mdr.looksLikeStructuring(1_000_000L, listOf(600_000L, 400_000L)))
    }

    // ---------- PayerLimits ----------

    @Test
    fun firstTimePayerNeedsConsentAbove500() {
        assertTrue(PayerLimits.needsConsent(50_100L, knownPayer = false))
        assertFalse(PayerLimits.needsConsent(50_000L, knownPayer = false))
        assertFalse(PayerLimits.needsConsent(10_00_000L, knownPayer = true))
    }
}
