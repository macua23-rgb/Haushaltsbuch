package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Budget
import de.haushaltsbuch.data.SavingsGoal
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*

private sealed interface BudgetDialog {
    data class Edit(val categoryId: Long) : BudgetDialog
    data object New : BudgetDialog
}

@Composable
fun BudgetsScreen(
    ui: UiState,
    onSaveBudget: (Budget) -> Unit, onDeleteBudget: (Long) -> Unit,
    onSaveGoal: (SavingsGoal) -> Unit, onDeleteGoal: (SavingsGoal) -> Unit
) {
    var dialog by remember { mutableStateOf<BudgetDialog?>(null) }
    var goalDialog by remember { mutableStateOf<SavingsGoal?>(null) }   // id 0 = neu

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Budgets im Monat", style = MaterialTheme.typography.titleLarge) }
        if (ui.budgets.isEmpty()) item { Text("Noch kein Budget angelegt.") }
        items(ui.budgets, key = { it.category.id }) { b ->
            Column(Modifier.clickable { dialog = BudgetDialog.Edit(b.category.id) }) {
                Row {
                    Text("${b.category.icon} ${b.category.name}${if (b.ratio >= 0.8f) " ⚠️" else ""}", Modifier.weight(1f))
                    Text("${ui.fmt(b.spent)} von ${ui.fmt(b.limit)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { b.ratio.coerceAtMost(1f) },
                    color = when { b.ratio >= 1f -> Expense; b.ratio >= 0.8f -> Warning; else -> Income },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                )
            }
        }
        item { OutlinedButton(onClick = { dialog = BudgetDialog.New }) { Text("Budget hinzufügen") } }

        item { Text("Sparziele", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.goals.forEach { g ->
                    Card(Modifier.weight(1f).clickable { goalDialog = g }) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ProgressRing(if (g.targetCents > 0) g.savedCents.toFloat() / g.targetCents else 0f, Income)
                            Text("${g.icon} ${g.name}", style = MaterialTheme.typography.titleSmall)
                            Text("${ui.fmt(g.savedCents)} von ${ui.fmt(g.targetCents)}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = { goalDialog = SavingsGoal(name = "", icon = "🎯", targetCents = 0, savedCents = 0) }) { Text("Sparziel hinzufügen") } }
    }

    dialog?.let { d ->
        val expenseCats = ui.categories.values.filter { it.type == TxType.EXPENSE }
        val existing = (d as? BudgetDialog.Edit)?.let { e -> ui.budgets.firstOrNull { it.category.id == e.categoryId } }
        var catId by remember(d) { mutableStateOf((d as? BudgetDialog.Edit)?.categoryId) }
        var limit by remember(d) { mutableStateOf(existing?.let { centsToInput(it.limit) } ?: "") }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (existing == null) "Budget hinzufügen" else "Budget bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (existing == null) {
                        val free = expenseCats.filter { c -> ui.budgets.none { it.category.id == c.id } }
                        if (free.isEmpty()) Text("Alle Kategorien haben schon ein Budget.")
                        ChipRow(free.map { it.id to "${it.icon} ${it.name}" }, catId) { catId = it }
                    } else Text("${existing.category.icon} ${existing.category.name}")
                    AmountField(limit, { limit = it }, "Monatslimit (${ui.base})")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val c = catId; val l = parseCents(limit)
                    if (c != null && l > 0) { onSaveBudget(Budget(c, l)); dialog = null }
                }) { Text("Speichern") }
            },
            dismissButton = {
                Row {
                    if (existing != null) TextButton(onClick = { onDeleteBudget(existing.category.id); dialog = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { dialog = null }) { Text("Abbrechen") }
                }
            }
        )
    }

    goalDialog?.let { g ->
        var name by remember(g) { mutableStateOf(g.name) }
        var target by remember(g) { mutableStateOf(if (g.targetCents > 0) centsToInput(g.targetCents) else "") }
        var saved by remember(g) { mutableStateOf(if (g.savedCents > 0) centsToInput(g.savedCents) else "") }
        AlertDialog(
            onDismissRequest = { goalDialog = null },
            title = { Text(if (g.id == 0L) "Sparziel hinzufügen" else "Sparziel bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    AmountField(target, { target = it }, "Zielbetrag")
                    AmountField(saved, { saved = it }, "Bereits gespart")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = parseCents(target)
                    if (name.isNotBlank() && t > 0) {
                        onSaveGoal(g.copy(name = name.trim(), targetCents = t, savedCents = parseCents(saved)))
                        goalDialog = null
                    }
                }) { Text("Speichern") }
            },
            dismissButton = {
                Row {
                    if (g.id != 0L) TextButton(onClick = { onDeleteGoal(g); goalDialog = null }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { goalDialog = null }) { Text("Abbrechen") }
                }
            }
        )
    }
}
