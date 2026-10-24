package openpay.app.ui

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import openpay.app.OpenPayApp
import openpay.app.data.Payment
import openpay.app.ui.theme.FaintInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(app: OpenPayApp, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(s("Payments")) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(s("Spending")) })
            }
            if (tab == 0) PaymentsList(app = app, onOpen = onOpen) else SpendingList(app = app)
        }
    }
}

@Composable
private fun PaymentsList(app: OpenPayApp, onOpen: (String) -> Unit) {
    val payments by app.store.payments.collectAsState()
    val sorted = payments.sortedByDescending { it.createdAtEpoch }

    if (sorted.isEmpty()) {
        EmptyNote(s("Nothing yet. Payments appear here the moment they start."))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sorted.groupBy { dayLabel(it.createdAtEpoch) }.forEach { (day, list) ->
            item(key = "day_$day") {
                ListHeader(day)
            }
            items(list, key = { it.id }) { payment ->
                PaymentRow(payment = payment, onClick = { onOpen(payment.id) })
            }
        }
    }
}

@Composable
private fun SpendingList(app: OpenPayApp) {
    val expenses by app.expenseStore.expenses.collectAsState()
    val sorted = expenses.sortedByDescending { it.createdAtEpoch }

    if (sorted.isEmpty()) {
        EmptyNote(s("No spending captured yet. Your bank's debit SMS become a private log here, on this phone only."))
        return
    }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    val groups = sorted.groupBy { monthFormat.format(Date(it.createdAtEpoch)) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { (month, list) ->
            item(key = "month_$month") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 2.dp)
                ) {
                    Text(
                        month,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = FaintInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        s("Rs {a}", "a" to rupeesOf(list.sumOf { it.amountPaise })),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(list, key = { "${it.createdAtEpoch}_${it.ref ?: it.amountPaise}" }) { e ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.payeeHint ?: s("unknown"),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            dayLabel(e.createdAtEpoch),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = FaintInk
                        )
                    }
                    Text(
                        "₹${rupeesOf(e.amountPaise)}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
        item {
            Text(
                s("Reads your bank's debit SMS on device. Nothing leaves this phone."),
                style = MaterialTheme.typography.bodySmall,
                color = FaintInk,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

private fun rupeesOf(paise: Long): String =
    if (paise % 100 == 0L) (paise / 100).toString() else "%d.%02d".format(paise / 100, paise % 100)

@Composable
private fun EmptyNote(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = FaintInk
        )
    }
}

@Composable
private fun ListHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = FaintInk,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
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
