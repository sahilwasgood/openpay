package openpay.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openpay.app.rails.Handoff
import openpay.app.rails.RechargeHandoff
import openpay.app.rails.RechargeOperator

private enum class Method { SBI, AXIS, IVR, MENU }

private val GlassText = Color(0xFFE7EDF5)
private val GlassMuted = Color(0xFFA8B8CC)
private val GlassGreen = Color(0xFF34D399)
private val GlassAmber = Color(0xFFFBBF24)
private val GlassShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var method by rememberSaveable { mutableStateOf(Method.SBI) }
    var operator by rememberSaveable { mutableStateOf(RechargeOperator.JIO) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var phoneText by rememberSaveable { mutableStateOf("") }
    var axisOperator by rememberSaveable { mutableStateOf("JIO") }
    var last6 by rememberSaveable { mutableStateOf("") }
    var otpText by rememberSaveable { mutableStateOf("") }
    var stepTwo by rememberSaveable { mutableStateOf(false) }

    fun openSms(sms: Handoff.Sms) {
        val uri = Uri.parse("sms:${sms.target}?body=${Uri.encode(sms.body)}")
        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
            .onFailure { Toast.makeText(context, ts("No SMS app found"), Toast.LENGTH_SHORT).show() }
    }

    fun openDial(dial: Handoff.Dial) {
        val uri = Uri.parse("tel:${dial.number.replace("#", "%23")}")
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) }
            .onFailure { Toast.makeText(context, ts("No dialer found"), Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = Color(0xFF080C13),
        topBar = {
            TopAppBar(
                title = { Text(s("Recharge"), color = GlassText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s("Back"),
                            tint = GlassText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = GlassText,
                    navigationIconContentColor = GlassText
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070B12), Color(0xFF101A24))))) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .blur(64.dp)
            ) {
                drawCircle(
                    Color(0x3344D6A0),
                    radius = size.minDimension * 0.36f,
                    center = Offset(size.width * 0.82f, size.height * 0.12f)
                )
                drawCircle(
                    Color(0x2F3B82F6),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width * 0.12f, size.height * 0.62f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard {
                    GlassEyebrow(s("Offline recharge"))
                    Spacer(Modifier.height(6.dp))
                    Text(s("Buy a data pack with zero mobile data"), color = GlassText, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s("OpenPay never touches the money. It composes the message or dials the number. You confirm on the handset, your bank pays, the operator credits the number."),
                        color = GlassMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }

                GlassCard {
                    GlassEyebrow(s("How do you want to pay?"))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Method.entries.forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m; stepTwo = false },
                                label = { Text(s(m.label()), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.07f),
                                    labelColor = GlassMuted,
                                    selectedContainerColor = Color(0x2A34D399),
                                    selectedLabelColor = GlassGreen
                                )
                            )
                        }
                    }
                }

                when (method) {
                    Method.SBI -> {
                        GlassCard {
                            OperatorPicker(operator) { operator = it }
                            Spacer(Modifier.height(10.dp))
                            AmountField(amountText, 4) { amountText = it.filter(Char::isDigit).take(4) }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                s("SBI Quick: Rs 700 a day, Rs 1000 a month, your own registered mobile only."),
                                color = GlassAmber,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(10.dp))
                            val amount = amountText.toIntOrNull() ?: 0
                            val first = RechargeHandoff.sbiStep1(operator, amount)
                            GlassButton(
                                text = s("Step 1 of 2 · Compose bank SMS"),
                                enabled = first != null && !stepTwo
                            ) {
                                if (first != null) { openSms(first); stepTwo = true }
                            }
                            if (stepTwo) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    s("After the OTP SMS arrives, enter its 6 digits and send the second SMS within 10 minutes."),
                                    color = GlassMuted,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = otpText,
                                    onValueChange = { otpText = it.filter(Char::isDigit).take(6) },
                                    label = { Text(s("6 digit OTP")) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = glassFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                val second = RechargeHandoff.sbiStep2(otpText)
                                GlassButton(s("Step 2 of 2 · Confirm"), enabled = second != null) {
                                    if (second != null) openSms(second)
                                }
                            }
                        }
                    }
                    Method.AXIS -> {
                        GlassCard {
                            Text(s("Any mobile number, no internet. Confirm the operator token with Axis."), color = GlassMuted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = phoneText,
                                onValueChange = { phoneText = it.filter(Char::isDigit).take(12) },
                                label = { Text(s("Mobile number")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = glassFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = axisOperator,
                                onValueChange = { axisOperator = it.take(16) },
                                label = { Text(s("Operator token, eg JIO")) },
                                singleLine = true,
                                colors = glassFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            AmountField(amountText, 5) { amountText = it.filter(Char::isDigit).take(5) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = last6,
                                onValueChange = { last6 = it.filter(Char::isDigit).take(6) },
                                label = { Text(s("Last 6 digits of Axis account")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = glassFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            val sms = RechargeHandoff.axis(phoneText, axisOperator, amountText.toIntOrNull() ?: 0, last6)
                            GlassButton(s("Compose Axis SMS"), enabled = sms != null) {
                                if (sms != null) openSms(sms)
                            }
                        }
                    }
                    Method.IVR -> {
                        GlassCard {
                            Text(s("UPI 123Pay voice line"), color = GlassText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s("Dial 080 4516 3666, choose Mobile Recharge, say the operator, enter the number and amount, then your UPI PIN. Your bank pays the operator directly. Menu wording depends on your bank."),
                                color = GlassMuted,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(12.dp))
                            GlassButton(s("Dial UPI 123Pay")) { openDial(RechargeHandoff.upi123Pay()) }
                        }
                    }
                    Method.MENU -> {
                        GlassCard {
                            OperatorPicker(operator) { operator = it }
                            Spacer(Modifier.height(10.dp))
                            val dial = RechargeHandoff.selfService(operator)
                            if (dial == null) {
                                Text(
                                    s("Jio carries no USSD. Use the MyJio app or a retailer for a data pack. This menu is for balance and offers only."),
                                    color = GlassAmber,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    s("Spends existing balance or a voucher, not bank money."),
                                    color = GlassMuted,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(12.dp))
                                GlassButton(s("Dial operator menu")) { openDial(dial) }
                            }
                        }
                    }
                }

                GlassCard {
                    GlassEyebrow(s("Good to know"))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s("A website cannot run USSD or send SMS by itself. You always see and confirm the message or call in your own phone app. OpenPay has no internet permission, no server, and never holds funds."),
                        color = GlassMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun Method.label(): String = when (this) {
    Method.SBI -> "SBI SMS"
    Method.AXIS -> "Axis SMS"
    Method.IVR -> "UPI 123Pay"
    Method.MENU -> "Operator menu"
}

@Composable
private fun glassFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = GlassText,
    unfocusedTextColor = GlassText,
    focusedLabelColor = GlassGreen,
    unfocusedLabelColor = GlassMuted,
    cursorColor = GlassGreen,
    focusedContainerColor = Color.White.copy(alpha = 0.06f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
    focusedIndicatorColor = GlassGreen,
    unfocusedIndicatorColor = GlassMuted.copy(alpha = 0.5f)
)

@Composable
private fun GlassEyebrow(text: String) {
    Text(
        text = text.uppercase(java.util.Locale.ROOT),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = GlassGreen
    )
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.07f), GlassShape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), GlassShape)
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun GlassButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassGreen,
            contentColor = Color(0xFF06231A),
            disabledContainerColor = Color.White.copy(alpha = 0.10f),
            disabledContentColor = GlassMuted
        ),
        shape = GlassShape
    ) {
        Text(text, fontSize = 15.sp)
    }
}

@Composable
private fun AmountField(value: String, maxDigits: Int, change: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(s("Amount in rupees")) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = glassFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OperatorPicker(selected: RechargeOperator, onSelect: (RechargeOperator) -> Unit) {
    GlassEyebrow(s("Operator"))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RechargeOperator.entries.forEach { op ->
            FilterChip(
                selected = selected == op,
                onClick = { onSelect(op) },
                label = { Text(op.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.07f),
                    labelColor = GlassMuted,
                    selectedContainerColor = Color(0x2A34D399),
                    selectedLabelColor = GlassGreen
                )
            )
        }
    }
}
