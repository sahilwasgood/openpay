package openpay.app.ui

import openpay.app.ui.theme.WarnAmber
import openpay.app.ui.theme.FaintInk
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import openpay.app.OpenPayApp
import openpay.app.Store
import openpay.app.data.PayState
import openpay.app.data.Payment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(app: OpenPayApp, id: String, onBack: () -> Unit) {
    val payments by app.store.payments.collectAsState()
    val payment = payments.firstOrNull { it.id == id }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var confirmFail by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("Payment")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (payment == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s("This payment is gone from the ledger."), color = FaintInk)
            }
            return@Scaffold
        }

        val ageMs = System.currentTimeMillis() - payment.createdAtEpoch
        val windowClosed = payment.state == PayState.PENDING && ageMs > Store.WINDOW_MS

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(payment.rail.name)
                    Text(
                        "₹${payment.amountRupeesString}",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        payment.payee.displayName.ifBlank {
                            payment.payee.vpa ?: payment.payee.phone ?: "Payee"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                StateChip(payment.state)
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow(s("When"), fullLabel(payment.createdAtEpoch))
                    payment.payee.vpa?.let { DetailRow("UPI ID", it) }
                    payment.payee.phone?.let { DetailRow(s("Phone"), it) }
                    if (payment.note.isNotBlank()) DetailRow(s("Note"), payment.note)
                    DetailRow(s("Rail"), railName(payment))
                    payment.bankRef?.let { DetailRow(s("Bank ref"), it) }
                    DetailRow(
                        s("Confirmed by"),
                        when {
                            payment.smsConfirmed -> s("your bank's SMS")
                            payment.intentResult != null -> s("the UPI app's response")
                            else -> s("nothing yet")
                        }
                    )
                    DetailRow(s("Record"), payment.id.take(8).uppercase())
                }
            }

            when (payment.state) {
                PayState.SUCCESS -> Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        payment.bankRef?.let { s("Confirmed. Bank reference {r}.", "r" to it) }
                            ?: s("Confirmed by the UPI app response."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                PayState.PENDING -> if (windowClosed) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                s("Pending past the SMS window."),
                                style = MaterialTheme.typography.titleMedium,
                                color = WarnAmber
                            )
                            Text(
                                s("If the money left your account, banks must auto reverse failed payments by T+1, and refunds must land within T+5 working days. After that the bank owes Rs 100 per day (RBI turn around time rules)."),
                                style = MaterialTheme.typography.bodySmall,
                                color = FaintInk
                            )
                        }
                    }
                } else {
                    Text(
                        s("Waiting for your bank's SMS. Usually under a minute."),
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk
                    )
                }
                PayState.FAILED -> Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        s("Marked failed. Nothing left this phone."),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://upihelp.npci.org.in"))
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Open NPCI help"), style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(escalationEmail(payment)))
                    Toast.makeText(context, ts("Escalation email copied"), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Copy escalation email"), style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, shareText(payment)),
                                ts("Share payment record")
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Share record"), style = MaterialTheme.typography.labelLarge)
            }
            if (payment.state == PayState.PENDING) {
                TextButton(onClick = { confirmFail = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(s("This payment never happened"), color = FaintInk)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmFail && payment != null) {
        AlertDialog(
            onDismissRequest = { confirmFail = false },
            title = { Text(s("Mark as never paid?")) },
            text = { Text(s("This only changes your local record. It cannot reverse a real debit.")) },
            confirmButton = {
                TextButton(onClick = {
                    app.store.update(payment.id) {
                        it.copy(
                            state = PayState.FAILED,
                            updatedAtEpoch = System.currentTimeMillis()
                        )
                    }
                    confirmFail = false
                }) { Text(s("Mark failed")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmFail = false }) { Text(s("Keep pending")) }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = FaintInk,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun railName(payment: Payment): String = when (payment.rail) {
    openpay.app.data.Rail.INTENT -> "UPI Intent"
    openpay.app.data.Rail.USSD -> "USSD *99#"
    openpay.app.data.Rail.IVR -> "IVR 123Pay"
}

private fun escalationEmail(payment: Payment): String {
    val ref = payment.bankRef ?: payment.id.take(8).uppercase()
    val payeeName = payment.payee.displayName.ifBlank {
        payment.payee.vpa ?: payment.payee.phone ?: "the payee"
    }
    return """
        Subject: Pending UPI transaction, ref $ref

        A UPI payment of Rs ${payment.amountRupeesString} to $payeeName made on
        ${fullLabel(payment.createdAtEpoch)} via ${railNameText(payment)} is still pending.

        Under NPCI rules a failed payment must auto reverse by T+1, and any refund must
        reach me within T+5 working days, failing which the bank owes Rs 100 per day
        under RBI directions on turn around time.

        Please confirm the status and reverse or complete this transaction.

        Ref: $ref
        Record: ${payment.id.take(8).uppercase()}
    """.trimIndent()
}

private fun shareText(payment: Payment): String {
    val payeeName = payment.payee.displayName.ifBlank {
        payment.payee.vpa ?: payment.payee.phone ?: "Payee"
    }
    return """
        OpenPay payment record
        Amount: Rs ${payment.amountRupeesString}
        Payee: $payeeName
        When: ${fullLabel(payment.createdAtEpoch)}
        Rail: ${railNameText(payment)}
        State: ${payment.state.name.lowercase()}
        Ref: ${payment.bankRef ?: "none"}
        Record: ${payment.id.take(8).uppercase()}
    """.trimIndent()
}

private fun railNameText(payment: Payment): String = when (payment.rail) {
    openpay.app.data.Rail.INTENT -> "UPI intent"
    openpay.app.data.Rail.USSD -> "USSD *99#"
    openpay.app.data.Rail.IVR -> "IVR 123Pay"
}
