package openpay.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import openpay.app.OpenPayApp
import openpay.app.data.PayState
import openpay.app.data.RupeeFormat
import openpay.app.notes.NoteQr
import openpay.app.notes.Notes
import openpay.app.notes.NotesStore.IssueResult
import openpay.app.rails.IntentRail
import openpay.app.ui.theme.FaintInk

private const val QR_SIZE = 760

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(app: OpenPayApp, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "Notes · issue" else "Notes · verify") },
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
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Issue") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Verify") })
            }
            if (tab == 0) IssueTab(app) else VerifyTab(app)
        }
    }
}

@Composable
private fun IssueTab(app: OpenPayApp) {
    val context = LocalContext.current
    val issued by app.notesStore.issued.collectAsState()
    val payerNameState by app.notesStore.payerName.collectAsState()
    val openPaise = app.notesStore.openPaise

    var from by rememberSaveable(payerNameState) { mutableStateOf(payerNameState) }
    var to by rememberSaveable { mutableStateOf("") }
    var vpa by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf(60) }
    var envelope by remember { mutableStateOf<Notes.Envelope?>(null) }
    var settleSerial by remember { mutableStateOf<String?>(null) }

    val settleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val serial = settleSerial
        if (serial != null) {
            val normalized = IntentRail.normalizeResponse(
                result.data?.extras?.getString("response")
            )
            if (normalized == "SUCCESS") {
                app.notesStore.markSettled(serial)
                Toast.makeText(context, "Note settled", Toast.LENGTH_SHORT).show()
            }
        }
        settleSerial = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A Note is a signed promise. The payee verifies it offline, it settles over UPI later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk
                )
                Text(
                    "Rs ${RupeeFormat.format(openPaise)} of Rs 10,000 open",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it.take(30); app.notesStore.setPayerName(it.take(30)) },
                    label = { Text("Your name, shown to the payee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it.take(30) },
                    label = { Text("Payee name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("Payee UPI ID, for settling") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    label = { Text("Or payee phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(4) },
                    label = { Text("Amount in rupees, max 2000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(60) },
                    label = { Text("Memo, optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1 hour", 360 to "6 hours", 1440 to "24 hours").forEach { (m, label) ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutes = m },
                            label = { Text(label) }
                        )
                    }
                }
                Button(
                    onClick = {
                        when (val r = app.notesStore.issue(
                            to = to.trim(),
                            vpa = vpa,
                            phone = phone,
                            amountPaise = (amount.toLongOrNull() ?: 0L) * 100,
                            validMinutes = minutes,
                            memo = memo,
                            from = from
                        )) {
                            is IssueResult.Created -> envelope = r.envelope
                            is IssueResult.Refused ->
                                Toast.makeText(context, r.reason, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Create Note", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        items(issued, key = { it.payload.serial }) { note ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "₹${RupeeFormat.format(note.payload.amountPaise)} to ${note.payload.to}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${note.payload.serial} · expires ${fullLabel(note.payload.expiresEpoch)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = FaintInk
                            )
                        }
                        StateChip(
                            if (note.open) PayState.PENDING else PayState.SUCCESS
                        )
                    }
                    if (note.open) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!note.payload.vpa.isNullOrBlank()) {
                                OutlinedButton(onClick = {
                                    settleSerial = note.payload.serial
                                    settleLauncher.launch(
                                        IntentRail.launchIntent(
                                            note.payload.vpa!!,
                                            note.payload.to,
                                            RupeeFormat.format(note.payload.amountPaise),
                                            "OpenPay note ${note.payload.serial}"
                                        )
                                    )
                                }) {
                                    Text("Settle over UPI")
                                }
                            }
                            TextButton(onClick = {
                                app.notesStore.markSettled(note.payload.serial)
                            }) {
                                Text("Mark settled", color = FaintInk)
                            }
                        }
                    }
                }
            }
        }
    }

    val current = envelope
    if (current != null) {
        AlertDialog(
            onDismissRequest = { envelope = null },
            confirmButton = {
                TextButton(onClick = { envelope = null }) { Text("Done") }
            },
            title = { Text("Note ${current.d.serial}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = NoteQr.bitmap(Notes.encode(current), QR_SIZE)
                            .asImageBitmap(),
                        contentDescription = "Note QR",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "The payee scans this in Verify mode. No signal needed on either phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk
                    )
                }
            }
        )
    }
}

@Composable
private fun VerifyTab(app: OpenPayApp) {
    val context = LocalContext.current
    val receivables by app.notesStore.receivables.collectAsState()
    var verdict by remember { mutableStateOf<Notes.Verdict?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@rememberLauncherForActivityResult
        val v = app.notesStore.verifyIncoming(content)
        verdict = v
        when (v) {
            is Notes.Verdict.Valid -> {
                app.voice.speak(
                    "Note verified. Rupees ${RupeeFormat.format(v.payload.amountPaise)} " +
                        "from ${v.payload.from}."
                )
                app.notesStore.acceptVerified(v.payload, v.knownPayer)
            }
            is Notes.Verdict.Bad ->
                app.voice.speak("Note rejected. ${v.reason}.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Merchant mode. Scan a customer's Note with zero signal: the signature, expiry, cap and serial are all checked on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk
                )
                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan an OpenPay Note")
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Scan a Note", style = MaterialTheme.typography.labelLarge)
                }

                when (val v = verdict) {
                    is Notes.Verdict.Valid -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (v.duplicateSerial) "Duplicate serial, already seen."
                                    else "Verified. Signature checks out.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (v.duplicateSerial) {
                                        openpay.app.ui.theme.WarnAmber
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                                Text(
                                    "₹${RupeeFormat.format(v.payload.amountPaise)} from ${v.payload.from} · ${v.payload.serial}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (v.knownPayer) "Known payer."
                                    else "New payer key. Save it to trust this key next time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FaintInk
                                )
                                if (!v.knownPayer && !v.duplicateSerial) {
                                    TextButton(onClick = {
                                        app.notesStore.pinPayer(v.payload.payerKey, v.payload.from)
                                        Toast.makeText(context, "Payer saved", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Save this payer")
                                    }
                                }
                            }
                        }
                    }
                    is Notes.Verdict.Bad -> Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            "Rejected: ${v.reason}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = openpay.app.ui.theme.WarnAmber,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    null -> Unit
                }
            }
        }

        items(receivables, key = { it.payload.serial + it.receivedAtEpoch }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "₹${RupeeFormat.format(r.payload.amountPaise)} from ${r.payload.from}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${fullLabel(r.receivedAtEpoch)} · ${r.payload.serial} · " +
                                if (r.knownPayer) "known payer" else "new key",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = FaintInk
                        )
                    }
                }
            }
        }
    }
}
