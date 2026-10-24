package openpay.app.ui

import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.runtime.LaunchedEffect
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
import openpay.app.notes.PayerLimits
import openpay.app.notes.Split
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
                title = { Text(if (tab == 0) s("Notes · issue") else if (tab == 1) s("Notes · verify") else s("Notes · split")) },
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
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(s("Issue")) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(s("Verify")) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(s("Split")) })
            }
            when (tab) {
                0 -> IssueTab(app)
                1 -> VerifyTab(app)
                else -> SplitTab(app)
            }
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
    var envelopeQueue by remember { mutableStateOf<List<Notes.Envelope>>(emptyList()) }
    var settleSerial by remember { mutableStateOf<String?>(null) }
    var settleQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var autoAdvanceSettle by remember { mutableStateOf(false) }

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
                Toast.makeText(context, ts("Note settled"), Toast.LENGTH_SHORT).show()
            }
        }
        settleSerial = null
        autoAdvanceSettle = true
    }

    LaunchedEffect(autoAdvanceSettle) {
        if (!autoAdvanceSettle) return@LaunchedEffect
        autoAdvanceSettle = false
        val next = settleQueue.firstOrNull() ?: return@LaunchedEffect
        settleQueue = settleQueue.drop(1)
        val record = app.notesStore.issued.value.firstOrNull {
            it.payload.serial == next && it.open
        } ?: return@LaunchedEffect
        settleSerial = next
        settleLauncher.launch(
            IntentRail.launchIntent(
                record.payload.vpa!!,
                record.payload.to,
                RupeeFormat.format(record.payload.amountPaise),
                "OpenPay note $next"
            )
        )
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
                    s("A Note is a promissory note, a digital khata slip. It is verified offline and settles over UPI later."),
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk
                )
                Text(
                    s("Rs {a} of Rs 10,000 open", "a" to RupeeFormat.format(openPaise)),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it.take(30); app.notesStore.setPayerName(it.take(30)) },
                    label = { Text(s("Your name, shown to the payee")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it.take(30) },
                    label = { Text(s("Payee name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text(s("Payee UPI ID, for settling")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    label = { Text(s("Or payee phone")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(4) },
                    label = { Text(s("Amount in rupees, max 2000")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(60) },
                    label = { Text(s("Memo, optional")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = {
                    val perm = android.Manifest.permission.CALL_PHONE
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, ts("Grant call and SMS, then tap Pay again"), Toast.LENGTH_LONG).show()
                        return@OutlinedButton
                    }
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_CALL,
                                openpay.app.rails.UssdRail.dialUri(openpay.app.rails.UssdRail.USSD_BALANCE)
                            )
                        )
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(s("Check balance (*99*3#)"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1 hour", 360 to "6 hours", 1440 to "24 hours").forEach { (m, label) ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutes = m },
                            label = { Text(s(label)) }
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
                    Text(s("Create Note"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (issued.any { it.open && !it.payload.vpa.isNullOrBlank() }) {
            item(key = "settleall") {
                OutlinedButton(
                    onClick = {
                        val opens = issued.filter { it.open && !it.payload.vpa.isNullOrBlank() }
                            .sortedBy { it.issuedAtEpoch }
                        if (opens.isNotEmpty() && settleSerial == null) {
                            settleQueue = opens.drop(1).map { it.payload.serial }
                            settleSerial = opens.first().payload.serial
                            settleLauncher.launch(
                                IntentRail.launchIntent(
                                    opens.first().payload.vpa!!,
                                    opens.first().payload.to,
                                    RupeeFormat.format(opens.first().payload.amountPaise),
                                    "OpenPay note ${opens.first().payload.serial}"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s("Settle all open notes"))
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
                    if (note.open && note.envelope != null) {
                        TextButton(onClick = { envelope = Notes.parse(note.envelope!!) }) {
                            Text(s("Show QR"))
                        }
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
                                    Text(s("Settle over UPI"))
                                }
                            }
                            TextButton(onClick = {
                                app.notesStore.markSettled(note.payload.serial)
                            }) {
                                Text(s("Mark settled"), color = FaintInk)
                            }
                        }
                    }
                }
            }
        }
    }

    val current = envelope
    if (current != null) {
        val advance = {
            envelope = envelopeQueue.firstOrNull()
            envelopeQueue = envelopeQueue.drop(1)
        }
        AlertDialog(
            onDismissRequest = { advance() },
            confirmButton = {
                TextButton(onClick = { advance() }) { Text(s("Done")) }
            },
            title = { Text(s("Note {serial}", "serial" to current.d.serial)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = NoteQr.bitmap(Notes.encode(current), QR_SIZE)
                            .asImageBitmap(),
                        contentDescription = "Note QR",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        s("The payee scans this in Verify mode. No signal needed on either phone."),
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val clipboard = LocalClipboardManager.current
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(Notes.encode(current)))
                            Toast.makeText(context, ts("Note text copied"), Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f)) {
                            Text(s("Copy note text"))
                        }
                        OutlinedButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT, Notes.encode(current)),
                                        ts("Share note")
                                    )
                                )
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text(s("Share note"))
                        }
                    }
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
    var consentPayload by remember { mutableStateOf<Notes.Payload?>(null) }
    val blockedKeys by app.notesStore.blockedKeys.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@rememberLauncherForActivityResult
        val v = app.notesStore.verifyIncoming(content)
        verdict = v
        when (v) {
            is Notes.Verdict.Valid ->
                when {
                    v.payload.payerKey in app.notesStore.blockedKeys.value -> {
                        app.voice.speak(ts("Blacklisted payer. Refuse."))
                    }
                    PayerLimits.needsConsent(v.payload.amountPaise, v.knownPayer) && !v.duplicateSerial -> {
                        consentPayload = v.payload
                        app.voice.speak(ts("New payer, big note."))
                    }
                    else -> {
                        app.voice.speak(
                            ts(
                                "Note verified. Rupees {a} from {f}.",
                                "a" to RupeeFormat.format(v.payload.amountPaise),
                                "f" to v.payload.from
                            )
                        )
                        app.notesStore.acceptVerified(v.payload, v.knownPayer)
                    }
                }
            is Notes.Verdict.Bad ->
                app.voice.speak(ts("Note rejected.") + " " + v.reason)
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
                    s("Merchant mode. Scan a customer's Note with zero signal: the signature, expiry, cap and serial are all checked on this phone."),
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk
                )
                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt(ts("Scan an OpenPay Note"))
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(s("Scan a Note"), style = MaterialTheme.typography.labelLarge)
                }

                when (val v = verdict) {
                    is Notes.Verdict.Valid -> {
                        val blockedNow = v.payload.payerKey in blockedKeys
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (blockedNow) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            )
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (blockedNow) {
                                    Text(
                                        s("Blacklisted payer. Refuse."),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = openpay.app.ui.theme.WarnAmber
                                    )
                                }
                                Text(
                                    if (v.duplicateSerial) s("Duplicate serial, already seen.")
                                    else s("Verified. Signature checks out."),
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
                                    if (v.knownPayer) s("Known payer.")
                                    else s("New payer key. Save it to trust this key next time."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FaintInk
                                )
                                if (!v.knownPayer && !v.duplicateSerial && !blockedNow) {
                                    TextButton(onClick = {
                                        app.notesStore.pinPayer(v.payload.payerKey, v.payload.from)
                                        Toast.makeText(context, ts("Payer saved"), Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text(s("Save this payer"))
                                    }
                                }
                                if (blockedNow) {
                                    TextButton(onClick = {
                                        app.notesStore.unblockPayer(v.payload.payerKey)
                                    }) {
                                        Text(s("Remove block"))
                                    }
                                } else if (!v.duplicateSerial) {
                                    TextButton(onClick = {
                                        app.notesStore.blockPayer(v.payload.payerKey)
                                        app.voice.speak(ts("Blacklisted payer. Refuse."))
                                    }) {
                                        Text(s("Block this payer"), color = openpay.app.ui.theme.WarnAmber)
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
                            s("Rejected: {r}", "r" to v.reason),
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
                                if (r.knownPayer) s("known payer") else s("new key"),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = FaintInk
                        )
                    }
                }
            }
        }
    }

    consentPayload?.let { payload ->
        AlertDialog(
            onDismissRequest = { consentPayload = null },
            title = { Text(s("New payer, big note")) },
            text = {
                Text(
                    s("First note from this key and above Rs 500. Notes are promises, not money. Accept?")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    app.notesStore.acceptVerified(payload, knownPayer = false)
                    app.voice.speak(
                        ts(
                            "Note verified. Rupees {a} from {f}.",
                            "a" to RupeeFormat.format(payload.amountPaise),
                            "f" to payload.from
                        )
                    )
                    verdict = Notes.Verdict.Valid(payload, knownPayer = false, duplicateSerial = false)
                    consentPayload = null
                }) { Text(s("Accept")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    app.voice.speak(ts("Note rejected."))
                    verdict = Notes.Verdict.Bad("declined by you")
                    consentPayload = null
                }) { Text(s("Decline")) }
            }
        )
    }
}


@Composable
private fun SplitTab(app: OpenPayApp) {
    val context = LocalContext.current
    var total by rememberSaveable { mutableStateOf("") }
    var people by rememberSaveable { mutableStateOf("") }
    var vpa by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var names by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var envelope by remember { mutableStateOf<Notes.Envelope?>(null) }
    var envelopeQueue by remember { mutableStateOf<List<Notes.Envelope>>(emptyList()) }

    val totalPaise = (total.toLongOrNull() ?: 0L) * 100
    val count = (people.toIntOrNull() ?: 0).coerceAtMost(12)
    val shares = Split.shares(totalPaise, count)
    val nameList = names.split(',').map { it.trim() }.filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            s("One bill, many notes. OpenPay cuts the total into fair shares and signs a Note per person, for the same UPI ID."),
            style = MaterialTheme.typography.bodySmall,
            color = FaintInk
        )
        OutlinedTextField(
            value = total,
            onValueChange = { total = it.filter(Char::isDigit).take(5) },
            label = { Text(s("Total amount in rupees")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = people,
            onValueChange = { people = it.filter(Char::isDigit).take(2) },
            label = { Text(s("Number of shares")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vpa,
            onValueChange = { vpa = it },
            label = { Text(s("Payee UPI ID, same for every note")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(10) },
            label = { Text(s("Or payee phone")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = names,
            onValueChange = { names = it.take(200) },
            label = { Text(s("Names, comma separated, optional")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it.take(40) },
            label = { Text(s("Memo, optional")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (shares.isNotEmpty() &&
            openpay.app.rails.Mdr.looksLikeStructuring(totalPaise, shares) &&
            (vpa.isNotBlank() || phone.isNotBlank())
        ) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    s("MDR structuring warning"),
                    style = MaterialTheme.typography.bodySmall,
                    color = openpay.app.ui.theme.WarnAmber,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (shares.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        s("Each share"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    shares.forEachIndexed { i, paise ->
                        val name = nameList.getOrNull(i) ?: ts("Person {n}", "n" to (i + 1).toString())
                        Text(
                            "${i + 1}. $name · ₹${if (paise % 100 == 0L) (paise / 100).toString() else "%d.%02d".format(paise / 100, paise % 100)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                if (totalPaise < count) {
                    Toast.makeText(context, ts("Total must be at least the number of shares"), Toast.LENGTH_LONG).show()
                    return@Button
                }
                val created = mutableListOf<Notes.Envelope>()
                var refused: String? = null
                for (i in shares.indices) {
                    val name = nameList.getOrNull(i) ?: ts("Person {n}", "n" to (i + 1).toString())
                    val noteMemo = listOf(memo, ts("Split {i}/{n}", "i" to (i + 1).toString(), "n" to count.toString()))
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    when (val r = app.notesStore.issue(
                        to = name,
                        vpa = vpa.trim().ifBlank { null },
                        phone = phone.trim().ifBlank { null },
                        amountPaise = shares[i],
                        validMinutes = 60,
                        memo = noteMemo,
                        from = app.notesStore.payerName.value
                    )) {
                        is IssueResult.Created -> created.add(r.envelope)
                        is IssueResult.Refused -> { refused = r.reason; break }
                    }
                }
                if (created.isNotEmpty()) {
                    envelope = created.first()
                    envelopeQueue = created.drop(1)
                }
                if (refused != null) {
                    Toast.makeText(context, refused, Toast.LENGTH_LONG).show()
                }
            },
            enabled = shares.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                if (shares.isEmpty()) s("Create Note") else s("Create {n} notes", "n" to count.toString()),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    val current = envelope
    if (current != null) {
        val advance = {
            envelope = envelopeQueue.firstOrNull()
            envelopeQueue = envelopeQueue.drop(1)
        }
        AlertDialog(
            onDismissRequest = { advance() },
            confirmButton = {
                TextButton(onClick = { advance() }) { Text(s("Done")) }
            },
            title = { Text(s("Note {serial}", "serial" to current.d.serial)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = NoteQr.bitmap(Notes.encode(current), QR_SIZE).asImageBitmap(),
                        contentDescription = s("Note QR"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        s("The payee scans this in Verify mode. No signal needed on either phone."),
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk
                    )
                }
            }
        )
    }
}
