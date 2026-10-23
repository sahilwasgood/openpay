package openpay.app

import android.app.Application
import android.content.Context
import openpay.app.data.Banks
import openpay.app.data.Ledger
import openpay.app.data.PayState
import openpay.app.data.Payment
import openpay.app.data.Rail
import openpay.app.rails.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenPayApp : Application() {

    lateinit var store: Store
        private set

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
    }
}

/**
 * Single source of truth. Ledger writes go through an IO coroutine, state is
 * exposed as flows for Compose. Everything lives on the device.
 */
class Store(context: Context) {

    companion object {
        /** How long after a dial we still accept a bank SMS as confirmation. */
        const val WINDOW_MS: Long = 6 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private val ledger = Ledger(appContext)
    private val prefs = appContext.getSharedPreferences("openpay", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val payments = MutableStateFlow(ledger.readAll())
    val bankIndex = MutableStateFlow(prefs.getInt("bank", Banks.all.lastIndex))
    val onboarded = MutableStateFlow(prefs.getBoolean("onboarded", false))

    fun setBank(index: Int) {
        prefs.edit().putInt("bank", index).apply()
        bankIndex.value = index
    }

    fun setOnboarded() {
        prefs.edit().putBoolean("onboarded", true).apply()
        onboarded.value = true
    }

    fun add(payment: Payment) {
        payments.value = listOf(payment) + payments.value
        val snapshot = payments.value
        scope.launch { runCatching { ledger.writeAll(snapshot) } }
    }

    fun update(id: String, transform: (Payment) -> Payment) {
        val updated = payments.value.map { if (it.id == id) transform(it) else it }
        payments.value = updated
        scope.launch { runCatching { ledger.writeAll(updated) } }
    }

    /**
     * A bank SMS arrived. Only counts when the sender is the user's bank,
     * the newest pending non intent payment is inside its window, and the
     * body reads as a debit of exactly that amount.
     */
    fun onSms(sender: String?, body: String) {
        val bank = Banks.all.getOrNull(bankIndex.value) ?: return
        if (bank.senders.isNotEmpty() && !SmsParser.senderMatchesBank(sender, bank.senders)) return
        val candidate = payments.value
            .filter {
                it.state == PayState.PENDING && it.rail != Rail.INTENT &&
                    System.currentTimeMillis() - it.createdAtEpoch < WINDOW_MS
            }
            .maxByOrNull { it.createdAtEpoch }
            ?: return
        if (!SmsParser.isDebitConfirmation(body, candidate.amountPaise)) return
        val now = System.currentTimeMillis()
        val ref = SmsParser.extractRef(body) ?: candidate.bankRef
        update(candidate.id) {
            it.copy(
                state = PayState.SUCCESS,
                smsConfirmed = true,
                bankRef = ref,
                updatedAtEpoch = now,
                settledAtEpoch = now
            )
        }
    }
}
