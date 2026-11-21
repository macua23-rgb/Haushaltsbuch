package de.haushaltsbuch.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import de.haushaltsbuch.data.Rate
import de.haushaltsbuch.export.ExportRow
import de.haushaltsbuch.export.Exporter
import de.haushaltsbuch.security.AppLock
import de.haushaltsbuch.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

@Composable
fun SettingsScreen(ui: UiState, vm: LedgerViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var range by remember { mutableStateOf("month") }
    var pendingRows by remember { mutableStateOf<List<ExportRow>>(emptyList()) }
    var pendingTitle by remember { mutableStateOf("") }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    val csvOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { Exporter.writeCsv(pendingRows, it) } }.isSuccess
            }
            toast(if (ok) "CSV gespeichert" else "Export fehlgeschlagen")
        }
    }
    val pdfOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { Exporter.writePdf(pendingRows, pendingTitle, ui.base, it) } }.isSuccess
            }
            toast(if (ok) "PDF gespeichert" else "Export fehlgeschlagen")
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun prepare(): String {
        val today = LocalDate.now()
        val r = when (range) { "month" -> rangeOf(Period.MONTH, today); "year" -> rangeOf(Period.YEAR, today); else -> null }
        pendingRows = vm.exportRows(r)
        pendingTitle = "Haushaltsbuch, " + when (range) { "month" -> "dieser Monat"; "year" -> "dieses Jahr"; else -> "alle Buchungen" }
        return "haushaltsbuch-$today"
    }

    SubScreen("Einstellungen", onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sicherheit", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Sperre mit Fingerabdruck, Gesicht oder PIN", ui.settings.appLock) { on ->
                if (!on) vm.setAppLock(false)
                else if (!AppLock.available(ctx)) toast("Richte zuerst eine Bildschirmsperre oder Biometrie am Gerät ein")
                else (ctx as? FragmentActivity)?.let { act -> AppLock.prompt(act) { vm.setAppLock(true) } }
            }
            Text("Bei aktiver Sperre zeigt das Widget keinen Kontostand.", style = MaterialTheme.typography.bodySmall)

            Text("Benachrichtigungen", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Warnung bei 80 % und 100 % eines Budgets", ui.settings.budgetAlerts) { on ->
                vm.setBudgetAlerts(on)
                if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            Text("Währungen", style = MaterialTheme.typography.titleMedium)
            Text("Basiswährung für Summen und Charts", style = MaterialTheme.typography.bodySmall)
            ChipRow(CURRENCIES.map { it to it }, ui.base) { vm.changeBase(it) }
            val used = ui.accounts.map { it.account.currency }.toSet()
            val shown = CURRENCIES.filter { it != ui.base && (it in used || ui.rates.any { r -> r.currency == it }) }
            val missing = used.filter { !ui.fx.hasRate(it) }
            if (missing.isNotEmpty()) Text(
                "Kurs fehlt für ${missing.joinToString()}. Bis dahin wird 1:1 gerechnet.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
            )
            shown.forEach { cur ->
                RateRow(cur, ui.base, ui.rates.firstOrNull { it.currency == cur }?.toBase) { vm.saveRate(Rate(cur, it)) }
            }
            Text("Kurs hinzufügen", style = MaterialTheme.typography.bodySmall)
            ChipRow(CURRENCIES.filter { it != ui.base && it !in shown }.map { it to it }, null) { vm.saveRate(Rate(it, 1.0)) }
            Text("Kurse pflegst du selbst. Neue Buchungen und Charts nutzen immer den aktuellen Kurs.", style = MaterialTheme.typography.bodySmall)

            Text("Export", style = MaterialTheme.typography.titleMedium)
            ChipRow(listOf("month" to "Dieser Monat", "year" to "Dieses Jahr", "all" to "Alles"), range) { range = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { csvOut.launch(prepare() + ".csv") }) { Text("CSV") }
                Button(onClick = { pdfOut.launch(prepare() + ".pdf") }) { Text("PDF") }
            }
            Text("Du wählst selbst, wo die Datei gespeichert wird. Belege sind nicht enthalten.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RateRow(cur: String, base: String, rate: Double?, onSave: (Double) -> Unit) {
    var text by remember(rate) {
        mutableStateOf(rate?.let { String.format(Locale.GERMANY, "%.4f", it).trimEnd('0').trimEnd(',') } ?: "")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1 $cur =", Modifier.width(72.dp))
        OutlinedTextField(
            text, { t -> text = t.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.weight(1f),
            suffix = { Text(base) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        TextButton(onClick = { text.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let(onSave) }) { Text("OK") }
    }
}
