package de.haushaltsbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.haushaltsbuch.ui.*

/** Statistik im Pegel-Layout: erst "Wohin geht das Geld?" als Rangliste, dann die letzten Monate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(ui: UiState, onPeriod: (Period) -> Unit) {
    val total = ui.shares.sumOf { it.cents }
    val max = (ui.shares.maxOfOrNull { it.cents } ?: 1L).coerceAtLeast(1L)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Wohin geht das Geld?", style = MaterialTheme.typography.headlineSmall)
        }
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (ui.shares.isEmpty()) {
                        Text("Keine Ausgaben in diesem Zeitraum.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column {
                            Text("Ausgaben gesamt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ui.fmt(total), style = MaterialTheme.typography.displaySmall)
                        }
                        // Eine Zeile Aussage statt Diagramm-Rätsel: der größte Posten in Worten
                        val top = ui.shares.first()
                        Text(
                            "Am meisten geht für ${top.category.name} drauf: ${ui.fmt(top.cents)}, das sind ${percent(top.cents, total)} %.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        ui.shares.forEach { s ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${s.category.icon} ${s.category.name}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Text("${ui.fmt(s.cents)} · ${percent(s.cents, total)} %", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { s.cents.toFloat() / max },
                                    color = Lagune, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Die letzten Monate", style = MaterialTheme.typography.titleMedium)
                    Text("Einnahmen und Ausgaben in ${ui.base}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    IncomeExpenseChart(ui.months)
                }
            }
        }
    }
}

private fun percent(part: Long, total: Long): Int = if (total <= 0) 0 else Math.round(part * 100.0 / total).toInt()
