package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Account
import de.haushaltsbuch.data.AccountType
import de.haushaltsbuch.ui.*

@Composable
fun MoreScreen(ui: UiState, onSettle: (Account) -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.padding(0.dp)) {
        item { Text("Konten", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) }
        items(ui.accounts, key = { it.account.id }) { a ->
            val card = a.account.type == AccountType.CREDIT_CARD
            ListItem(
                modifier = Modifier.clickable { onOpen("accounts") },
                leadingContent = { Text(a.account.icon, style = MaterialTheme.typography.titleLarge) },
                headlineContent = { Text(a.account.name) },
                supportingContent = {
                    if (card) Text("Abrechnung am ${a.account.settlementDay ?: "–"}. des Monats")
                },
                trailingContent = {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(
                            a.balanceCents.money(a.account.currency),
                            color = if (a.balanceCents < 0) Expense else MaterialTheme.colorScheme.onSurface
                        )
                        if (card && a.balanceCents < 0) TextButton({ onSettle(a.account) }) { Text("Jetzt abrechnen") }
                    }
                }
            )
        }
        val entries = listOf(
            "accounts" to "Konten verwalten", "categories" to "Kategorien verwalten",
            "recurring" to "Wiederkehrende Buchungen", "settings" to "Einstellungen, Export und Sperre"
        )
        items(entries) { (route, label) ->
            ListItem(modifier = Modifier.clickable { onOpen(route) }, headlineContent = { Text(label) })
        }
    }
}
