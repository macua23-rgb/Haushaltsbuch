package de.haushaltsbuch.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Account
import de.haushaltsbuch.data.AccountType
import de.haushaltsbuch.ui.*

private val TYPE_LABELS = mapOf(
    AccountType.CHECKING to "Girokonto", AccountType.CASH to "Bargeld",
    AccountType.CREDIT_CARD to "Kreditkarte", AccountType.SAVINGS to "Sparkonto"
)

@Composable
fun AccountsScreen(ui: UiState, onSave: (Account) -> Unit, onDelete: (Account, (Boolean) -> Unit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Account?>(null) }   // id 0 = neu
    SubScreen("Konten", onBack, actions = {
        IconButton(onClick = { editing = Account(name = "", type = AccountType.CHECKING, icon = "🏦") }) {
            Icon(Icons.Default.Add, "Konto hinzufügen")
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(ui.accounts, key = { it.account.id }) { a ->
                ListItem(
                    modifier = Modifier.clickable { editing = a.account },
                    leadingContent = { Text(a.account.icon, style = MaterialTheme.typography.titleLarge) },
                    headlineContent = { Text(a.account.name) },
                    supportingContent = { Text("${TYPE_LABELS[a.account.type]} · ${a.account.currency}") },
                    trailingContent = { Text(a.balanceCents.money(a.account.currency)) }
                )
            }
        }
    }
    editing?.let { acc ->
        AccountEditor(
            ui, acc, onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = {
                onDelete(acc) { ok ->
                    if (ok) editing = null
                    else Toast.makeText(ctx, "Konto hat noch Buchungen oder wiederkehrende Buchungen", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditor(ui: UiState, acc: Account, onDismiss: () -> Unit, onSave: (Account) -> Unit, onDelete: () -> Unit) {
    val isNew = acc.id == 0L
    var name by remember { mutableStateOf(acc.name) }
    var icon by remember { mutableStateOf(acc.icon) }
    var type by remember { mutableStateOf(acc.type) }
    var currency by remember { mutableStateOf(acc.currency) }
    var start by remember { mutableStateOf(if (acc.startBalanceCents != 0L) centsToInput(acc.startBalanceCents) else "") }
    var day by remember { mutableStateOf(acc.settlementDay?.toString() ?: "") }
    var settleId by remember { mutableStateOf(acc.settlementAccountId) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Die Währung ändert die Bedeutung vorhandener Beträge, daher nur ohne Buchungen
    val used = !isNew && ui.bookings.any { it.accountId == acc.id || it.toAccountId == acc.id }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isNew) "Konto hinzufügen" else "Konto bearbeiten", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            ChipRow(ACCOUNT_ICONS.map { it to it }, icon) { icon = it }
            ChipRow(TYPE_LABELS.map { it.key to it.value }, type) { type = it }
            ChipRow(CURRENCIES.map { it to it }, currency, enabled = !used) { currency = it }
            if (used) Text("Die Währung lässt sich nicht ändern, solange Buchungen existieren.", style = MaterialTheme.typography.bodySmall)
            AmountField(start, { start = it }, "Startsaldo (${symbolOf(currency)})", Modifier.fillMaxWidth(), allowNegative = true)
            if (type == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    day, { day = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(),
                    label = { Text("Abrechnungstag (1 bis 31)") }, singleLine = true
                )
                Text("Abbuchen von", style = MaterialTheme.typography.labelMedium)
                ChipRow(
                    ui.accounts.filter { it.account.id != acc.id && it.account.type != AccountType.CREDIT_CARD }
                        .map { it.account.id to "${it.account.icon} ${it.account.name}" },
                    settleId
                ) { settleId = it }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val d = day.toIntOrNull()
                    when {
                        name.isBlank() -> error = "Name eingeben"
                        type == AccountType.CREDIT_CARD && d != null && d !in 1..31 -> error = "Abrechnungstag zwischen 1 und 31"
                        else -> onSave(
                            acc.copy(
                                name = name.trim(), icon = icon, type = type, currency = currency,
                                startBalanceCents = parseSignedCents(start),
                                settlementDay = d.takeIf { type == AccountType.CREDIT_CARD },
                                settlementAccountId = settleId.takeIf { type == AccountType.CREDIT_CARD }
                            )
                        )
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("Speichern") }
            if (!isNew) TextButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                Text("Konto löschen", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete) ConfirmDialog(
        "Konto löschen?", "Das geht nur, wenn das Konto keine Buchungen und keine wiederkehrenden Buchungen hat.", "Löschen",
        onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false }
    )
}
