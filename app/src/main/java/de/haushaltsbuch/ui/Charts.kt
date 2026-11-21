package de.haushaltsbuch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.format.TextStyle as DateStyle
import java.util.Locale

/** Donut: Ausgaben nach Kategorie. Vico hat keinen Pie-Chart, daher Canvas. */
@Composable
fun DonutChart(shares: List<CategoryShare>, center: String, modifier: Modifier = Modifier) {
    val total = shares.sumOf { it.cents }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Butt)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = stroke)
            var start = -90f
            shares.forEach { s ->
                val sweep = 360f * s.cents / total
                drawArc(Color(s.category.colorArgb), start, (sweep - 1.5f).coerceAtLeast(0.5f), false, topLeft, arcSize, style = stroke)
                start += sweep
            }
        }
        Text(center, style = MaterialTheme.typography.titleMedium)
    }
}

/** Fortschrittsring für Sparziele. */
@Composable
fun ProgressRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = stroke)
        }
        Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.labelLarge)
    }
}

/** Einnahmen (grün) und Ausgaben (rot) der letzten Monate, als Canvas gezeichnet. */
@Composable
fun IncomeExpenseChart(months: List<MonthSum>, modifier: Modifier = Modifier) {
    val maxValue = (months.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1L).coerceAtLeast(1L)
    val axis = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val slot = size.width / months.size.coerceAtLeast(1)
            val barW = slot * 0.3f
            val gap = 2.dp.toPx()
            months.forEachIndexed { i, m ->
                val center = i * slot + slot / 2
                val hIncome = size.height * m.income / maxValue
                val hExpense = size.height * m.expense / maxValue
                drawRect(Income, Offset(center - barW - gap / 2, size.height - hIncome), Size(barW, hIncome))
                drawRect(Expense, Offset(center + gap / 2, size.height - hExpense), Size(barW, hExpense))
            }
            drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            months.forEach {
                Text(
                    it.month.month.getDisplayName(DateStyle.SHORT, Locale.GERMAN),
                    Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("■ Einnahmen", color = Income, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(12.dp))
            Text("■ Ausgaben", color = Expense, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Kontostand-Verlauf im laufenden Monat. */
@Composable
fun BalanceLineChart(points: List<Long>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Text("Noch zu wenig Daten", style = MaterialTheme.typography.bodySmall)
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        producer.runTransaction { lineSeries { series(points.map { it / 100.0 }) } }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(160.dp)
    )
}
