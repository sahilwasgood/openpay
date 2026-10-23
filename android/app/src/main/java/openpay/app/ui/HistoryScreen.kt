package openpay.app.ui

import openpay.app.ui.theme.FaintInk
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import openpay.app.OpenPayApp
import openpay.app.data.Payment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(app: OpenPayApp, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val payments by app.store.payments.collectAsState()
    val sorted = payments.sortedByDescending { it.createdAtEpoch }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("History")) },
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
        if (sorted.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    s("Nothing yet. Payments appear here the moment they start."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FaintInk
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                sorted.groupBy { dayLabel(it.createdAtEpoch) }.forEach { (day, list) ->
                    item(key = "day_$day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = FaintInk,
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp
                            )
                        )
                    }
                    items(list, key = { it.id }) { payment ->
                        PaymentRow(payment = payment, onClick = { onOpen(payment.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                payment.payee.displayName.ifBlank {
                    payment.payee.vpa ?: payment.payee.phone ?: "Payee"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                buildString {
                    append(fullLabel(payment.createdAtEpoch))
                    payment.bankRef?.let { append(" · ref $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = FaintInk,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.padding(2.dp))
            StateChip(payment.state)
        }
        Text(
            "₹${payment.amountRupeesString}",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
