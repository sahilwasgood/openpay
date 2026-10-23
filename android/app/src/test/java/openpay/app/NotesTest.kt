package openpay.app

import openpay.app.notes.Notes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class NotesTest {

    private fun payload(
        amountPaise: Long = 45_000,
        expiresEpoch: Long = Long.MAX_VALUE,
        from: String = "Meera",
        payerKey: String? = null,
        serial: String = "NB7F04A1"
    ): Notes.Payload {
        val seed = Notes.newKeySeed()
        val key = payerKey ?: Base64.getEncoder().encodeToString(Notes.publicKey(seed))
        return Notes.Payload(
            from = from,
            payerKey = key,
            to = "Sahil",
            vpa = "sahil@okaxis",
            phone = null,
            amountPaise = amountPaise,
            serial = serial,
            nonce = "9C22E610",
            expiresEpoch = expiresEpoch,
            memo = "chai"
        )
    }

    private fun freshSigned(): Pair<Notes.Payload, String> {
        val seed = Notes.newKeySeed()
        val p = payload(payerKey = Base64.getEncoder().encodeToString(Notes.publicKey(seed)))
        val sig = Notes.sign(seed, Notes.canonical(p).toByteArray())
        return p to Notes.encode(Notes.Envelope(d = p, sig = Base64.getEncoder().encodeToString(sig)))
    }

    @Test
    fun signVerifyRoundtrip() {
        val seed = Notes.newKeySeed()
        val pub = Notes.publicKey(seed)
        val msg = "openpay".toByteArray()
        val sig = Notes.sign(seed, msg)
        assertTrue(Notes.verify(pub, msg, sig))
        assertFalse(Notes.verify(pub, "openpaY".toByteArray(), sig))
        assertFalse(Notes.verify(ByteArray(32), msg, sig))
    }

    @Test
    fun validNoteVerifies() {
        val (p, content) = freshSigned()
        val verdict = Notes.verifyNote(content, nowEpoch = 1000L, seenSerials = emptySet(), pinnedKeys = emptySet())
        assertTrue(verdict is Notes.Verdict.Valid)
        verdict as Notes.Verdict.Valid
        assertEquals(p.serial, verdict.payload.serial)
        assertFalse(verdict.knownPayer)
        assertFalse(verdict.duplicateSerial)
    }

    @Test
    fun tamperedAmountBreaksSignature() {
        val (p, content) = freshSigned()
        val tampered = content.replace("45000", "450000", ignoreCase = false)
        val verdict = Notes.verifyNote(tampered, 1000L, emptySet(), emptySet())
        assertTrue(verdict is Notes.Verdict.Bad)
    }

    @Test
    fun expiredNoteRejected() {
        val seed = Notes.newKeySeed()
        val p = payload(
            expiresEpoch = 2000L,
            payerKey = Base64.getEncoder().encodeToString(Notes.publicKey(seed))
        )
        val sig = Notes.sign(seed, Notes.canonical(p).toByteArray())
        val content = Notes.encode(Notes.Envelope(d = p, sig = Base64.getEncoder().encodeToString(sig)))
        val verdict = Notes.verifyNote(content, nowEpoch = 5000L, emptySet(), emptySet())
        assertTrue(verdict is Notes.Verdict.Bad)
        assertEquals("note expired", (verdict as Notes.Verdict.Bad).reason)
    }

    @Test
    fun overCapAmountRejected() {
        val seed = Notes.newKeySeed()
        val p = payload(
            amountPaise = Notes.MAX_NOTE_PAISE + 100,
            payerKey = Base64.getEncoder().encodeToString(Notes.publicKey(seed))
        )
        val sig = Notes.sign(seed, Notes.canonical(p).toByteArray())
        val content = Notes.encode(Notes.Envelope(d = p, sig = Base64.getEncoder().encodeToString(sig)))
        val verdict = Notes.verifyNote(content, 1000L, emptySet(), emptySet())
        assertTrue(verdict is Notes.Verdict.Bad)
        assertEquals("amount outside note limits", (verdict as Notes.Verdict.Bad).reason)
    }

    @Test
    fun duplicateAndKnownSerialsFlagged() {
        val (_, content) = freshSigned()
        val verdict = Notes.verifyNote(content, 1000L, seenSerials = setOf("NB7F04A1"), pinnedKeys = emptySet())
        assertTrue(verdict is Notes.Verdict.Valid)
        assertTrue((verdict as Notes.Verdict.Valid).duplicateSerial)
    }

    @Test
    fun garbageRejected() {
        assertTrue(Notes.verifyNote("hello", 1000L, emptySet(), emptySet()) is Notes.Verdict.Bad)
        assertNotNull(Notes.parse(Notes.encode(Notes.Envelope(d = payload(), sig = "AA=="))))
    }

    @Test
    fun canonicalIsDeterministic() {
        val p = payload()
        assertEquals(Notes.canonical(p), Notes.canonical(p.copy()))
        assertFalse(Notes.canonical(p) == Notes.canonical(p.copy(amountPaise = 100)))
    }
}
