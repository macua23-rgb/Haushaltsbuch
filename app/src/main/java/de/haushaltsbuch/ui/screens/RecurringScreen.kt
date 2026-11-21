package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Interval
import de.haushaltsbuch.data.RecurringRule
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecurringScreen(ui: UiState, onSave: (RecurringRule) -> Unit, onDelete: (RecurringRule) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<RecurringRule?>(null) }   // id 0 = neu
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    SubScreen("Wiederkehrende Buchungen", onBack, actions = {
        IconButton(onClick = {
            editing = RecurringRule(
                type = TxType.EXPENSE, amountCents = 0, accountId = ui.accounts.firstOrNull()?.account?.id ?: 0L,
                categoryId = null, note = "", interval = Interval.MONTHLY, nextDue = LocalDate.now()
            )
        }) { Icon(Icons.Default.Add, "Hinzufügen") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            if (ui.recurring.isEmpty()) item { Text("Noch keine wiederkehrenden Buchungen.", Modifier.padding(16.dp)) }
            items(ui.recurring, key = { it.id }) { r ->
                val cat = r.categoryId?.let { ui.categories[it] }
                ListItem(
                    modifier = Modifier.clickable { editing = r },
                    leadingContent = { Text(cat?.icon ?: "🔁", style = MaterialTheme.typography.titleLarge) },
                    headlineContent = { Text(r.note.ifBlank { cat?.name ?: "Buchung" }) },
                    supportingContent = { Text("${r.interval.label} · nächste am ${r.nextDue.format(fmt)} · ${r.amountCents.money(ui.currencyOf(r.accountId))}") },
                    trailingContent = { Switch(checked = r.active, onCheckedChange = { onSave(r.copy(active = it)) }) }
                )
            }
        }
    }
    editing?.let { r ->
        RuleEditor(ui, r, onDismiss = { editing = null }, onSave = { onSave(it); editing = null }, onDelete = { onDelete(r); editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditor(ui: UiState, r: RecurringRule, onDismiss: () -> Unit, onSave: (RecurringRule) -> Unit, onDelete: () -> Unit) {
    val isNew = r.id == 0L
    var type by remember { mutableStateOf(r.type) }
    var amount by remember { mutableStateOf(if (r.amountCents > 0) centsToInput(r.amountCents) else "") }
    var accountId by remember { mutableStateOf(r.accountId) }
    var catId by remember { mutableStateOf(r.categoryId) }
    var note by remember { mutableStateOf(r.note) }
    var interval by remember { mutableStateOf(r.interval) }
    var due by remember { mutableStateOf(r.nextDue) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isNew) "Wiederkehrende Buchung" else "Buchung bearbeiten", style = MaterialTheme.typography.titleLarge)
            ChipRow(listOf(TxType.EXPENSE to "Ausgabe", TxType.INCOME to "Einnahme"), type) { type = it; catId = null }
            AmountField(amount, { amount = it }, "Betrag (${symbolOf(ui.currencyOf(accountId))})", Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Notiz, z. B. Miete") }, singleLine = true)
            ChipRow(ui.categories.values.filter { it.type == type }.map { it.id to "${it.icon} ${it.name}" }, catId) { catId = it }
            Text("Konto", style = MaterialTheme.typography.labelMedium)
            ChipRow(ui.accounts.map { it.account.id to "${it.account.icon} ${it.account.name}" }, accountId) { accountId = it }
            ChipRow(Interval.entries.map { it to it.label }, interval) { interval = it }
            DatePickerField(due, { due = it }, "Nächste Fälligkeit")
            Text(
                "Fällige Buchungen legt die App täglich an, auch rückwirkend, wenn sie länger nicht geöffnet war.",
                style = MaterialTheme.typography.bodySmall
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val c = parseCents(amount)
                    when {
                        c <= 0 -> error = "Betrag eingeben"
                        ui.accounts.none { it.account.id == accountId } -> error = "Konto wählen"
                        else -> onSave(r.copy(type = type, amountCents = c, accountId = accountId, categoryId = catId, note = note.trim(), interval = interval, nextDue = due, anchorDay = due.dayOfMonth))
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("Speichern") }
            if (!isNew) TextButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                Text("Löschen", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete) ConfirmDialog(
        "Wiederkehrende Buchung löschen?", "Bereits angelegte Buchungen bleiben erhalten.", "Löschen",
        onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false }
    )
}
