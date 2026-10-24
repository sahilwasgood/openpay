package openpay.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import openpay.app.data.Vault
import openpay.app.rails.ExpenseParser

/**
 * Private spending log built from the user's own bank debit SMS. Stays on
 * the phone: the app has no internet permission, so it cannot leave.
 */
class ExpenseStore(context: Context) {

    companion object {
        private const val MAX_ENTRIES = 500
    }

    @Serializable
    data class Expense(
        val createdAtEpoch: Long,
        val amountPaise: Long,
        val payeeHint: String? = null,
        val ref: String? = null,
        val sender: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val vault = Vault(context, "expenses.openpay")

    val expenses = MutableStateFlow(load())

    /** Latest capture wins; entries cap so the vault stays small. */
    fun ingest(sender: String?, body: String, nowEpoch: Long) {
        val parsed = ExpenseParser.parseDebit(body) ?: return
        val current = expenses.value
        if (parsed.ref != null && current.any { it.ref == parsed.ref }) return
        val entry = Expense(
            createdAtEpoch = nowEpoch,
            amountPaise = parsed.amountPaise,
            payeeHint = parsed.payeeHint,
            ref = parsed.ref,
            sender = sender
        )
        expenses.value = (listOf(entry) + current).take(MAX_ENTRIES)
        persist()
    }

    private fun persist() {
        vault.write(json.encodeToString(expenses.value))
    }

    private fun load(): List<Expense> =
        vault.read()?.let {
            runCatching { json.decodeFromString<List<Expense>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
}
