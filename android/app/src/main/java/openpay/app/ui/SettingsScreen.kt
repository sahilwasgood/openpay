package openpay.app.ui

import openpay.app.ui.theme.FaintInk
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
                title = { Text("Settings") },
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
                    Eyebrow("Bank SMS matching")
                    Text(bank.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        if (bank.senders.isEmpty()) {
                            "Other bank: sender matching is off, only amount plus debit wording counts."
                        } else {
                            "Only SMS from ${bank.senders.joinToString(" or ")} close a payment."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("This phone")
                    SettingRow("Carrier", carrier ?: "unknown")
                    SettingRow("Network", when (net) {
                        NetKind.ONLINE -> "online"
                        NetKind.SIGNAL -> "signal only"
                        NetKind.NONE -> "none"
                    })
                    SettingRow("Jio detected", if (RailDecider.isJio(carrier)) "yes, IVR rail" else "no, USSD rail first")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Trust")
                    SettingRow("Internet permission", "absent, by design")
                    SettingRow("Accounts", "none")
                    SettingRow("Analytics", "none")
                    SettingRow("Ledger", "encrypted on device")
                    SettingRow(
                        "Version",
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                        }.getOrDefault("?")
                    )
                }
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
                Text("Source code", style = MaterialTheme.typography.labelLarge)
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
                                    .putExtra(Intent.EXTRA_TEXT, text.ifBlank { "No payments yet." }),
                                "Share ledger"
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Share ledger", style = MaterialTheme.typography.labelLarge)
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
