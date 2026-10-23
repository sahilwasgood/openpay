package openpay.app

import openpay.app.data.PayState
import openpay.app.data.Payee
import openpay.app.data.Payment
import openpay.app.data.Rail
import openpay.app.rails.SmsConfirmations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsConfirmationsTest {

    private fun payment(
        createdAt: Long,
        amountPaise: Long = 45_000,
        rail: Rail = Rail.USSD,
        state: PayState = PayState.PENDING
    ) = Payment(
        id = "p$createdAt",
        createdAtEpoch = createdAt,
        payee = Payee("Meera", "meera@okhdfcbank", null),
        amountPaise = amountPaise,
        rail = rail,
        state = state
    )

    private val window = 6 * 60 * 1000L
    private val now = 10_000_000L

    @Test
    fun picksNewestPendingNonIntentInsideWindow() {
        val old = payment(now - window - 1000)
        val recent = payment(now - 60_000)
        val intent = payment(now - 30_000, rail = Rail.INTENT)
        val done = payment(now - 20_000, state = PayState.SUCCESS)
        val picked = SmsConfirmations.findCandidate(listOf(old, recent, intent, done), now, window)
        assertEquals(recent.id, picked?.id)
    }

    @Test
    fun confirmRequiresBankSender() {
        val p = payment(now - 60_000)
        val updated = SmsConfirmations.confirm(
            p, sender = "AD-JIOFIB", bankSenders = listOf("HDFCBK"),
            body = "Rs 450.00 debited", nowEpoch = now
        )
        assertNull(updated)
    }

    @Test
    fun confirmMatchesAmountAndSetsRef() {
        val p = payment(now - 60_000)
        val updated = SmsConfirmations.confirm(
            p, sender = "AD-HDFCBK", bankSenders = listOf("HDFCBK"),
            body = "Rs 450.00 debited from a/c XX12 ref no 447190211234", nowEpoch = now
        )
        assertNotNull(updated)
        updated!!
        assertEquals(PayState.SUCCESS, updated.state)
        assertTrue(updated.smsConfirmed)
        assertEquals("447190211234", updated.bankRef)
        assertEquals(now, updated.settledAtEpoch)
    }

    @Test
    fun confirmRejectsWrongAmount() {
        val p = payment(now - 60_000)
        assertNull(
            SmsConfirmations.confirm(
                p, sender = "AD-HDFCBK", bankSenders = listOf("HDFCBK"),
                body = "Rs 460.00 debited", nowEpoch = now
            )
        )
    }

    @Test
    fun otherBankSkipsSenderCheckButNotAmount() {
        val p = payment(now - 60_000)
        val ok = SmsConfirmations.confirm(
            p, sender = "XY-ANYONE", bankSenders = emptyList(),
            body = "INR 450 debited", nowEpoch = now
        )
        assertNotNull(ok)
        val bad = SmsConfirmations.confirm(
            p, sender = "XY-ANYONE", bankSenders = emptyList(),
            body = "INR 999 debited", nowEpoch = now
        )
        assertFalse(bad != null)
    }
}
