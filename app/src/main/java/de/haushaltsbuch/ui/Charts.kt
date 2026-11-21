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
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
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

/** Einnahmen (grün) und Ausgaben (rot) der letzten Monate. */
@Composable
fun IncomeExpenseChart(months: List<MonthSum>, modifier: Modifier = Modifier) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(months) {
        producer.runTransaction {
            columnSeries {
                series(months.map { it.income / 100.0 })
                series(months.map { it.expense / 100.0 })
            }
        }
    }
    val labels = months.map { it.month.month.getDisplayName(DateStyle.SHORT, Locale.GERMAN) }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(color = Income, thickness = 12.dp),
                    rememberLineComponent(color = Expense, thickness = 12.dp)
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, x, _ -> labels.getOrElse(x.toInt()) { "" } }
            )
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(200.dp)
    )
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
