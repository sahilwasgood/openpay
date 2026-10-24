package openpay.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import openpay.app.OpenPayApp

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as? OpenPayApp ?: return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        val sender = messages.firstOrNull()?.originatingAddress
        val body = messages.joinToString("") { it.displayMessageBody.orEmpty() }
        if (body.isBlank()) return
        app.store.onSms(sender, body)
        if (app.store.expensesEnabled.value) {
            app.expenseStore.ingest(sender, body, System.currentTimeMillis())
        }
    }
}
