package openpay.app.ui

import openpay.app.ui.theme.WarnAmber
import openpay.app.ui.theme.FaintInk
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import openpay.app.OpenPayApp
import openpay.app.Store
import openpay.app.rails.Guards
import openpay.app.data.PayState
import openpay.app.data.Payee
import openpay.app.data.Payment
import openpay.app.data.RupeeFormat
import openpay.app.rails.IntentRail
import openpay.app.rails.IvrRail
import openpay.app.rails.NetKind
import openpay.app.rails.Qr
import openpay.app.data.Rail
import openpay.app.rails.RailContext
import openpay.app.rails.RailDecider
import openpay.app.rails.RailPick
import openpay.app.rails.UssdRail
import openpay.app.util.Net
import kotlinx.coroutines.delay
import java.util.UUID

private enum class GuideKind { USSD, IVR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(
    app: OpenPayApp,
    onOpenHistory: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var payeeName by rememberSaveable { mutableStateOf("") }
    var vpa by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var guide by remember { mutableStateOf<GuideKind?>(null) }
    var pendingIntentPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var dupWarn by remember { mutableStateOf<openpay.app.data.Payment?>(null) }
    var allowDup by remember { mutableStateOf(false) }

    var net by remember { mutableStateOf(Net.netKind(context)) }
    var carrier by remember { mutableStateOf(Net.carrier(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            net = Net.netKind(context)
            carrier = Net.carrier(context)
            delay(3000)
        }
    }

    val amountPaise = parsePaise(amountText)
    val payee = Payee(
        displayName = payeeName.trim(),
        vpa = vpa.trim().ifBlank { null },
        phone = phone.trim().ifBlank { null }
    )
    val pick = RailDecider.pick(
        RailContext(net, carrier, payeeHasVpa = payee.vpa != null, payeeHasPhone = payee.phone != null)
    )
    val rupeesString = if (amountPaise > 0) RupeeFormat.format(amountPaise) else ""
    val ivrRupees = amountPaise / 100
    val ivrReady = payee.phone != null && IvrRail.dtmfSuffix(payee.phone!!, ivrRupees) != null
    val hasDecimals = amountPaise % 100 != 0L

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents ?: return@rememberLauncherForActivityResult
        Qr.parse(content)?.let { q ->
            vpa = q.vpa
            if (q.name.isNotBlank()) payeeName = q.name
            q.amountPaise?.let { amountText = RupeeFormat.format(it) }
            if (!q.note.isNullOrBlank()) noteText = q.note
        } ?: run {
            Toast.makeText(context, ts("Not a UPI QR code"), Toast.LENGTH_SHORT).show()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val upiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingIntentPaymentId
        if (id != null) {
            val normalized = IntentRail.normalizeResponse(
                result.data?.extras?.getString("response")
            )
            val now = System.currentTimeMillis()
            when (normalized) {
                "SUCCESS" -> app.store.update(id) {
                    it.copy(
                        state = PayState.SUCCESS,
                        intentResult = "SUCCESS",
                        updatedAtEpoch = now,
                        settledAtEpoch = now
                    )
                }
                "FAILURE" -> app.store.update(id) {
                    it.copy(state = PayState.FAILED, intentResult = "FAILURE", updatedAtEpoch = now)
                }
                else -> Unit
            }
        }
        pendingIntentPaymentId = null
    }

    fun pay() {
        if (amountPaise <= 0) return
        val dup = Guards.findDuplicate(
            app.store.payments.value, amountPaise, payee.vpa, payee.phone,
            System.currentTimeMillis(), Store.WINDOW_MS
        )
        if (dup != null && !allowDup) {
            dupWarn = dup
            return
        }
        allowDup = false
        val rail: Rail = when (pick) {
            is RailPick.Intent -> Rail.INTENT
            is RailPick.Ussd -> Rail.USSD
            is RailPick.Ivr -> Rail.IVR
            is RailPick.None -> return
        }
        val missing = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.RECEIVE_SMS)
            .filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        if (missing.isNotEmpty()) {
            Toast.makeText(context, ts("Grant call and SMS, then tap Pay again"), Toast.LENGTH_LONG).show()
            permLauncher.launch(missing.toTypedArray())
            return
        }
        val payment = Payment(
            id = UUID.randomUUID().toString(),
            createdAtEpoch = System.currentTimeMillis(),
            payee = payee,
            amountPaise = amountPaise,
            note = noteText.trim(),
            rail = rail
        )
        app.store.add(payment)
        when (pick) {
            is RailPick.Intent -> {
                val intent = IntentRail.launchIntent(
                    payee.vpa ?: return,
                    payee.displayName,
                    rupeesString,
                    noteText.trim()
                )
                if (intent.resolveActivity(context.packageManager) != null) {
                    pendingIntentPaymentId = payment.id
                    upiLauncher.launch(intent)
                } else {
                    Toast.makeText(context, ts("No UPI app installed"), Toast.LENGTH_LONG).show()
                }
            }
            is RailPick.Ussd -> {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_CALL, UssdRail.dialUri()))
                }.onFailure {
                    Toast.makeText(context, ts("The dialer refused the call"), Toast.LENGTH_SHORT).show()
                }
                guide = GuideKind.USSD
            }
            is RailPick.Ivr -> {
                val uri = IvrRail.dialUri(payee.phone ?: return, ivrRupees) ?: return
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_CALL, uri))
                }.onFailure {
                    Toast.makeText(context, "The dialer refused the call", Toast.LENGTH_SHORT).show()
                }
                guide = GuideKind.IVR
            }
            is RailPick.None -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MarkIcon(20.dp)
                        Text(
                            "OpenPay",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "History")
                    }
                    IconButton(onClick = onOpenNotes) {
                        Icon(Icons.Filled.ConfirmationNumber, contentDescription = "Notes")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            Text(
                s("Net: {a} · Carrier: {b}", "a" to netLabel(net), "b" to (carrier ?: s("unknown"))),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = FaintInk
            )

            OutlinedTextField(
                value = payeeName,
                onValueChange = { payeeName = it },
                label = { Text(s("Payee name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vpa,
                onValueChange = { vpa = it },
                label = { Text(s("UPI ID, like name@bank")) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan a UPI QR code")
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                label = { Text(s("Or payee phone (for 123Pay)")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(7) },
                label = { Text(s("Amount in rupees")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                prefix = { Text("₹", style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it.take(50) },
                label = { Text(s("Note, optional")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            RailCard(pick = pick, ivrReady = ivrReady && !hasDecimals)

            if (pick is RailPick.Ivr && hasDecimals) {
                HintCard(s("The 123Pay call needs whole rupees. Drop the paise or use a UPI ID with data."))
            }

            Button(
                onClick = ::pay,
                enabled = amountPaise > 0 &&
                    (payee.vpa != null || payee.phone != null) &&
                    pick !is RailPick.None &&
                    (pick !is RailPick.Ivr || (ivrReady && !hasDecimals)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    if (rupeesString.isEmpty()) s("Pay") else s("Pay ₹{amt}", "amt" to rupeesString),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                s("Your UPI PIN is typed on the carrier menu or the call keypad. OpenPay never sees it."),
                style = MaterialTheme.typography.bodySmall,
                color = FaintInk
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    dupWarn?.let { dup ->
        AlertDialog(
            onDismissRequest = { dupWarn = null },
            title = { Text(s("Possible double payment")) },
            text = {
                Text(
                    s(
                        "You started a payment of Rs {a} to {p} {s} seconds ago and it has not confirmed yet. Retrying can double debit. Wait for the SMS or continue.",
                        "a" to dup.amountRupeesString,
                        "p" to dup.payee.displayName.ifBlank { dup.payee.vpa ?: dup.payee.phone ?: "" },
                        "s" to ((System.currentTimeMillis() - dup.createdAtEpoch) / 1000).toString()
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { dupWarn = null; allowDup = true; pay() }) { Text(s("Pay anyway")) }
            },
            dismissButton = {
                TextButton(onClick = { dupWarn = null }) { Text(s("Wait")) }
            }
        )
    }

    val currentGuide = guide
    if (currentGuide != null) {
        val (title, steps) = when (currentGuide) {
            GuideKind.USSD -> s("Finish it on the USSD menu") to
                UssdRail.guideSteps(payee.vpa).map {
                    s(it, "vpa" to (payee.vpa ?: ""), "amt" to rupeesString)
                }
            GuideKind.IVR -> s("Finish it on the call") to
                IvrRail.guideSteps().map { s(it, "amt" to RupeeFormat.format(ivrRupees)) }
        }
        ModalBottomSheet(onDismissRequest = { guide = null }) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 7.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    s("Recorded as pending. Your bank's SMS closes it automatically, usually within a minute."),
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk
                )
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun RailCard(pick: RailPick, ivrReady: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (pick is RailPick.None) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Eyebrow(s("Best rail right now"))
            Spacer(Modifier.height(6.dp))
            when (pick) {
                is RailPick.Intent -> RailLine(s("UPI Intent"), s("Data available. Handed to your UPI app."))
                is RailPick.Ussd -> RailLine(s("USSD · *99#"), s("Signal only. Carrier menu, no data needed."))
                is RailPick.Ivr ->
                    if (ivrReady) {
                        RailLine(s("IVR · UPI 123Pay"), s("Signal only. Works on Jio."))
                    } else {
                        RailLine(s("IVR · UPI 123Pay"), s("Needs a valid 10 digit phone number."))
                    }
                is RailPick.None -> Text(
                    noneCopy(pick.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarnAmber
                )
            }
        }
    }
}

@Composable
private fun RailLine(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(
        sub,
        style = MaterialTheme.typography.bodySmall,
        color = FaintInk,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun HintCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = WarnAmber,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun noneCopy(reason: RailPick.Reason): String = when (reason) {
    RailPick.Reason.NO_PAYEE -> s("Add a UPI ID or a phone number first.")
    RailPick.Reason.OFFLINE_NO_PHONE ->
        s("Jio carries no USSD and the 123Pay call needs the payee's phone number. Add it, or wait for data.")
    RailPick.Reason.NO_SIGNAL ->
        s("No network at all right now. Create an OpenPay Note instead: it verifies offline and settles over UPI later. The ticket icon opens Notes.")
    RailPick.Reason.NO_TELEPHONY -> s("No SIM detected on this phone.")
}

@Composable
private fun netLabel(net: NetKind): String = when (net) {
    NetKind.ONLINE -> s("online")
    NetKind.SIGNAL -> s("signal only")
    NetKind.NONE -> s("none")
}

private fun parsePaise(text: String): Long {
    val cleaned = text.filter(Char::isDigit)
    if (cleaned.isEmpty()) return 0L
    return cleaned.toLongOrNull()?.times(100) ?: 0L
}
