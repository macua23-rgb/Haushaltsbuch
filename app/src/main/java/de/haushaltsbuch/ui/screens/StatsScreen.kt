package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(ui: UiState, onPeriod: (Period) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = ui.period == p, onClick = { onPeriod(p) },
                        shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size)
                    ) { Text(p.label) }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ausgaben nach Kategorie", style = MaterialTheme.typography.titleMedium)
                    if (ui.shares.isEmpty()) Text("Keine Ausgaben in diesem Zeitraum.", style = MaterialTheme.typography.bodySmall)
                    else DonutChart(
                        ui.shares, ui.fmt(ui.shares.sumOf { it.cents }),
                        Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally)
                    )
                    ui.shares.forEach { s ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(Color(s.category.colorArgb)))
                            Text("  ${s.category.name}", Modifier.weight(1f))
                            Text(ui.fmt(s.cents))
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Einnahmen und Ausgaben (${ui.base})", style = MaterialTheme.typography.titleMedium)
                    IncomeExpenseChart(ui.months)
                }
            }
        }
    }
}
