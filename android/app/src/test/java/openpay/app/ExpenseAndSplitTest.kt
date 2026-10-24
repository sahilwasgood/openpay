package openpay.app

import openpay.app.data.PayState
import openpay.app.data.Payee
import openpay.app.data.Payment
import openpay.app.data.Rail
import openpay.app.notes.Split
import openpay.app.rails.ExpenseParser
import openpay.app.rails.Guards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseAndSplitTest {

    // ---------- ExpenseParser ----------

    @Test
    fun parsesHdfcDebitWithVpa() {
        val body = "Rs 45.00 debited from a/c XX1234 on 24-10-26 ref 447190211234 UPI meera@okhdfcbank"
        val parsed = ExpenseParser.parseDebit(body)
        assertNotNull(parsed)
        assertEquals(4500L, parsed!!.amountPaise)
        assertEquals("meera@okhdfcbank", parsed.payeeHint)
        assertEquals("447190211234", parsed.ref)
    }

    @Test
    fun parsesTowardsHintWhenNoVpa() {
        val parsed = ExpenseParser.parseDebit("INR 1,250 debited towards Amazon Pay on 24-10 ref 99881X")
        assertNotNull(parsed)
        assertEquals(125_000L, parsed!!.amountPaise)
        assertEquals("Amazon Pay", parsed.payeeHint)
    }

    @Test
    fun ignoresCredits() {
        assertNull(ExpenseParser.parseDebit("Rs 5000 credited to your a/c XX12 ref 123456"))
        assertNull(ExpenseParser.parseDebit("Your salary of Rs 50,000 has been received"))
    }

    @Test
    fun ignoresNonDebitWordings() {
        assertNull(ExpenseParser.parseDebit("Your OTP is 4821. Do not share."))
        assertNull(ExpenseParser.parseDebit("Hi"))
    }

    @Test
    fun parsesPlainDebit() {
        val parsed = ExpenseParser.parseDebit("Rs 45 debited from a/c XX12")
        assertNotNull(parsed)
        assertEquals(4500L, parsed!!.amountPaise)
        assertNull(parsed.payeeHint)
    }

    // ---------- Split ----------

    @Test
    fun splitsEvenly() {
        assertEquals(listOf(100L, 100L, 100L), Split.shares(300L, 3))
    }

    @Test
    fun remainderGoesToFirstShares() {
        assertEquals(listOf(34L, 34L, 33L), Split.shares(101L, 3))
        assertEquals(listOf(1L, 0L), Split.shares(1L, 2))
    }

    @Test
    fun partsAlwaysSumToTotal() {
        listOf(4599L to 4, 1L to 3, 100000L to 7, 250L to 6).forEach { (total, people) ->
            assertEquals(total, Split.shares(total, people).sum())
        }
    }

    @Test
    fun degenerateSplits() {
        assertEquals(emptyList<Long>(), Split.shares(100L, 0))
        assertEquals(emptyList<Long>(), Split.shares(0L, 3))
        assertEquals(listOf(100L), Split.shares(100L, 1))
    }

    // ---------- Guards ----------

    private fun pending(amountPaise: Long, vpa: String?, phone: String?, createdAt: Long) = Payment(
        id = "p$createdAt",
        createdAtEpoch = createdAt,
        payee = Payee("X", vpa, phone),
        amountPaise = amountPaise,
        rail = Rail.USSD
    )

    @Test
    fun duplicateDetectedInsideWindow() {
        val now = 10_000_000L
        val existing = pending(45_000, "meera@ok", null, now - 30_000)
        val dup = Guards.findDuplicate(
            listOf(existing), 45_000, "meera@ok", null, now, 6 * 60_000L
        )
        assertEquals(existing.id, dup?.id)
    }

    @Test
    fun noDuplicateForDifferentAmountOrOutsideWindow() {
        val now = 10_000_000L
        val existing = pending(45_000, "meera@ok", null, now - 30_000)
        val old = pending(45_000, "meera@ok", null, now - 7 * 60_000L)
        assertNull(Guards.findDuplicate(listOf(existing), 46_000, "meera@ok", null, now, 6 * 60_000L))
        assertNull(Guards.findDuplicate(listOf(old), 45_000, "meera@ok", null, now, 6 * 60_000L))
    }

    @Test
    fun noDuplicateWhenSettled() {
        val now = 10_000_000L
        val settled = pending(45_000, "meera@ok", null, now - 30_000).copy(state = PayState.SUCCESS)
        assertNull(Guards.findDuplicate(listOf(settled), 45_000, "meera@ok", null, now, 6 * 60_000L))
    }

    @Test
    fun phoneMatchesAlsoCount() {
        val now = 10_000_000L
        val existing = pending(45_000, null, "9876543210", now - 30_000)
        val dup = Guards.findDuplicate(listOf(existing), 45_000, null, "9876543210", now, 6 * 60_000L)
        assertEquals(existing.id, dup?.id)
    }
}
