package openpay.app

import openpay.app.rails.IvrRail
import openpay.app.rails.NetKind
import openpay.app.rails.RailContext
import openpay.app.rails.RailDecider
import openpay.app.rails.RailPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RailLogicTest {

    private fun ctx(
        net: NetKind,
        carrier: String? = "Airtel",
        vpa: Boolean = true,
        phone: Boolean = true
    ) = RailContext(net, carrier, payeeHasVpa = vpa, payeeHasPhone = phone)

    @Test
    fun onlinePicksIntent() {
        assertEquals(RailPick.Intent, RailDecider.pick(ctx(NetKind.ONLINE)))
    }

    @Test
    fun signalNonJioWithVpaPicksUssd() {
        assertEquals(RailPick.Ussd, RailDecider.pick(ctx(NetKind.SIGNAL, carrier = "Airtel")))
    }

    @Test
    fun signalJioPicksIvr() {
        assertEquals(
            RailPick.Ivr,
            RailDecider.pick(ctx(NetKind.SIGNAL, carrier = "Jio 4G"))
        )
    }

    @Test
    fun jioWithVpaOnlyHasNoRail() {
        val pick = RailDecider.pick(ctx(NetKind.SIGNAL, carrier = "Reliance Jio", phone = false))
        assertTrue(pick is RailPick.None)
        assertEquals(RailPick.Reason.OFFLINE_NO_PHONE, (pick as RailPick.None).reason)
    }

    @Test
    fun nonJioPhoneOnlyPicksIvr() {
        assertEquals(
            RailPick.Ivr,
            RailDecider.pick(ctx(NetKind.SIGNAL, carrier = "Vi", vpa = false, phone = true))
        )
    }

    @Test
    fun noNetworkHasNoRail() {
        val pick = RailDecider.pick(ctx(NetKind.NONE))
        assertTrue(pick is RailPick.None)
        assertEquals(RailPick.Reason.NO_SIGNAL, (pick as RailPick.None).reason)
    }

    @Test
    fun carrierDetection() {
        assertTrue(RailDecider.isJio("Jio 4G"))
        assertTrue(RailDecider.isJio("reliance jio"))
        assertFalse(RailDecider.isJio("Airtel"))
        assertFalse(RailDecider.isJio(null))
    }

    @Test
    fun ivrDtmfGolden() {
        assertEquals("1,,9876543210,,45,,1", IvrRail.dtmfSuffix("9876543210", 45))
        assertEquals("1,,9876543210,,10000,,1", IvrRail.dtmfSuffix("+91 98765 43210", 10_000))
    }

    @Test
    fun ivrRejectsBadInput() {
        assertNull(IvrRail.dtmfSuffix("1234567890", 45))    // not a mobile prefix
        assertNull(IvrRail.dtmfSuffix("98765432", 45))      // too short
        assertNull(IvrRail.dtmfSuffix("9876543210", 0))     // zero amount
        assertNull(IvrRail.dtmfSuffix("9876543210", 10_001)) // over the rail cap
    }
}
