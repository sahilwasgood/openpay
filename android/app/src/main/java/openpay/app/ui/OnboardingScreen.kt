package openpay.app.ui

import openpay.app.ui.theme.FaintInk
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import openpay.app.OpenPayApp
import openpay.app.data.Banks

@Composable
fun OnboardingScreen(app: OpenPayApp, onDone: () -> Unit) {
    val context = LocalContext.current
    val bankIndex by app.store.bankIndex.collectAsState()
    var showBankPicker by remember { mutableStateOf(false) }
    var permsGranted by remember {
        mutableStateOf(
            listOf(Manifest.permission.CALL_PHONE, Manifest.permission.RECEIVE_SMS)
                .all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> permsGranted = grants.values.all { it } }

    val bank = Banks.all.getOrNull(bankIndex) ?: Banks.all.last()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkIcon(34.dp)
            Text(
                "OpenPay",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Text(
            "Pay even with zero signal.",
            style = MaterialTheme.typography.headlineMedium
        )

        OnboardCard(
            "One payment, four rails",
            "With data, OpenPay hands off to your UPI app. With signal only, it dials *99#, or the 123Pay line when you are on Jio. Offline Notes arrive in the next release."
        )
        OnboardCard(
            "Your PIN stays yours",
            "Your UPI PIN is typed on the carrier menu or the call keypad. OpenPay never sees it and never asks for it."
        )
        OnboardCard(
            "No internet, actually",
            "The app ships without the internet permission. It cannot reach any server. Every record is encrypted on this phone."
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Eyebrow("Pick your bank")
                Text(
                    "Confirmation SMS are only trusted from your bank's sender ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                OutlinedButton(onClick = { showBankPicker = true }) {
                    Text(bank.name)
                }
            }
        }

        if (!permsGranted) {
            Button(
                onClick = {
                    permLauncher.launch(
                        arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.RECEIVE_SMS)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Allow calling and SMS", style = MaterialTheme.typography.labelLarge)
            }
        }

        Button(
            onClick = {
                app.store.setOnboarded()
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Start using OpenPay", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "OpenPay is not a bank. Payments settle on UPI rails through your bank. *99# and UPI 123Pay are NPCI services.",
            style = MaterialTheme.typography.bodySmall,
            color = FaintInk,
            modifier = Modifier.padding(bottom = 28.dp)
        )
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
private fun OnboardCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
