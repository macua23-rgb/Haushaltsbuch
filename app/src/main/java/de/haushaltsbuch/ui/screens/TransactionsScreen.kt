package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(ui: UiState, onEdit: (Booking) -> Unit, onDelete: (Booking) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<TxType?>(null) }
    var toDelete by remember { mutableStateOf<Booking?>(null) }
    val list = ui.bookings.filter { b ->
        (filter == null || b.type == filter) &&
            (query.isBlank() || (b.note + b.tags + ui.category(b)?.name.orEmpty()).contains(query, ignoreCase = true))
    }
    Column {
        OutlinedTextField(
            query, { query = it },
            Modifier.fillMaxWidth().padding(16.dp, 8.dp),
            placeholder = { Text("Suchen (Notiz, Kategorie, Tag)") }, singleLine = true
        )
        Row(Modifier.padding(horizontal = 16.dp)) {
            ChipRow(
                listOf<Pair<TxType?, String>>(null to "Alle", TxType.EXPENSE to "Ausgaben", TxType.INCOME to "Einnahmen", TxType.TRANSFER to "Umbuchungen"),
                filter
            ) { filter = it }
        }
        if (list.isEmpty()) {
            Text("Keine Buchungen gefunden.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn {
            list.groupBy { it.date }.forEach { (date, rows) ->
                stickyHeader {
                    Text(
                        date.label(),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp, 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                items(rows, key = { it.id }) { b ->
                    SwipeRow(onSwipe = { toDelete = b }) { BookingRow(b, ui, Modifier.clickable { onEdit(b) }) }
                }
            }
        }
    }
    toDelete?.let { b ->
        ConfirmDialog("Buchung löschen?", "Die Buchung und ein angehängter Beleg werden entfernt.", "Löschen",
            onConfirm = { onDelete(b); toDelete = null }, onDismiss = { toDelete = null })
    }
}

/** Wischen öffnet nur die Rückfrage, die Zeile springt zurück. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(onSwipe: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) onSwipe()
        false
    })
    SwipeToDismissBox(
        state, enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) { Icon(Icons.Default.Delete, "Löschen") }
        }
    ) { Surface { content() } }
}
