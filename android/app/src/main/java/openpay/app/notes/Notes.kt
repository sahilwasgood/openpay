package openpay.app.notes

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * OpenPay Notes: signed offline payment vouchers. Rail 4. The payer's phone
 * signs, the payee's phone verifies with zero network, and the note settles
 * over UPI later. Signatures are Ed25519. Not money, never held by us:
 * a Note is a promise with proof attached.
 */
object Notes {

    const val MAX_NOTE_PAISE: Long = 200_000L        // Rs 2,000 each
    const val MAX_OPEN_PAISE: Long = 1_000_000L      // Rs 10,000 outstanding

    private val json = Json { ignoreUnknownKeys = true }
    private val curve = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    private val random = SecureRandom()

    @Serializable
    data class Payload(
        val v: Int = 1,
        val from: String,
        val payerKey: String,
        val to: String,
        val vpa: String? = null,
        val phone: String? = null,
        val amountPaise: Long,
        val serial: String,
        val nonce: String,
        val expiresEpoch: Long,
        val memo: String = ""
    )

    @Serializable
    data class Envelope(
        val op: String = "openpay-note",
        val d: Payload,
        val sig: String
    )

    @Serializable
    data class Issued(
        val payload: Payload,
        val issuedAtEpoch: Long,
        val settledAtEpoch: Long? = null
    ) {
        val open: Boolean get() = settledAtEpoch == null
    }

    @Serializable
    data class Receivable(
        val payload: Payload,
        val receivedAtEpoch: Long,
        val knownPayer: Boolean
    )

    fun newKeySeed(): ByteArray = ByteArray(32).also { random.nextBytes(it) }

    fun publicKey(seed: ByteArray): ByteArray {
        val key = EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, curve))
        val encoded = key.abyte
        return if (encoded.size == 32) encoded else encoded.copyOf(32)
    }

    fun sign(seed: ByteArray, message: ByteArray): ByteArray {
        val key = EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, curve))
        val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        engine.initSign(key)
        engine.update(message)
        return engine.sign()
    }

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        runCatching {
            val key = EdDSAPublicKey(EdDSAPublicKeySpec(publicKey, curve))
            val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
            engine.initVerify(key)
            engine.update(message)
            engine.verify(signature)
        }.getOrDefault(false)

    /**
     * Canonical, deterministic signing string. Fields are pipe joined so the
     * JSON serializer can never reorder what was signed.
     */
    fun canonical(p: Payload): String = listOf(
        "openpay-note",
        p.v.toString(),
        p.from,
        p.payerKey,
        p.to,
        p.vpa.orEmpty(),
        p.phone.orEmpty(),
        p.amountPaise.toString(),
        p.serial,
        p.nonce,
        p.expiresEpoch.toString(),
        p.memo
    ).joinToString("|")

    fun serial(): String {
        val bytes = ByteArray(3).also { random.nextBytes(it) }
        return "NB" + bytes.joinToString("") { "%02X".format(it) } +
            "%04X".format(random.nextInt(0x10000))
    }

    fun nonce(): String {
        val bytes = ByteArray(4).also { random.nextBytes(it) }
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun encode(envelope: Envelope): String = json.encodeToString(envelope)

    fun parse(content: String): Envelope? {
        val envelope = runCatching { json.decodeFromString<Envelope>(content) }.getOrNull()
            ?: return null
        if (envelope.op != "openpay-note") return null
        return envelope
    }

    sealed interface Verdict {
        data class Valid(
            val payload: Payload,
            val knownPayer: Boolean,
            val duplicateSerial: Boolean
        ) : Verdict

        data class Bad(val reason: String) : Verdict
    }

    /**
     * Full offline verification: signature over the canonical string, expiry,
     * amount cap, and replay by serial.
     */
    fun verifyNote(
        content: String,
        nowEpoch: Long,
        seenSerials: Set<String>,
        pinnedKeys: Set<String>
    ): Verdict {
        val envelope = parse(content) ?: return Verdict.Bad("not an OpenPay note")
        val payload = envelope.d
        val pub = runCatching { java.util.Base64.getDecoder().decode(payload.payerKey) }
            .getOrNull() ?: return Verdict.Bad("bad payer key")
        val sig = runCatching { java.util.Base64.getDecoder().decode(envelope.sig) }
            .getOrNull() ?: return Verdict.Bad("bad signature encoding")
        val message = canonical(payload).toByteArray()
        if (!verify(pub, message, sig)) return Verdict.Bad("signature does not match")
        if (payload.amountPaise <= 0 || payload.amountPaise > MAX_NOTE_PAISE) {
            return Verdict.Bad("amount outside note limits")
        }
        if (nowEpoch >= payload.expiresEpoch) return Verdict.Bad("note expired")
        if (payload.serial.isBlank() || payload.nonce.isBlank()) return Verdict.Bad("missing serial or nonce")
        return Verdict.Valid(
            payload = payload,
            knownPayer = payload.payerKey in pinnedKeys,
            duplicateSerial = payload.serial in seenSerials
        )
    }
}
