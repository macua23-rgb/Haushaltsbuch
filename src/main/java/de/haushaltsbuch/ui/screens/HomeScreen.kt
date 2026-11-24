package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HomeScreen(ui: UiState, onEdit: (Booking) -> Unit, onAdd: (TxType) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Hero(ui) }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onAdd(TxType.EXPENSE) },
                    Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tinte, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Remove, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ausgabe", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                Button(
                    onClick = { onAdd(TxType.INCOME) },
                    Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Income, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Einnahme", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
            }
        }
        item { Text("Letzte Buchungen", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium) }
        items(ui.bookings.take(5), key = { it.id }) { BookingRow(it, ui, Modifier.clickable { onEdit(it) }) }
    }
}

/** Kopfbereich: Wie viel ist bis Monatsende noch frei? Ohne Budgets steht dort der Kontostand. */
@Composable
private fun Hero(ui: UiState) {
    val today = LocalDate.now()
    val end = today.withDayOfMonth(today.lengthOfMonth())
    val daysLeft = (ChronoUnit.DAYS.between(today, end) + 1).toInt()
    val budgetTotal = ui.budgets.sumOf { it.limit }
    val hasBudget = budgetTotal > 0
    val free = budgetTotal - ui.monthExpense
    val used = if (hasBudget) ui.monthExpense.toFloat() / budgetTotal else 0f
    val soft = Color.White.copy(alpha = 0.88f)

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Lagune)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val title = when {
            !hasBudget -> "Kontostand"
            free >= 0 -> "Frei bis ${end.format(DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN))}"
            else -> "Über den Budgets"
        }
        Text(title, color = soft, style = MaterialTheme.typography.titleMedium)
        Text(
            ui.fmt(if (hasBudget) free else ui.totalBalance),
            color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 50.sp
        )
        if (hasBudget && free > 0) {
            val perDay = free / daysLeft
            Text(
                (if (daysLeft == 1) "Noch 1 Tag" else "Noch $daysLeft Tage") + ", etwa ${ui.fmt(perDay)} pro Tag",
                color = soft, style = MaterialTheme.typography.bodyMedium
            )
        } else if (!hasBudget) {
            Text("Lege unter „Budgets“ Limits an, dann siehst du hier, was noch frei ist.", color = soft, style = MaterialTheme.typography.bodyMedium)
        }
        if (hasBudget) {
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { used.coerceIn(0f, 1f) },
                color = Brandung, trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
            )
            Spacer(Modifier.height(6.dp))
            Text("${(used * 100).toInt()} % der Budgets verbraucht", color = soft, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            HeroStat("Einnahmen", ui.fmt(ui.monthIncome), Modifier.weight(1f))
            HeroStat("Ausgaben", ui.fmt(ui.monthExpense), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
