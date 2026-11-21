package de.haushaltsbuch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val ICONS = listOf("🏠", "🛒", "🚗", "🎬", "📺", "💊", "💼", "👶", "🧾", "✈️", "🍽️", "👕", "🎁", "📚", "🐶", "⚡", "📱", "🏋️", "☕", "💰")
val ACCOUNT_ICONS = listOf("🏦", "👛", "💳", "🐷", "💵", "🪙", "🏠", "📈")
val PALETTE = listOf(0xFF4F7CAC, 0xFFE0A030, 0xFF8E6BBF, 0xFFD4472F, 0xFF2A9D8F, 0xFFE07A9A, 0xFF14804F, 0xFF66776F, 0xFF3D5A80, 0xFFEE6C4D)

@Composable
fun <T> ChipRow(
    options: List<Pair<T, String>>, selected: T?, modifier: Modifier = Modifier,
    enabled: Boolean = true, onSelect: (T) -> Unit
) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (key, label) ->
            FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(label) }, enabled = enabled)
        }
    }
}

@Composable
fun AmountField(
    value: String, onChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, allowNegative: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            onChange(v.filterIndexed { i, c -> c.isDigit() || c == ',' || c == '.' || (allowNegative && i == 0 && c == '-') }.take(14))
        },
        modifier = modifier, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(date: LocalDate, onChange: (LocalDate) -> Unit, label: String = "Datum") {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Text("$label: ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Abbrechen") } }
        ) { DatePicker(state) }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
    title: String, onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = actions
            )
        },
        content = content
    )
}
