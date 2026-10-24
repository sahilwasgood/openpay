package openpay.app.ui

import openpay.app.ui.theme.FaintInk
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.core.content.FileProvider
import openpay.app.data.Payment
import java.io.File
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import openpay.app.OpenPayApp
import openpay.app.data.Banks
import openpay.app.rails.NetKind
import openpay.app.rails.RailDecider
import openpay.app.util.Net
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: OpenPayApp, onBack: () -> Unit) {
    val context = LocalContext.current
    val bankIndex by app.store.bankIndex.collectAsState()
    val payments by app.store.payments.collectAsState()
    var showBankPicker by remember { mutableStateOf(false) }
    var net by remember { mutableStateOf(Net.netKind(context)) }
    var carrier by remember { mutableStateOf(Net.carrier(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            net = Net.netKind(context)
            carrier = Net.carrier(context)
            delay(3000)
        }
    }
    val bank = Banks.all.getOrNull(bankIndex) ?: Banks.all.last()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("Settings")) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBankPicker = true }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Eyebrow(s("Bank SMS matching"))
                    Text(bank.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        if (bank.senders.isEmpty()) {
                            s("Other bank: sender matching is off, only amount plus debit wording counts.")
                        } else {
                            s("Only SMS from {s} close a payment.", "s" to bank.senders.joinToString(" or "))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow(s("This phone"))
                    SettingRow(s("Carrier"), carrier ?: s("unknown"))
                    SettingRow(s("Network"), when (net) {
                        NetKind.ONLINE -> s("online")
                        NetKind.SIGNAL -> s("signal only")
                        NetKind.NONE -> s("none")
                    })
                    SettingRow(s("Jio detected"), if (RailDecider.isJio(carrier)) s("yes, IVR rail") else s("no, USSD rail first"))
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow(s("Trust"))
                    SettingRow(s("Internet permission"), s("absent, by design"))
                    SettingRow(s("Accounts"), s("none"))
                    SettingRow(s("Analytics"), s("none"))
                    SettingRow(s("Ledger"), s("encrypted on device"))
                    SettingRow(
                        s("Version"),
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                        }.getOrDefault("?")
                    )
                }
            }

            val language by app.store.language.collectAsState()
            val elderly by app.store.elderly.collectAsState()
            val expensesEnabled by app.store.expensesEnabled.collectAsState()

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow(s("Language"))
                    Lang.names.forEach { (code, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { app.store.setLanguage(code) }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = language == code, onClick = null)
                            Text(name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(s("Accessibility"))
                        Text(s("Elder mode, large text"), style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(checked = elderly, onCheckedChange = { app.store.setElderly(it) })
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(s("Expense log"))
                        Text(s("Private spending log"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            s("Reads bank debit SMS on device to build a private spending log. No internet, ever."),
                            style = MaterialTheme.typography.bodySmall,
                            color = FaintInk
                        )
                    }
                    Switch(checked = expensesEnabled, onCheckedChange = { app.store.setExpensesEnabled(it) })
                }
            }

            Button(
                onClick = { runCatching { exportCsv(context, payments) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Export CSV"), style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sahilwasgood/openpay"))
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Source code"), style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = {
                    val text = payments.sortedByDescending { it.createdAtEpoch }
                        .joinToString("\n\n") { p ->
                            "${fullLabel(p.createdAtEpoch)} · ₹${p.amountRupeesString} · " +
                                "${p.payee.displayName.ifBlank { p.payee.vpa ?: p.payee.phone ?: "payee" }} · " +
                                p.state.name.lowercase() +
                                (p.bankRef?.let { " · ref $it" } ?: "")
                        }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, text.ifBlank { ts("No payments yet.") }),
                                ts("Share ledger")
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(s("Share ledger"), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showBankPicker) {
        BankPickerDialog(
            selectedIndex = bankIndex,
            onSelect = { app.store.setBank(it) },
            onDismiss = { showBankPicker = false }
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun exportCsv(context: Context, payments: List<Payment>) {
    val csv = buildString {
        appendLine("date,payee,upi,phone,amount_rupees,rail,state,bank_ref")
        payments.sortedBy { it.createdAtEpoch }.forEach { p ->
            fun esc(v: String?) = (v ?: "").replace(",", " ").replace("\n", " ")
            appendLine(
                listOf(
                    fullLabel(p.createdAtEpoch),
                    esc(p.payee.displayName),
                    esc(p.payee.vpa),
                    esc(p.payee.phone),
                    p.amountRupeesString,
                    p.rail.name,
                    p.state.name,
                    esc(p.bankRef)
                ).joinToString(",")
            )
        }
    }
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val out = File(dir, "openpay-ledger.csv")
    out.writeText(csv)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", out)
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/csv")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "OpenPay ledger"
            )
        )
    }
    Toast.makeText(context, ts("Ledger exported"), Toast.LENGTH_SHORT).show()
}
