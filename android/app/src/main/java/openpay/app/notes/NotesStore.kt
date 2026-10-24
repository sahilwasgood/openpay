package openpay.app.notes

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import openpay.app.data.Vault
import java.util.Base64

/**
 * Everything Notes related that lives on this phone: the payer keypair,
 * issued notes, receivables, pinned payer keys, and seen serials. All
 * encrypted at rest, all local.
 */
class NotesStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val keysVault = Vault(context, "notekeys.openpay")
    private val dataVault = Vault(context, "notedata.openpay")

    val issued = MutableStateFlow(loadIssued())
    val receivables = MutableStateFlow(loadReceivables())
    val pinnedKeys = MutableStateFlow<Map<String, String>>(loadPinned())
    val seenSerials = MutableStateFlow<Set<String>>(loadSerials())
    val payerName = MutableStateFlow(loadName())

    /** Public key and seed, base64. Created once, kept encrypted. */
    val keyPair: Pair<String, String> by lazy {
        val existing = keysVault.read()?.let {
            runCatching { json.decodeFromString<Pair<String, String>>(it) }.getOrNull()
        }
        if (existing != null) {
            existing
        } else {
            val seed = Notes.newKeySeed()
            val pub = Notes.publicKey(seed)
            val pair = Base64.getEncoder().encodeToString(pub) to
                Base64.getEncoder().encodeToString(seed)
            keysVault.write(json.encodeToString(pair))
            pair
        }
    }

    val publicKeyB64: String get() = keyPair.first

    fun setPayerName(name: String) {
        payerName.value = name
        persist()
    }

    val openPaise: Long
        get() = issued.value.filter { it.open }.sumOf { it.payload.amountPaise }

    sealed interface IssueResult {
        data class Created(val issued: Notes.Issued, val envelope: Notes.Envelope) : IssueResult
        data class Refused(val reason: String) : IssueResult
    }

    fun issue(
        to: String,
        vpa: String?,
        phone: String?,
        amountPaise: Long,
        validMinutes: Int,
        memo: String,
        from: String
    ): IssueResult {
        if (amountPaise <= 0) return IssueResult.Refused("amount must be positive")
        if (amountPaise > Notes.MAX_NOTE_PAISE) {
            return IssueResult.Refused("notes cap at Rs 2,000 each")
        }
        if (openPaise + amountPaise > Notes.MAX_OPEN_PAISE) {
            return IssueResult.Refused("Rs 10,000 open at once is the cap")
        }
        if (to.isBlank()) return IssueResult.Refused("payee name required")
        if (vpa.isNullOrBlank() && phone.isNullOrBlank()) {
            return IssueResult.Refused("add a UPI ID or phone so it can settle")
        }
        val payload = Notes.Payload(
            from = from.ifBlank { "OpenPay payer" },
            payerKey = publicKeyB64,
            to = to,
            vpa = vpa?.trim()?.ifBlank { null },
            phone = phone?.trim()?.ifBlank { null },
            amountPaise = amountPaise,
            serial = Notes.serial(),
            nonce = Notes.nonce(),
            expiresEpoch = System.currentTimeMillis() + validMinutes * 60_000L,
            memo = memo.take(60)
        )
        val seed = Base64.getDecoder().decode(keyPair.second)
        val sig = Notes.sign(seed, Notes.canonical(payload).toByteArray())
        val envelope = Notes.Envelope(
            d = payload,
            sig = Base64.getEncoder().encodeToString(sig)
        )
        val record = Notes.Issued(
            payload = payload,
            issuedAtEpoch = System.currentTimeMillis(),
            envelope = Notes.encode(envelope)
        )
        issued.value = listOf(record) + issued.value
        seenSerials.value = seenSerials.value + payload.serial
        persist()
        return IssueResult.Created(record, envelope)
    }

    fun acceptVerified(payload: Notes.Payload, knownPayer: Boolean) {
        receivables.value = listOf(
            Notes.Receivable(
                payload = payload,
                receivedAtEpoch = System.currentTimeMillis(),
                knownPayer = knownPayer
            )
        ) + receivables.value
        seenSerials.value = seenSerials.value + payload.serial
        persist()
    }

    fun pinPayer(key: String, name: String) {
        if (key.isBlank()) return
        pinnedKeys.value = pinnedKeys.value + (key to name.ifBlank { "saved payer" })
        persist()
    }

    fun envelopeFor(serial: String): Notes.Envelope? =
        issued.value.firstOrNull { it.payload.serial == serial }?.envelope?.let { Notes.parse(it) }

    fun markSettled(serial: String) {
        issued.value = issued.value.map {
            if (it.payload.serial == serial && it.open) {
                it.copy(settledAtEpoch = System.currentTimeMillis())
            } else it
        }
        persist()
    }

    fun verifyIncoming(content: String): Notes.Verdict =
        Notes.verifyNote(
            content = content,
            nowEpoch = System.currentTimeMillis(),
            seenSerials = seenSerials.value,
            pinnedKeys = pinnedKeys.value.keys
        )

    private fun persist() {
        dataVault.write(
            json.encodeToString(
                Persisted(
                    issued = issued.value,
                    receivables = receivables.value,
                    pinned = pinnedKeys.value,
                    serials = seenSerials.value.toList(),
                    payerName = payerName.value
                )
            )
        )
    }

    @kotlinx.serialization.Serializable
    private data class Persisted(
        val issued: List<Notes.Issued> = emptyList(),
        val receivables: List<Notes.Receivable> = emptyList(),
        val pinned: Map<String, String> = emptyMap(),
        val serials: List<String> = emptyList(),
        val payerName: String = ""
    )

    private fun loadPersisted(): Persisted? =
        dataVault.read()?.let { runCatching { json.decodeFromString<Persisted>(it) }.getOrNull() }

    private fun loadIssued() = loadPersisted()?.issued ?: emptyList()
    private fun loadReceivables() = loadPersisted()?.receivables ?: emptyList()
    private fun loadPinned() = loadPersisted()?.pinned ?: emptyMap()
    private fun loadSerials() = loadPersisted()?.serials?.toSet() ?: emptySet()
    private fun loadName() = loadPersisted()?.payerName ?: ""
}
