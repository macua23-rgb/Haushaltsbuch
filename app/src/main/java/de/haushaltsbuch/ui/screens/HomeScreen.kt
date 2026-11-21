package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*

@Composable
fun HomeScreen(ui: UiState, onEdit: (Booking) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kontostand", style = MaterialTheme.typography.labelLarge)
            Text(ui.fmt(ui.totalBalance), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Einnahmen", ui.fmt(ui.monthIncome), Income, Modifier.weight(1f))
                StatCard("Ausgaben", ui.fmt(ui.monthExpense), Expense, Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Kontostand im Monat", style = MaterialTheme.typography.titleMedium)
                    BalanceLineChart(ui.balanceSeries)
                }
            }
        }
        item { Text("Letzte Buchungen", style = MaterialTheme.typography.titleMedium) }
        items(ui.bookings.take(5), key = { it.id }) { BookingRow(it, ui, Modifier.clickable { onEdit(it) }) }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BookingRow(b: Booking, ui: UiState, modifier: Modifier = Modifier) {
    val cat = ui.category(b)
    val from = ui.account(b.accountId)
    val to = ui.account(b.toAccountId)
    val (sign, color) = when (b.type) {
        TxType.INCOME -> "+" to Income
        TxType.EXPENSE -> "−" to Expense
        TxType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        modifier = modifier,
        leadingContent = { Text(if (b.type == TxType.TRANSFER) "🔁" else cat?.icon ?: "•", style = MaterialTheme.typography.titleLarge) },
        headlineContent = { Text(b.note.ifBlank { cat?.name ?: "Umbuchung" }) },
        supportingContent = {
            val tags = if (b.tags.isNotBlank()) " · ${b.tags}" else ""
            val clip = if (b.receiptUri != null) " · 📎" else ""
            Text(
                (if (to != null) "${from?.name} → ${to.name}" else "${cat?.name.orEmpty()} · ${from?.name.orEmpty()}") + tags + clip
            )
        },
        trailingContent = {
            Text(sign + b.amountCents.money(ui.currencyOf(b.accountId)), color = color, fontWeight = FontWeight.SemiBold)
        }
    )
}
