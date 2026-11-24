package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.data.Category
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.*

@Composable
fun CategoriesScreen(ui: UiState, onSave: (Category) -> Unit, onDelete: (Category) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Category?>(null) }   // id 0 = neu
    var toDelete by remember { mutableStateOf<Category?>(null) }
    SubScreen("Kategorien", onBack, actions = {
        IconButton(onClick = { editing = Category(name = "", icon = ICONS[0], colorArgb = PALETTE[0], type = TxType.EXPENSE) }) {
            Icon(Icons.Default.Add, "Kategorie hinzufügen")
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            listOf(TxType.EXPENSE to "Ausgaben", TxType.INCOME to "Einnahmen").forEach { (t, title) ->
                item { Text(title, Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp), style = MaterialTheme.typography.titleMedium) }
                items(ui.categories.values.filter { it.type == t }, key = { it.id }) { c ->
                    ListItem(
                        modifier = Modifier.clickable { editing = c },
                        leadingContent = {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c.colorArgb).copy(alpha = 0.25f)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text(c.icon)
                            }
                        },
                        headlineContent = { Text(c.name) },
                        trailingContent = {
                            IconButton(onClick = { toDelete = c }) { Icon(Icons.Default.Delete, "${c.name} löschen") }
                        }
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier.clickable { editing = Category(name = "", icon = ICONS[0], colorArgb = PALETTE[0], type = t) },
                        leadingContent = { Icon(Icons.Default.Add, null) },
                        headlineContent = { Text("Kategorie hinzufügen") }
                    )
                }
            }
        }
    }
    editing?.let { cat ->
        CategoryEditor(cat, onDismiss = { editing = null }, onSave = { onSave(it); editing = null }, onDelete = { onDelete(cat); editing = null })
    }
    toDelete?.let { c ->
        ConfirmDialog(
            "Kategorie löschen?",
            "Die Buchungen bleiben erhalten, haben danach aber keine Kategorie. Ein Budget dazu wird gelöscht.",
            "Löschen",
            onConfirm = { onDelete(c); toDelete = null }, onDismiss = { toDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditor(cat: Category, onDismiss: () -> Unit, onSave: (Category) -> Unit, onDelete: () -> Unit) {
    val isNew = cat.id == 0L
    var name by remember { mutableStateOf(cat.name) }
    var icon by remember { mutableStateOf(cat.icon) }
    var color by remember { mutableStateOf(cat.colorArgb) }
    var type by remember { mutableStateOf(cat.type) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isNew) "Kategorie hinzufügen" else "Kategorie bearbeiten", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            // Der Typ bleibt bei bestehenden Kategorien fest, sonst passen Budgets und Buchungen nicht mehr
            ChipRow(listOf(TxType.EXPENSE to "Ausgabe", TxType.INCOME to "Einnahme"), type, enabled = isNew) { type = it }
            ChipRow(ICONS.map { it to it }, icon) { icon = it }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PALETTE) { c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                            .clickable { color = c }
                    )
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    if (name.isBlank()) error = "Name eingeben"
                    else onSave(cat.copy(name = name.trim(), icon = icon, colorArgb = color, type = type))
                },
                Modifier.fillMaxWidth()
            ) { Text("Speichern") }
            if (!isNew) TextButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                Text("Kategorie löschen", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete) ConfirmDialog(
        "Kategorie löschen?", "Die Buchungen bleiben erhalten, haben danach aber keine Kategorie. Ein Budget dazu wird gelöscht.", "Löschen",
        onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false }
    )
}
