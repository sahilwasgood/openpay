package openpay.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import openpay.app.data.Banks

@Composable
fun BankPickerDialog(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s("Done")) }
        },
        title = { Text(s("Your bank")) },
        text = {
            Column {
                Banks.all.forEachIndexed { index, bank ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null
                        )
                        Text(
                            text = bank.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    )
}
